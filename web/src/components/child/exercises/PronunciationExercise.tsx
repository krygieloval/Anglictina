import React, { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowLeft, ArrowRight, Mic, MicOff, Sparkles } from 'lucide-react';
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
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [lastHeard, setLastHeard] = useState<string | null>(null);
  const [matchPercent, setMatchPercent] = useState<number | null>(null);
  const [isAccepted, setIsAccepted] = useState(false);
  const [outcomesMap, setOutcomesMap] = useState<Map<string, PracticeOutcomeItem>>(new Map());

  const recognizerRef = useRef<BrowserSpeechRecognition | null>(null);
  const currentWord = queue[currentIndex];

  const stopRecognizer = () => {
    if (recognizerRef.current) {
      try {
        recognizerRef.current.abort();
      } catch {
        // Ignore abort errors
      }
      recognizerRef.current = null;
    }
    setIsListening(false);
  };

  useEffect(() => {
    stopRecognizer();
    setStatusMessage(null);
    setLastHeard(null);
    setMatchPercent(null);
    setIsAccepted(false);
  }, [currentIndex]);

  useEffect(() => {
    return () => {
      stopRecognizer();
    };
  }, []);

  if (!currentWord) return null;

  const finishOrNext = (finalOutcomes: Map<string, PracticeOutcomeItem>) => {
    stopRecognizer();
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

  const handleToggleMic = () => {
    if (!speechSupported) {
      setStatusMessage(
        'Tento prohlížeč nepodporuje rozpoznávání řeči přes mikrofon. Použij Chrome na Androidu nebo Safari na iOS.'
      );
      return;
    }

    if (isListening) {
      try {
        recognizerRef.current?.stop();
      } catch {
        stopRecognizer();
      }
      return;
    }

    stopRecognizer();
    setStatusMessage('Poslouchám... Řekni anglické slovíčko nahlas!');
    setLastHeard(null);
    setMatchPercent(null);

    const rec = createEnglishSpeechRecognizer({
      onResults: (candidates) => {
        const evalResult = evaluatePronunciation(currentWord.englishWord, candidates);
        setLastHeard(evalResult.heardText || null);
        setMatchPercent(evalResult.matchPercent);
        setIsAccepted(evalResult.isAccepted);

        if (evalResult.isAccepted) {
          setStatusMessage(`Výborně! Shoda ${evalResult.matchPercent} %`);
          setOutcomesMap((prev) => {
            const next = new Map(prev);
            next.set(currentWord.id, { word: currentWord, isCorrect: true });
            return next;
          });
        } else {
          setStatusMessage(
            evalResult.heardText
              ? `Slyšel jsem „${evalResult.heardText}“ (${evalResult.matchPercent} %). Zkus to prosím ještě jednou!`
              : 'Neslyšel jsem slovíčko zřetelně, zkus to prosím znovu!'
          );
          setOutcomesMap((prev) => {
            if (prev.has(currentWord.id)) return prev;
            const next = new Map(prev);
            next.set(currentWord.id, { word: currentWord, isCorrect: false });
            return next;
          });
        }
      },
      onError: (msg) => {
        setStatusMessage(msg);
        setIsListening(false);
      },
      onEnd: () => {
        setIsListening(false);
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
    } catch {
      setIsListening(false);
      setStatusMessage('Mikrofon se nepodařilo aktivovat. Zkus to prosím znovu.');
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

  return (
    <div className="mx-auto max-w-2xl space-y-4 px-3 py-4 sm:px-4">
      <div className="flex items-center gap-2.5">
        <button
          type="button"
          onClick={() => {
            stopRecognizer();
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

        <p className="mt-2 text-center text-sm font-bold text-brand-muted">
          {isAccepted
            ? 'Skvělá výslovnost!'
            : isListening
            ? 'Naslouchám... Klepni znovu pro zastavení'
            : 'Klepni na mikrofon a řekni slovo anglicky'}
        </p>
      </div>

      {statusMessage && (
        <div
          className={`rounded-2xl p-4 text-center ${
            isAccepted
              ? 'bg-brand-mintLight text-brand-mintDark'
              : matchPercent !== null && !isAccepted
              ? 'bg-amber-50 border border-amber-200 text-brand-amberDark'
              : 'bg-white border border-brand-outline text-brand-ink'
          }`}
        >
          <div className="flex items-center justify-center gap-2 font-bold">
            {isAccepted && <Sparkles className="h-5 w-5 text-brand-mint shrink-0" />}
            <span>{statusMessage}</span>
          </div>
          {lastHeard && !isAccepted && (
            <p className="mt-1 text-xs opacity-80">Rozpoznaný text: „{lastHeard}“</p>
          )}
        </div>
      )}

      <button
        type="button"
        onClick={handleAdvance}
        className={`flex h-[52px] w-full items-center justify-center gap-2 rounded-2xl font-bold shadow-sm transition active:scale-95 ${
          isAccepted
            ? 'bg-brand-mint text-white'
            : 'border border-brand-outline bg-white text-brand-ink hover:bg-slate-50'
        }`}
      >
        <span>
          {currentIndex + 1 < queue.length
            ? isAccepted
              ? 'Další slovíčko'
              : 'Přeskočit slovíčko'
            : 'Dokončit cvičení'}
        </span>
        <ArrowRight className="h-5 w-5" />
      </button>
    </div>
  );
};
