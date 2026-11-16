import React, { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, CheckCircle2, Volume2 } from 'lucide-react';
import { speakCzech, speakEnglish } from '../../../services/speech';
import { VocabWord } from '../../../types/vocab';
import { WordIllustration } from '../../WordIllustration';
import { PracticeOutcomeItem } from '../PracticeSummaryScreen';

interface MatchingExerciseProps {
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

export const MatchingExercise: React.FC<MatchingExerciseProps> = ({
  words,
  onBack,
  onFinish,
}) => {
  const batches = useMemo(() => {
    const picked = shuffle(words).slice(0, 10);
    const out: VocabWord[][] = [];
    for (let i = 0; i < picked.length; i += 5) {
      out.push(picked.slice(i, i + 5));
    }
    return out;
  }, [words]);

  const [batchIndex, setBatchIndex] = useState(0);
  const currentBatch = batches[batchIndex] ?? [];

  const leftItems = useMemo(() => shuffle(currentBatch), [batchIndex, currentBatch]);
  const rightItems = useMemo(() => shuffle(currentBatch), [batchIndex, currentBatch]);

  const [selectedLeftId, setSelectedLeftId] = useState<string | null>(null);
  const [selectedRightId, setSelectedRightId] = useState<string | null>(null);
  const [matchedIds, setMatchedIds] = useState<Set<string>>(new Set());
  const [wrongPair, setWrongPair] = useState<[string, string] | null>(null);
  const [totalMistakes, setTotalMistakes] = useState(0);
  const [outcomesMap, setOutcomesMap] = useState<Map<string, PracticeOutcomeItem>>(new Map());

  useEffect(() => {
    if (!selectedLeftId || !selectedRightId) return;
    const leftWord = currentBatch.find((w) => w.id === selectedLeftId);

    if (selectedLeftId === selectedRightId) {
      if (leftWord) {
        setOutcomesMap((prev) => {
          if (prev.has(leftWord.id)) return prev;
          const next = new Map(prev);
          next.set(leftWord.id, { word: leftWord, isCorrect: true });
          return next;
        });
      }
      setMatchedIds((prev) => new Set(prev).add(selectedLeftId));
      setSelectedLeftId(null);
      setSelectedRightId(null);
      if (leftWord) {
        const t = window.setTimeout(() => speakEnglish(leftWord.englishWord), 550);
        return () => window.clearTimeout(t);
      }
    } else {
      if (leftWord) {
        setOutcomesMap((prev) => {
          const next = new Map(prev);
          next.set(leftWord.id, { word: leftWord, isCorrect: false });
          return next;
        });
      }
      setTotalMistakes((m) => m + 1);
      setWrongPair([selectedLeftId, selectedRightId]);
      const t = window.setTimeout(() => {
        setWrongPair(null);
        setSelectedLeftId(null);
        setSelectedRightId(null);
      }, 550);
      return () => window.clearTimeout(t);
    }
  }, [selectedLeftId, selectedRightId, currentBatch]);

  useEffect(() => {
    if (currentBatch.length > 0 && matchedIds.size === currentBatch.length) {
      const t = window.setTimeout(() => {
        if (batchIndex + 1 < batches.length) {
          setBatchIndex((i) => i + 1);
          setMatchedIds(new Set());
          setSelectedLeftId(null);
          setSelectedRightId(null);
        } else {
          const allOutcomes = Array.from(outcomesMap.values());
          const totalWords = Math.max(1, words.length);
          const correct = allOutcomes.filter((o) => o.isCorrect).length;
          const stars =
            totalMistakes === 0 ? 3 : Math.floor((correct * 100) / totalWords) >= 60 ? 2 : 1;
          const score = Math.max(10, correct * 20 - totalMistakes * 5);
          onFinish(allOutcomes, stars, score);
        }
      }, 450);
      return () => window.clearTimeout(t);
    }
  }, [matchedIds, batchIndex, batches.length, currentBatch.length, outcomesMap, totalMistakes, words.length, onFinish]);

  const progress =
    currentBatch.length === 0 ? 0 : Math.round((matchedIds.size / currentBatch.length) * 100);

  return (
    <div className="mx-auto max-w-3xl space-y-4 px-3 py-4 sm:px-4">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onBack}
          aria-label="Zpět"
          className="flex h-12 w-12 items-center justify-center rounded-full hover:bg-slate-200/70"
        >
          <ArrowLeft className="h-6 w-6" />
        </button>
        <div className="flex-1">
          <h2 className="font-display text-xl font-bold">Spojovačka</h2>
          <p className="text-xs font-semibold text-brand-muted">
            Kolo {batchIndex + 1} z {Math.max(1, batches.length)}
          </p>
        </div>
        <span className="font-display text-base font-bold text-brand-indigo">
          {matchedIds.size}/{currentBatch.length}
        </span>
      </div>

      <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200">
        <div
          className="h-full rounded-full bg-brand-indigo transition-all"
          style={{ width: `${progress}%` }}
        />
      </div>

      <p className="text-sm font-semibold text-brand-muted">
        Ťukni na anglické slovíčko vlevo a pak na jeho obrázek a překlad vpravo!
      </p>

      <div className="grid grid-cols-2 gap-2.5 sm:gap-3">
        <div className="space-y-2.5">
          {leftItems.map((word) => {
            const isMatched = matchedIds.has(word.id);
            const isSelected = selectedLeftId === word.id;
            const isWrong = wrongPair?.[0] === word.id;
            return (
              <div
                key={word.id}
                onClick={() => {
                  if (isMatched || wrongPair) return;
                  setSelectedLeftId(word.id);
                  speakEnglish(word.englishWord);
                }}
                role="button"
                tabIndex={0}
                className={`flex h-24 cursor-pointer items-center justify-between gap-2 rounded-2xl border-2 px-3 transition select-none ${
                  isMatched
                    ? 'border-brand-mint bg-brand-mintLight text-brand-mintDark'
                    : isWrong
                    ? 'border-red-500 bg-red-100 text-red-900'
                    : isSelected
                    ? 'border-brand-indigo bg-brand-indigoLight text-brand-indigoDark'
                    : 'border-brand-outline bg-white text-brand-ink'
                }`}
              >
                <span className="line-clamp-2 font-display text-base font-bold sm:text-lg">
                  {word.englishWord}
                </span>
                {isMatched ? (
                  <CheckCircle2 className="h-5 w-5 shrink-0 text-brand-mint" />
                ) : (
                  <button
                    type="button"
                    aria-label={`Poslechnout ${word.englishWord}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      speakEnglish(word.englishWord);
                    }}
                    className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-brand-indigo hover:bg-brand-indigoLight/70"
                  >
                    <Volume2 className="h-5 w-5" />
                  </button>
                )}
              </div>
            );
          })}
        </div>

        <div className="space-y-2.5">
          {rightItems.map((word) => {
            const isMatched = matchedIds.has(word.id);
            const isSelected = selectedRightId === word.id;
            const isWrong = wrongPair?.[1] === word.id;
            return (
              <div
                key={word.id}
                onClick={() => {
                  if (isMatched || wrongPair) return;
                  setSelectedRightId(word.id);
                  speakCzech(word.czechTranslation);
                }}
                role="button"
                tabIndex={0}
                className={`flex h-24 cursor-pointer items-center gap-2.5 rounded-2xl border-2 px-2.5 transition select-none ${
                  isMatched
                    ? 'border-brand-mint bg-brand-mintLight text-brand-mintDark'
                    : isWrong
                    ? 'border-red-500 bg-red-100 text-red-900'
                    : isSelected
                    ? 'border-brand-indigo bg-brand-indigoLight text-brand-indigoDark'
                    : 'border-brand-outline bg-white text-brand-ink'
                }`}
              >
                <WordIllustration
                  englishWord={word.englishWord}
                  czechTranslation={word.czechTranslation}
                  imageDataUrl={word.imageDataUrl}
                  className="h-16 w-16 shrink-0"
                />
                <span className="line-clamp-2 text-sm font-bold sm:text-base">
                  {word.czechTranslation}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
