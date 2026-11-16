import React, { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowLeft, ArrowRight, Mic, MicOff, RotateCcw, Sparkles } from 'lucide-react';
import {
  BrowserSpeechRecognition,
  createEnglishSpeechRecognizer,
  evaluatePronunciation,
  isWebSpeechRecognitionSupported,
  speakEnglish,
} from '../../../services/speech';
import { VocabWord } from '../../../types/vocab';
import { SpeakButton } from '../../SpeakButton';
import { WordIllustration } from '../../WordIllustration';
import { PracticeOutcomeItem } from '../PracticeSummaryScreen';

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

export const PronunciationExercise: React.FC<PronunciationExerciseProps> = ({
  words,
  onBack,
  onFinish,
}) => {
  const queue = useMemo(() => shuffle(words).slice(0, 8), [words]);
  const speechSupported = useMemo(() => isWebSpeechRecognitionSupported(), []);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [isListening, setIsListening] = useState(false);
  const [micActiveConfirmed, setMicActiveConfirmed] = useState(false);
  const [timedOutNoSpeech, setTimedOutNoSpeech] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [lastHeard, setLastHeard] = useState<string | null>(null);
  const [matchPercent, setMatchPercent] = useState<number | null>(null);
  const [isAccepted, setIsAccepted] = useState(false);
  const [outcomesMap, setOutcomesMap] = useState<Map<string, PracticeOutcomeItem>>(new Map());

  const recognizerRef = useRef<BrowserSpeechRecognition | null>(null);
  const silenceTimerRef = useRef<number | null>(null);
  const acceptedRef = useRef(false);
  const currentWord = queue[currentIndex];

  const clearSilenceTimer = () => {
    if (silenceTimerRef.current !== null) {
      window.clearTimeout(silenceTimerRef.current);
      silenceTimerRef.current = null;
    }
  };

  const abortRecognizerSafely = () => {
    clearSilenceTimer();
    const rec = recognizerRef.current;
    recognizerRef.current = null;
    if (rec) {
      rec.onstart = null;
      rec.onaudiostart = null;
      rec.onspeechstart = null;
      rec.onresult = null;
      rec.onerror = null;
      rec.onend = null;
      try {
        rec.abort();
      } catch {
        // Ignore abort errors on Safari/Chrome
      }
    }
    setIsListening(false);
    setMicActiveConfirmed(false);
  };

  useEffect(() => {
    abortRecognizerSafely();
    acceptedRef.current = false;
    setTimedOutNoSpeech(false);
    setStatusMessage(null);
    setLastHeard(null);
    setMatchPercent(null);
    setIsAccepted(false);
  }, [currentIndex]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    return () => {
      abortRecognizerSafely();
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (!currentWord) return null;

  const finishOrNext = (finalOutcomes: Map<string, PracticeOutcomeItem>) => {
    abortRecognizerSafely();
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

  const startListeningSession = () => {
    if (!speechSupported) {
      setStatusMessage(
        'Tento prohlížeč nepodporuje rozpoznávání řeči přes mikrofon. Použij Chrome na Androidu nebo Safari na iOS.'
      );
      return;
    }

    // Cancel any active TTS audio so iOS Safari frees the audio session for the mic
    try {
      window.speechSynthesis?.cancel();
    } catch {
      // Ignore
    }

    // Always abort any prior SpeechRecognition instance before starting a new one (iOS fix)
    abortRecognizerSafely();

    acceptedRef.current = false;
    setTimedOutNoSpeech(false);
    setStatusMessage('Spouštím mikrofon... Řekni slovíčko anglicky!');
    setLastHeard(null);
    setMatchPercent(null);

    const rec = createEnglishSpeechRecognizer({
      onStart: () => {
        setMicActiveConfirmed(true);
        setStatusMessage('Mikrofon naslouchá! Řekni anglické slovíčko nahlas.');
      },
      onResults: ({ candidates, isFinal }) => {
        if (acceptedRef.current) return;

        // Reset 4s no-speech timer once browser detects any speech
        clearSilenceTimer();

        const evalResult = evaluatePronunciation(currentWord.englishWord, candidates);
        if (evalResult.heardText) {
          setLastHeard(evalResult.heardText);
        }

        if (evalResult.isAccepted) {
          acceptedRef.current = true;
          setMatchPercent(evalResult.matchPercent);
          setIsAccepted(true);
          setTimedOutNoSpeech(false);
          setStatusMessage(
            evalResult.isPhoneticMatch
              ? 'Výborně! Výslovnost zní správně!'
              : `Výborně! Shoda ${evalResult.matchPercent} %`
          );
          setOutcomesMap((prev) => {
            const next = new Map(prev);
            next.set(currentWord.id, { word: currentWord, isCorrect: true });
            return next;
          });
          abortRecognizerSafely();
          return;
        }

        if (isFinal) {
          setMatchPercent(evalResult.matchPercent);
          setIsAccepted(false);
          setStatusMessage(
            evalResult.heardText
              ? `Shoda ${evalResult.matchPercent} % — zkus to říct ještě jednou!`
              : 'Neslyšel jsem slovíčko zřetelně, zkus to prosím znovu!'
          );
          setOutcomesMap((prev) => {
            if (prev.has(currentWord.id)) return prev;
            const next = new Map(prev);
            next.set(currentWord.id, { word: currentWord, isCorrect: false });
            return next;
          });
        } else {
          // Interim partial result: keep listening and re-arm 4s timeout in case user pauses
          silenceTimerRef.current = window.setTimeout(() => {
            if (!acceptedRef.current) {
              abortRecognizerSafely();
              setTimedOutNoSpeech(true);
              setStatusMessage(
                'Po 4 sekundách se nepodařilo zachytit celou výslovnost. Zkus to znovu nebo slovo přeskoč.'
              );
            }
          }, 4000);
        }
      },
      onError: (msg) => {
        clearSilenceTimer();
        setStatusMessage(msg);
        setIsListening(false);
        setMicActiveConfirmed(false);
        setTimedOutNoSpeech(true);
      },
      onEnd: () => {
        clearSilenceTimer();
        setIsListening(false);
        setMicActiveConfirmed(false);
        recognizerRef.current = null;
      },
    });

    if (!rec) {
      setStatusMessage('Nepodařilo se spustit mikrofon.');
      return;
    }

    recognizerRef.current = rec;
    try {
      setIsListening(true);
      rec.start();
      // 4-second safeguard timeout for iOS Safari / Android Chrome if browser hears nothing
      silenceTimerRef.current = window.setTimeout(() => {
        if (!acceptedRef.current) {
          abortRecognizerSafely();
          setTimedOutNoSpeech(true);
          setStatusMessage(
            'Prohlížeč po 4 sekundách nic nezachytil. Zkus to znovu nebo slovíčko přeskoč.'
          );
        }
      }, 4000);
    } catch {
      abortRecognizerSafely();
      setTimedOutNoSpeech(true);
      setStatusMessage('Mikrofon se nepodařilo aktivovat. Zkus to prosím znovu.');
    }
  };

  const handleToggleMic = () => {
    if (isListening) {
      try {
        recognizerRef.current?.stop();
      } catch {
        abortRecognizerSafely();
      }
      return;
    }
    startListeningSession();
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

  return (
    <div className="mx-auto max-w-2xl space-y-4 px-3 py-4 sm:px-4">
      <div className="flex items-center gap-2.5">
        <button
          type="button"
          onClick={() => {
            abortRecognizerSafely();
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

      {!speechSupported && (
        <div className="rounded-2xl border border-amber-300 bg-brand-amberLight p-3.5 text-center text-sm font-semibold text-brand-amberDark">
          Rozpoznávání řeči není v tomto prohlížeči podporováno. Doporučujeme Safari (iOS) nebo
          Chrome (Android). Slovíčko si můžeš poslechnout a přeskočit tlačítkem níže.
        </div>
      )}

      <div className="flex flex-col items-center py-2">
        <div className="relative flex h-28 w-28 items-center justify-center">
          {isListening && (
            <span className="absolute inset-0 rounded-full bg-brand-coral animate-mic-pulse" />
          )}
          <button
            type="button"
            onClick={handleToggleMic}
            disabled={isAccepted}
            aria-label={isListening ? 'Zastavit nahrávání' : 'Mluvit do mikrofonu'}
            className={`relative z-10 flex h-[88px] w-[88px] items-center justify-center rounded-full shadow-lg transition active:scale-95 ${
              isAccepted
                ? 'bg-brand-mint text-white'
                : isListening
                ? 'bg-brand-coral text-white'
                : 'bg-brand-indigo text-white hover:bg-brand-indigo/90'
            }`}
          >
            {speechSupported ? <Mic className="h-10 w-10" /> : <MicOff className="h-9 w-9" />}
          </button>
        </div>

        {/* Live listening indicator */}
        {isListening && (
          <div className="mt-2 inline-flex items-center gap-2 rounded-full bg-brand-coralLight px-3.5 py-1 text-xs font-extrabold text-brand-coralDark">
            <span className="flex items-end gap-0.5 h-3.5">
              <span className="w-1 h-2 bg-brand-coral rounded-full animate-bounce" />
              <span className="w-1 h-3.5 bg-brand-coral rounded-full animate-pulse" />
              <span className="w-1 h-2.5 bg-brand-coral rounded-full animate-bounce" />
            </span>
            <span>
              {micActiveConfirmed ? 'Mikrofon naslouchá (EN-US)...' : 'Připojuji mikrofon...'}
            </span>
          </div>
        )}

        <p className="mt-2 text-center text-sm font-bold text-brand-muted">
          {isAccepted
            ? 'Skvělá výslovnost!'
            : isListening
            ? 'Řekni slovo nahlas — klepnutím zastavíš'
            : 'Klepni na mikrofon a řekni slovo anglicky'}
        </p>

        {/* Small diagnostic caption showing exact browser transcript */}
        <p className="mt-1 min-h-[18px] text-center text-xs font-semibold text-brand-muted">
          {lastHeard
            ? `Slyšel jsem: „${lastHeard}“`
            : isListening
            ? 'Slyšel jsem: (čekám na hlas...)'
            : ''}
        </p>
      </div>

      {statusMessage && (
        <div
          className={`rounded-2xl p-4 text-center ${
            isAccepted
              ? 'bg-brand-mintLight text-brand-mintDark'
              : matchPercent !== null && !isAccepted
              ? 'bg-amber-50 border border-amber-200 text-brand-amberDark'
              : timedOutNoSpeech
              ? 'bg-amber-50 border border-amber-200 text-brand-amberDark'
              : 'bg-white border border-brand-outline text-brand-ink'
          }`}
        >
          <div className="flex items-center justify-center gap-2 font-bold">
            {isAccepted && <Sparkles className="h-5 w-5 text-brand-mint shrink-0" />}
            <span>{statusMessage}</span>
          </div>
        </div>
      )}

      <div className="flex gap-2.5">
        {timedOutNoSpeech && !isAccepted && speechSupported && (
          <button
            type="button"
            onClick={startListeningSession}
            className="flex h-[52px] flex-1 items-center justify-center gap-2 rounded-2xl bg-brand-indigo font-bold text-white shadow-sm active:scale-95"
          >
            <RotateCcw className="h-5 w-5" />
            <span>Zkusit znovu</span>
          </button>
        )}

        <button
          type="button"
          onClick={handleAdvance}
          className={`flex h-[52px] flex-1 items-center justify-center gap-2 rounded-2xl font-bold shadow-sm transition active:scale-95 ${
            isAccepted
              ? 'bg-brand-mint text-white'
              : 'border border-brand-outline bg-white text-brand-ink hover:bg-slate-50'
          }`}
        >
          <span>
            {currentIndex + 1 < queue.length
              ? isAccepted
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
