import React, { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, Delete, Lightbulb, RefreshCw } from 'lucide-react';
import { speakEnglish } from '../../../services/speech';
import { VocabWord } from '../../../types/vocab';
import { SpeakButton } from '../../SpeakButton';
import { WordIllustration } from '../../WordIllustration';
import { PracticeOutcomeItem } from '../PracticeSummaryScreen';

interface AnagramTile {
  index: number;
  char: string;
}

interface AnagramExerciseProps {
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

function scrambleLetters(target: string): AnagramTile[] {
  const chars = target.split('');
  const uniqueChars = new Set(chars);
  let mixed = shuffle(chars);
  if (chars.length >= 2 && uniqueChars.size > 1) {
    let guard = 0;
    while (mixed.join('') === target && guard < 10) {
      mixed = shuffle(chars);
      guard++;
    }
  }
  return mixed.map((char, index) => ({ index, char }));
}

export const AnagramExercise: React.FC<AnagramExerciseProps> = ({
  words,
  onBack,
  onFinish,
}) => {
  const queue = useMemo(() => shuffle(words).slice(0, 10), [words]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const currentWord = queue[currentIndex];

  const targetLettersOnly = useMemo(() => {
    if (!currentWord) return '';
    return currentWord.englishWord
      .trim()
      .toUpperCase()
      .replace(/[^A-Z]/g, '');
  }, [currentWord]);

  const [tiles, setTiles] = useState<AnagramTile[]>([]);
  const [placedTiles, setPlacedTiles] = useState<AnagramTile[]>([]);
  const [hadMistake, setHadMistake] = useState(false);
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);
  const [isSolved, setIsSolved] = useState(false);
  const [outcomesMap, setOutcomesMap] = useState<Map<string, PracticeOutcomeItem>>(new Map());

  useEffect(() => {
    if (!targetLettersOnly) return;
    setTiles(scrambleLetters(targetLettersOnly));
    setPlacedTiles([]);
    setHadMistake(false);
    setFeedbackMessage(null);
    setIsSolved(false);
  }, [currentIndex, targetLettersOnly]);

  useEffect(() => {
    if (!isSolved) return;
    const t = window.setTimeout(() => {
      if (currentIndex + 1 < queue.length) {
        setCurrentIndex((i) => i + 1);
      } else {
        const allOutcomes = Array.from(outcomesMap.values());
        const correct = allOutcomes.filter((o) => o.isCorrect).length;
        const pct = Math.floor((correct * 100) / Math.max(1, queue.length));
        const stars = pct >= 85 ? 3 : pct >= 55 ? 2 : 1;
        onFinish(allOutcomes, stars, correct * 25);
      }
    }, 1000);
    return () => window.clearTimeout(t);
  }, [isSolved, currentIndex, queue.length, outcomesMap, onFinish]);

  if (!currentWord) return null;

  const progress = Math.round(((currentIndex + 1) / Math.max(1, queue.length)) * 100);
  const usedTileIndices = new Set(placedTiles.map((t) => t.index));

  const checkAttempt = (nextPlaced: AnagramTile[]) => {
    if (nextPlaced.length < targetLettersOnly.length) return;
    const formed = nextPlaced.map((t) => t.char).join('');
    if (formed === targetLettersOnly) {
      setOutcomesMap((prev) => {
        if (prev.has(currentWord.id)) return prev;
        const next = new Map(prev);
        next.set(currentWord.id, { word: currentWord, isCorrect: !hadMistake });
        return next;
      });
      setIsSolved(true);
      setFeedbackMessage('Výborně! Poskládáno správně!');
      speakEnglish(currentWord.englishWord);
    } else {
      setHadMistake(true);
      setOutcomesMap((prev) => {
        const next = new Map(prev);
        next.set(currentWord.id, { word: currentWord, isCorrect: false });
        return next;
      });
      setFeedbackMessage('Zkus písmenka přeházet jinak!');
    }
  };

  const handleTileTap = (tile: AnagramTile) => {
    if (isSolved || usedTileIndices.has(tile.index)) return;
    if (placedTiles.length >= targetLettersOnly.length) return;
    const next = [...placedTiles, tile];
    setPlacedTiles(next);
    setFeedbackMessage(null);
    checkAttempt(next);
  };

  const handleReshuffle = () => {
    if (isSolved) return;
    setPlacedTiles([]);
    setTiles(scrambleLetters(targetLettersOnly));
    setFeedbackMessage(null);
  };

  const handleDelete = () => {
    if (isSolved || placedTiles.length === 0) return;
    setPlacedTiles((prev) => prev.slice(0, -1));
    setFeedbackMessage(null);
  };

  const handleHint = () => {
    if (isSolved) return;
    let firstMismatchIdx = 0;
    while (
      firstMismatchIdx < placedTiles.length &&
      placedTiles[firstMismatchIdx].char === targetLettersOnly[firstMismatchIdx]
    ) {
      firstMismatchIdx++;
    }
    if (firstMismatchIdx >= targetLettersOnly.length) return;

    const prefix = placedTiles.slice(0, firstMismatchIdx);
    const prefixUsed = new Set(prefix.map((t) => t.index));
    const neededChar = targetLettersOnly[firstMismatchIdx];
    const matchingTile = tiles.find(
      (t) => t.char === neededChar && !prefixUsed.has(t.index)
    );
    if (!matchingTile) return;

    const next = [...prefix, matchingTile];
    setHadMistake(true);
    setPlacedTiles(next);
    setFeedbackMessage(null);
    checkAttempt(next);
  };

  return (
    <div className="mx-auto max-w-2xl space-y-4 px-3 py-4 sm:px-4">
      <div className="flex items-center gap-2.5">
        <button
          type="button"
          onClick={onBack}
          aria-label="Zpět"
          className="flex h-12 w-12 items-center justify-center rounded-full hover:bg-slate-200/70"
        >
          <ArrowLeft className="h-6 w-6" />
        </button>
        <div className="flex-1">
          <h2 className="font-display text-xl font-bold">Anagram</h2>
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
        <p className="mt-3 text-center font-display text-2xl font-extrabold text-brand-ink">
          {currentWord.czechTranslation}
        </p>
        <SpeakButton
          onClick={() => speakEnglish(currentWord.englishWord)}
          label="Poslechnout si slovo"
          className="mt-3"
        />
      </div>

      <div
        className={`flex flex-wrap justify-center gap-2 rounded-2xl border-2 p-4 ${
          isSolved ? 'border-brand-mint bg-brand-mintLight' : 'border-brand-outline bg-white'
        }`}
      >
        {Array.from({ length: targetLettersOnly.length }, (_, slotIdx) => {
          const picked = placedTiles[slotIdx];
          return (
            <button
              key={slotIdx}
              type="button"
              disabled={!picked || isSolved}
              onClick={() => {
                setPlacedTiles((prev) => prev.filter((_, idx) => idx !== slotIdx));
                setFeedbackMessage(null);
              }}
              className={`flex h-12 w-12 items-center justify-center rounded-xl border-2 font-display text-xl font-bold transition ${
                picked
                  ? isSolved
                    ? 'border-brand-mint bg-brand-mintLight text-brand-mintDark'
                    : 'border-brand-indigo bg-brand-indigoLight text-brand-indigoDark'
                  : 'border-brand-outline bg-slate-100 text-brand-muted'
              }`}
            >
              {picked ? picked.char : '_'}
            </button>
          );
        })}
      </div>

      <div className="flex flex-wrap justify-center gap-2.5">
        {tiles.map((tile) => {
          const isUsed = usedTileIndices.has(tile.index);
          return (
            <button
              key={tile.index}
              type="button"
              disabled={isUsed || isSolved}
              onClick={() => handleTileTap(tile)}
              className={`flex min-h-[52px] min-w-[52px] items-center justify-center rounded-2xl border-2 font-display text-xl font-bold transition active:scale-95 ${
                isUsed
                  ? 'border-transparent bg-slate-200/50 text-transparent'
                  : 'border-brand-indigo/40 bg-white text-brand-indigo shadow-sm'
              }`}
            >
              {isUsed ? '' : tile.char}
            </button>
          );
        })}
      </div>

      <div className="flex gap-2.5">
        <button
          type="button"
          disabled={isSolved}
          onClick={handleReshuffle}
          className="flex h-12 flex-1 items-center justify-center gap-1.5 rounded-2xl border border-brand-outline bg-white text-sm font-bold text-brand-ink disabled:opacity-45"
        >
          <RefreshCw className="h-4 w-4" />
          <span>Zamíchat</span>
        </button>
        <button
          type="button"
          disabled={placedTiles.length === 0 || isSolved}
          onClick={handleDelete}
          className="flex h-12 flex-1 items-center justify-center gap-1.5 rounded-2xl border border-brand-outline bg-white text-sm font-bold text-brand-ink disabled:opacity-45"
        >
          <Delete className="h-4 w-4" />
          <span>Smazat</span>
        </button>
        <button
          type="button"
          disabled={isSolved}
          onClick={handleHint}
          className="flex h-12 flex-1 items-center justify-center gap-1.5 rounded-2xl border border-brand-outline bg-white text-sm font-bold text-brand-ink disabled:opacity-45"
        >
          <Lightbulb className="h-4 w-4" />
          <span>Nápověda</span>
        </button>
      </div>

      {feedbackMessage && (
        <div
          className={`rounded-2xl p-3.5 text-center font-bold ${
            isSolved ? 'bg-brand-mintLight text-brand-mintDark' : 'bg-red-100 text-red-900'
          }`}
        >
          {feedbackMessage}
        </div>
      )}
    </div>
  );
};
