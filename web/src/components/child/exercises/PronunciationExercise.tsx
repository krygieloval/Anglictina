import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Loader2,
  Mic,
  MicOff,
  RotateCcw,
  Sparkles,
  Square,
} from 'lucide-react';
import {
  evaluatePronunciationAudio,
  isAudioRecordingSupported,
  MissingGeminiKeyError,
  pickCompatibleAudioMimeType,
  speakEnglish,
} from '../../../services/speech';
import { VocabWord } from '../../../types/vocab';
import { SpeakButton } from '../../SpeakButton';
import { WordIllustration } from '../../WordIllustration';
import { PracticeOutcomeItem } from '../PracticeSummaryScreen';

const MAX_RECORD_MS = 15000;
const SILENCE_TIMEOUT_MS = 1200;
const BASE_SPEECH_RMS_THRESHOLD = 0.025;
const VAD_POLL_MS = 60;

interface PronunciationExerciseProps {
  words: VocabWord[];
  onBack: () => void;
  onFinish: (outcomes: PracticeOutcomeItem[], starsEarned: number, score: number) => void;
}

function shuffle<T>(arr: T[]): T[] {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

function getAudioContextCtor(): (new () => AudioContext) | null {
  if (typeof window === 'undefined') return null;
  const w = window as unknown as {
    AudioContext?: new () => AudioContext;
    webkitAudioContext?: new () => AudioContext;
  };
  return w.AudioContext ?? w.webkitAudioContext ?? null;
}

export const PronunciationExercise: React.FC<PronunciationExerciseProps> = ({
  words,
  onBack,
  onFinish,
}) => {
  const queue = useMemo(() => shuffle(words).slice(0, 8), [words]);
  const recordingSupported = useMemo(() => isAudioRecordingSupported(), []);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [isRecording, setIsRecording] = useState(false);
  const [isEvaluating, setIsEvaluating] = useState(false);
  const [hasSpoken, setHasSpoken] = useState(false);
  const [micLevel, setMicLevel] = useState(0);
  const [elapsedMs, setElapsedMs] = useState(0);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [scorePercent, setScorePercent] = useState<number | null>(null);
  const [isPassed, setIsPassed] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [outcomesMap, setOutcomesMap] = useState<Map<string, PracticeOutcomeItem>>(new Map());

  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const sourceNodeRef = useRef<MediaStreamAudioSourceNode | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const maxStopTimeoutRef = useRef<number | null>(null);
  const vadIntervalRef = useRef<number | null>(null);
  const discardRef = useRef(false);

  const currentWord = queue[currentIndex];

  const clearTimersAndAudioNodes = () => {
    if (maxStopTimeoutRef.current !== null) {
      window.clearTimeout(maxStopTimeoutRef.current);
      maxStopTimeoutRef.current = null;
    }
    if (vadIntervalRef.current !== null) {
      window.clearInterval(vadIntervalRef.current);
      vadIntervalRef.current = null;
    }
    if (sourceNodeRef.current) {
      try {
        sourceNodeRef.current.disconnect();
      } catch {
        // Ignore disconnect errors
      }
      sourceNodeRef.current = null;
    }
    if (audioCtxRef.current) {
      const ctx = audioCtxRef.current;
      audioCtxRef.current = null;
      try {
        if (ctx.state !== 'closed') {
          void ctx.close();
        }
      } catch {
        // Ignore close errors
      }
    }
  };

  const releaseStream = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => {
        try {
          t.stop();
        } catch {
          // Ignore track stop errors
        }
      });
      streamRef.current = null;
    }
  };

  const cleanupRecording = (discard: boolean) => {
    discardRef.current = discard;
    clearTimersAndAudioNodes();
    const rec = recorderRef.current;
    recorderRef.current = null;
    if (rec && rec.state !== 'inactive') {
      try {
        rec.stop();
      } catch {
        // Ignore recorder stop errors
      }
    }
    releaseStream();
    setIsRecording(false);
    setHasSpoken(false);
    setMicLevel(0);
    setElapsedMs(0);
  };

  useEffect(() => {
    cleanupRecording(true);
    setIsEvaluating(false);
    setStatusMessage(null);
    setScorePercent(null);
    setIsPassed(false);
    setHasError(false);
  }, [currentIndex]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    return () => {
      cleanupRecording(true);
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (!currentWord) return null;

  const finishOrNext = (finalOutcomes: Map<string, PracticeOutcomeItem>) => {
    cleanupRecording(true);
    if (currentIndex + 1 < queue.length) {
      setCurrentIndex((i) => i + 1);
    } else {
      const all = Array.from(finalOutcomes.values());
      const correct = all.filter((o) => o.isCorrect).length;
      const pct = Math.floor((correct * 100) / Math.max(1, queue.length));
      const stars = pct >= 80 ? 3 : pct >= 50 ? 2 : 1;
      onFinish(all, stars, correct * 30);
    }
  };

  const stopRecordingNow = () => {
    clearTimersAndAudioNodes();
    const rec = recorderRef.current;
    if (rec && rec.state === 'recording') {
      try {
        rec.stop();
      } catch {
        cleanupRecording(true);
      }
    } else {
      cleanupRecording(true);
    }
  };

  const startRecordingSession = async () => {
    if (!recordingSupported) {
      setHasError(true);
      setStatusMessage(
        'Tento prohlížeč nepodporuje nahrávání zvuku přes mikrofon. Použij Safari na iOS nebo Chrome na Androidu.'
      );
      return;
    }

    if (isEvaluating) return;

    // Stop any playing TTS so iOS Safari switches cleanly to capture mode
    try {
      window.speechSynthesis?.cancel();
    } catch {
      // Ignore
    }

    cleanupRecording(true);
    discardRef.current = false;
    setHasError(false);
    setScorePercent(null);
    setStatusMessage(null);
    setHasSpoken(false);
    setMicLevel(0);

    // Create/resume AudioContext synchronously on user tap for iOS Safari compatibility
    const AudioCtx = getAudioContextCtor();
    let audioCtx: AudioContext | null = null;
    if (AudioCtx) {
      try {
        audioCtx = new AudioCtx();
        if (audioCtx.state === 'suspended') {
          void audioCtx.resume();
        }
        audioCtxRef.current = audioCtx;
      } catch {
        audioCtx = null;
      }
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      if (discardRef.current) {
        stream.getTracks().forEach((t) => t.stop());
        clearTimersAndAudioNodes();
        return;
      }
      streamRef.current = stream;

      const preferredMime = pickCompatibleAudioMimeType();
      const recorder = preferredMime
        ? new MediaRecorder(stream, { mimeType: preferredMime })
        : new MediaRecorder(stream);

      recorderRef.current = recorder;
      chunksRef.current = [];
      const actualMime = recorder.mimeType || preferredMime || 'audio/mp4';

      recorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          chunksRef.current.push(event.data);
        }
      };

      recorder.onerror = () => {
        cleanupRecording(true);
        setHasError(true);
        setStatusMessage('Nepodařilo se nahrát zvuk z mikrofonu. Zkus to prosím znovu.');
      };

      recorder.onstop = async () => {
        clearTimersAndAudioNodes();
        releaseStream();
        recorderRef.current = null;
        setIsRecording(false);
        setHasSpoken(false);
        setMicLevel(0);
        setElapsedMs(0);

        if (discardRef.current) return;

        const audioBlob = new Blob(chunksRef.current, { type: actualMime });
        chunksRef.current = [];

        if (audioBlob.size < 200) {
          setHasError(true);
          setStatusMessage('Nahrávka byla příliš krátká. Zkus říct slovíčko ještě jednou nahlas!');
          return;
        }

        setIsEvaluating(true);
        setHasError(false);
        setStatusMessage('Vyhodnocuji tvoji výslovnost...');

        try {
          const result = await evaluatePronunciationAudio(
            currentWord.englishWord,
            audioBlob,
            actualMime
          );
          if (discardRef.current) return;

          setScorePercent(result.score);
          setIsPassed(result.passed);
          setStatusMessage(result.feedback);

          setOutcomesMap((prev) => {
            const next = new Map(prev);
            if (result.passed) {
              next.set(currentWord.id, { word: currentWord, isCorrect: true });
            } else if (!next.has(currentWord.id)) {
              next.set(currentWord.id, { word: currentWord, isCorrect: false });
            }
            return next;
          });
        } catch (err) {
          if (discardRef.current) return;
          setHasError(true);
          setScorePercent(null);
          if (err instanceof MissingGeminiKeyError) {
            setStatusMessage('Zkus to prosím za chvilku znovu');
          } else {
            setStatusMessage('Zkus to prosím za chvilku znovu');
          }
        } finally {
          if (!discardRef.current) {
            setIsEvaluating(false);
          }
        }
      };

      // Set up Web Audio API AnalyserNode for real-time volume & silence detection
      let analyser: AnalyserNode | null = null;
      let sampleBuffer: Uint8Array<ArrayBuffer> | null = null;
      if (audioCtx) {
        try {
          if (audioCtx.state === 'suspended') {
            await audioCtx.resume();
          }
          const src = audioCtx.createMediaStreamSource(stream);
          sourceNodeRef.current = src;
          analyser = audioCtx.createAnalyser();
          analyser.fftSize = 1024;
          analyser.smoothingTimeConstant = 0.25;
          src.connect(analyser);
          sampleBuffer = new Uint8Array(new ArrayBuffer(analyser.fftSize));
        } catch {
          analyser = null;
        }
      }

      const startedAt = Date.now();
      let detectedSpeech = false;
      let lastSpeechAt = startedAt;
      let noiseFloorSum = 0;
      let noiseFloorSamples = 0;

      setIsRecording(true);
      setElapsedMs(0);
      recorder.start();

      vadIntervalRef.current = window.setInterval(() => {
        const now = Date.now();
        setElapsedMs(Math.min(MAX_RECORD_MS, now - startedAt));

        if (!analyser || !sampleBuffer) return;

        analyser.getByteTimeDomainData(sampleBuffer);
        let sumSq = 0;
        for (let i = 0; i < sampleBuffer.length; i++) {
          const centered = (sampleBuffer[i] - 128) / 128;
          sumSq += centered * centered;
        }
        const rms = Math.sqrt(sumSq / sampleBuffer.length);
        setMicLevel(Math.min(1, rms * 5));

        // Calibrate room noise floor during initial ~240ms before setting adaptive threshold
        if (!detectedSpeech && now - startedAt < 240) {
          noiseFloorSum += rms;
          noiseFloorSamples += 1;
        }
        const ambientAvg = noiseFloorSamples > 0 ? noiseFloorSum / noiseFloorSamples : 0;
        const threshold = Math.min(
          0.075,
          Math.max(BASE_SPEECH_RMS_THRESHOLD, ambientAvg * 2.2)
        );

        if (rms >= threshold) {
          if (!detectedSpeech) {
            detectedSpeech = true;
            setHasSpoken(true);
          }
          lastSpeechAt = now;
        } else if (detectedSpeech && now - lastSpeechAt >= SILENCE_TIMEOUT_MS) {
          stopRecordingNow();
        }
      }, VAD_POLL_MS);

      // 15-second hard safety limit in case background noise prevents silence detection
      maxStopTimeoutRef.current = window.setTimeout(() => {
        stopRecordingNow();
      }, MAX_RECORD_MS);
    } catch (err) {
      cleanupRecording(true);
      setHasError(true);
      const errName = err instanceof Error ? err.name : '';
      if (errName === 'NotAllowedError' || errName === 'PermissionDeniedError') {
        setStatusMessage(
          'Pro kontrolu výslovnosti je potřeba povolit přístup k mikrofonu v prohlížeči.'
        );
      } else {
        setStatusMessage('Nepodařilo se spustit mikrofon. Zkus to prosím znovu.');
      }
    }
  };

  const handleToggleMic = () => {
    if (isEvaluating || isPassed) return;
    if (isRecording) {
      stopRecordingNow();
    } else {
      void startRecordingSession();
    }
  };

  const handleAdvance = () => {
    const nextMap = new Map(outcomesMap);
    if (!nextMap.has(currentWord.id)) {
      nextMap.set(currentWord.id, { word: currentWord, isCorrect: false });
      setOutcomesMap(nextMap);
    }
    finishOrNext(nextMap);
  };

  const progress = Math.round(((currentIndex + 1) / Math.max(1, queue.length)) * 100);
  const micBarPercent = Math.round(micLevel * 100);

  return (
    <div className="mx-auto max-w-2xl space-y-4 px-3 py-4 sm:px-4">
      <div className="flex items-center gap-2.5">
        <button
          type="button"
          onClick={() => {
            cleanupRecording(true);
            onBack();
          }}
          aria-label="Zpět"
          className="flex h-12 w-12 items-center justify-center rounded-full hover:bg-slate-200/70"
        >
          <ArrowLeft className="h-6 w-6" />
        </button>
        <div className="flex-1">
          <h2 className="font-display text-xl font-bold">Výslovnost</h2>
          <p className="text-xs font-semibold text-brand-muted">
            Slovíčko {currentIndex + 1} z {queue.length}
          </p>
        </div>
      </div>

      <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200">
        <div
          className="h-full rounded-full bg-brand-indigo transition-all"
          style={{ width: `${progress}%` }}
        />
      </div>

      <div className="flex flex-col items-center rounded-3xl bg-white p-5 shadow-sm">
        <WordIllustration
          englishWord={currentWord.englishWord}
          czechTranslation={currentWord.czechTranslation}
          imageDataUrl={currentWord.imageDataUrl}
          className="h-40 w-40"
        />

        <h3 className="mt-4 text-center font-display text-4xl font-extrabold text-brand-indigo">
          {currentWord.englishWord}
        </h3>

        <SpeakButton
          onClick={() => speakEnglish(currentWord.englishWord)}
          label="Poslechnout si vzor"
          className="mt-3"
        />
      </div>

      {!recordingSupported && (
        <div className="rounded-2xl border border-amber-300 bg-brand-amberLight p-3.5 text-center text-sm font-semibold text-brand-amberDark">
          Nahrávání zvuku přes mikrofon není v tomto prohlížeči podporováno. Doporučujeme Safari
          (iOS) nebo Chrome (Android) přes HTTPS.
        </div>
      )}

      <div className="flex flex-col items-center py-2">
        <div className="relative flex h-28 w-28 items-center justify-center">
          {isRecording && (
            <span
              className="absolute inset-0 rounded-full bg-brand-coral/35 transition-transform duration-75"
              style={{ transform: `scale(${1 + micLevel * 0.35})` }}
            />
          )}
          <button
            type="button"
            onClick={handleToggleMic}
            disabled={isPassed || isEvaluating}
            aria-label={isRecording ? 'Zastavit nahrávání' : 'Nahrát výslovnost'}
            className={`relative z-10 flex h-[88px] w-[88px] items-center justify-center rounded-full shadow-lg transition active:scale-95 disabled:opacity-80 ${
              isPassed
                ? 'bg-brand-mint text-white'
                : isRecording
                ? 'bg-brand-coral text-white'
                : 'bg-brand-indigo text-white hover:bg-brand-indigo/90'
            }`}
          >
            {!recordingSupported ? (
              <MicOff className="h-9 w-9" />
            ) : isEvaluating ? (
              <Loader2 className="h-10 w-10 animate-spin" />
            ) : isRecording ? (
              <Square className="h-8 w-8 fill-current" />
            ) : (
              <Mic className="h-10 w-10" />
            )}
          </button>
        </div>

        {isRecording && (
          <div className="mt-2 flex flex-col items-center gap-1.5">
            <div className="inline-flex items-center gap-2 rounded-full bg-brand-coralLight px-3.5 py-1 text-xs font-extrabold text-brand-coralDark">
              <span className="h-2.5 w-2.5 rounded-full bg-brand-coral animate-pulse" />
              <span>
                {hasSpoken
                  ? `Slyším tě! (${(elapsedMs / 1000).toFixed(1)} s)`
                  : `Čekám na hlas... (${(elapsedMs / 1000).toFixed(1)} s)`}
              </span>
            </div>
            <div className="h-2 w-40 overflow-hidden rounded-full bg-slate-200">
              <div
                className="h-full rounded-full bg-brand-coral transition-all duration-75"
                style={{ width: `${Math.max(8, micBarPercent)}%` }}
              />
            </div>
          </div>
        )}

        <p className="mt-2 text-center text-sm font-bold text-brand-muted">
          {isPassed
            ? 'Skvělá výslovnost!'
            : isEvaluating
            ? 'Posílám nahrávku k vyhodnocení...'
            : isRecording
            ? 'Jakmile domluvíš, nahrávání se samo zastaví (nebo klepni na čtvereček)'
            : 'Klepni na mikrofon a řekni slovo anglicky'}
        </p>
      </div>

      {statusMessage && (
        <div
          className={`rounded-2xl p-4 text-center ${
            isPassed
              ? 'bg-brand-mintLight text-brand-mintDark'
              : hasError
              ? 'border border-red-200 bg-red-50 text-red-900'
              : isEvaluating
              ? 'border border-brand-outline bg-white text-brand-ink'
              : 'border border-amber-200 bg-amber-50 text-brand-amberDark'
          }`}
        >
          <div className="flex items-center justify-center gap-2 font-bold">
            {isPassed && <Sparkles className="h-5 w-5 shrink-0 text-brand-mint" />}
            <span>{statusMessage}</span>
          </div>
          {scorePercent !== null && (
            <p className="mt-1 text-xs font-extrabold opacity-85">
              Hodnocení: {scorePercent} %
            </p>
          )}
        </div>
      )}

      <div className="flex gap-2.5">
        {!isPassed && !isRecording && !isEvaluating && (scorePercent !== null || hasError) && (
          <button
            type="button"
            onClick={() => void startRecordingSession()}
            className="flex h-[52px] flex-1 items-center justify-center gap-2 rounded-2xl bg-brand-indigo font-bold text-white shadow-sm active:scale-95"
          >
            <RotateCcw className="h-5 w-5" />
            <span>Zkusit znovu</span>
          </button>
        )}

        <button
          type="button"
          onClick={handleAdvance}
          disabled={isEvaluating}
          className={`flex h-[52px] flex-1 items-center justify-center gap-2 rounded-2xl font-bold shadow-sm transition active:scale-95 disabled:opacity-60 ${
            isPassed
              ? 'bg-brand-mint text-white'
              : 'border border-brand-outline bg-white text-brand-ink hover:bg-slate-50'
          }`}
        >
          <span>
            {currentIndex + 1 < queue.length
              ? isPassed
                ? 'Další slovíčko'
                : 'Přeskočit'
              : 'Dokončit cvičení'}
          </span>
          <ArrowRight className="h-5 w-5" />
        </button>
      </div>
    </div>
  );
};
