import React, { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, Check, Delete, Keyboard, Lightbulb, Volume2 } from 'lucide-react';
import { speakCzech, speakEnglish } from '../../../services/speech';
import { VocabWord } from '../../../types/vocab';
import { SpeakButton } from '../../SpeakButton';
import { WordIllustration } from '../../WordIllustration';
import { PracticeOutcomeItem } from '../PracticeSummaryScreen';

interface LetterTile {
  index: number;
  char: string;
}

interface FillBlankExerciseProps {
  words: VocabWord[];
  onBack: () => void;
  onFinish: (outcomes: PracticeOutcomeItem[], starsEarned: number, score: number) => void;
}

function prefillCountForLength(len: number): number {
  if (len <= 1) return 0;
  if (len <= 5) return 1;
  return 2;
}

function choosePrefilledIndices(len: number): Set<number> {
  const count = prefillCountForLength(len);
  if (count === 0) return new Set();
  const indices = Array.from({ length: len }, (_, i) => i);
  for (let i = indices.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [indices[i], indices[j]] = [indices[j], indices[i]];
  }
  return new Set(indices.slice(0, count));
}

function shuffle<T>(arr: T[]): T[] {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

export const FillBlankExercise: React.FC<FillBlankExerciseProps> = ({
  words,
  onBack,
  onFinish,
}) => {
  const queue = useMemo(() => shuffle(words).slice(0, 10), [words]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const currentWord = queue[currentIndex];

  const [useKeyboardMode, setUseKeyboardMode] = useState(false);
  const [typedInput, setTypedInput] = useState('');
  const [placedTiles, setPlacedTiles] = useState<Record<number, LetterTile>>({});
  const [hadMistake, setHadMistake] = useState(false);
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);
  const [isSolved, setIsSolved] = useState(false);
  const [outcomesMap, setOutcomesMap] = useState<Map<string, PracticeOutcomeItem>>(new Map());

  const targetLettersOnly = useMemo(() => {
    if (!currentWord) return '';
    return currentWord.englishWord
      .trim()
      .toUpperCase()
      .replace(/[^A-Z]/g, '');
  }, [currentWord]);

  const prefilledIndices = useMemo(
    () => choosePrefilledIndices(targetLettersOnly.length),
    [currentIndex, targetLettersOnly]
  );

  const openSlots = useMemo(
    () =>
      Array.from({ length: targetLettersOnly.length }, (_, i) => i).filter(
        (i) => !prefilledIndices.has(i)
      ),
    [targetLettersOnly, prefilledIndices]
  );

  const tileBank = useMemo(() => {
    const alphabet = 'AEIOURSTLNMDPCBG';
    const extras = Array.from({ length: 3 }, () => alphabet[Math.floor(Math.random() * alphabet.length)]);
    const needed = openSlots.map((idx) => targetLettersOnly[idx]);
    return shuffle([...needed, ...extras]).map((char, index) => ({ index, char }));
  }, [currentIndex, targetLettersOnly, openSlots]);

  useEffect(() => {
    setTypedInput('');
    setPlacedTiles({});
    setHadMistake(false);
    setFeedbackMessage(null);
    setIsSolved(false);
  }, [currentIndex]);

  const buildCandidate = (placed: Record<number, LetterTile>): string =>
    Array.from({ length: targetLettersOnly.length }, (_, idx) =>
      prefilledIndices.has(idx) ? targetLettersOnly[idx] : placed[idx]?.char ?? ''
    ).join('');

  const submitAnswer = (raw: string) => {
    if (!currentWord || isSolved) return;
    const clean = raw
      .trim()
      .toUpperCase()
      .replace(/[^A-Z]/g, '');
    if (clean === targetLettersOnly) {
      setOutcomesMap((prev) => {
        if (prev.has(currentWord.id)) return prev;
        const next = new Map(prev);
        next.set(currentWord.id, { word: currentWord, isCorrect: !hadMistake });
        return next;
      });
      setIsSolved(true);
      setFeedbackMessage('Výborně! Správně!');
      speakEnglish(currentWord.englishWord);
    } else {
      setHadMistake(true);
      setOutcomesMap((prev) => {
        const next = new Map(prev);
        next.set(currentWord.id, { word: currentWord, isCorrect: false });
        return next;
      });
      setFeedbackMessage('Zkus to ještě opravit!');
    }
  };

  useEffect(() => {
    if (!isSolved) return;
    const t = window.setTimeout(() => {
      if (currentIndex + 1 < queue.length) {
        setCurrentIndex((i) => i + 1);
      } else {
        const allOutcomes = Array.from(outcomesMap.values());
        const correct = allOutcomes.filter((o) => o.isCorrect).length;
        const pct = Math.floor((correct * 100) / Math.max(1, queue.length));
        const stars = pct >= 85 ? 3 : pct >= 50 ? 2 : 1;
        onFinish(allOutcomes, stars, correct * 25);
      }
    }, 1000);
    return () => window.clearTimeout(t);
  }, [isSolved, currentIndex, queue.length, outcomesMap, onFinish]);

  if (!currentWord) return null;

  const progress = Math.round(((currentIndex + 1) / Math.max(1, queue.length)) * 100);
  const usedTileIndices = new Set(Object.values(placedTiles).map((t) => t.index));

  const handleTileTap = (tile: LetterTile) => {
    if (isSolved || usedTileIndices.has(tile.index)) return;
    const nextSlot = openSlots.find((s) => placedTiles[s] === undefined);
    if (nextSlot === undefined) return;
    const next = { ...placedTiles, [nextSlot]: tile };
    setPlacedTiles(next);
    setFeedbackMessage(null);
    if (Object.keys(next).length === openSlots.length) {
      submitAnswer(buildCandidate(next));
    }
  };

  const handleDelete = () => {
    if (isSolved) return;
    const lastSlot = [...openSlots].reverse().find((s) => placedTiles[s] !== undefined);
    if (lastSlot === undefined) return;
    const next = { ...placedTiles };
    delete next[lastSlot];
    setPlacedTiles(next);
    setFeedbackMessage(null);
  };

  const handleHint = () => {
    if (isSolved) return;
    const targetSlot =
      openSlots.find((s) => placedTiles[s] === undefined) ??
      openSlots.find((s) => placedTiles[s]?.char !== targetLettersOnly[s]);
    if (targetSlot === undefined) return;

    const neededChar = targetLettersOnly[targetSlot];
    const unusedMatch = tileBank.find(
      (tile) => tile.char === neededChar && !usedTileIndices.has(tile.index)
    );
    let next: Record<number, LetterTile> | null = null;
    if (unusedMatch) {
      next = { ...placedTiles, [targetSlot]: unusedMatch };
    } else {
      const sourceSlot =
        openSlots.find(
          (s) =>
            s !== targetSlot &&
            placedTiles[s]?.char === neededChar &&
            placedTiles[s]?.char !== targetLettersOnly[s]
        ) ?? openSlots.find((s) => s !== targetSlot && placedTiles[s]?.char === neededChar);
      if (sourceSlot !== undefined) {
        const moved = placedTiles[sourceSlot];
        next = { ...placedTiles };
        delete next[sourceSlot];
        next[targetSlot] = moved;
      }
    }

    if (next) {
      setHadMistake(true);
      setPlacedTiles(next);
      setFeedbackMessage(null);
      if (Object.keys(next).length === openSlots.length) {
        submitAnswer(buildCandidate(next));
      }
    }
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
          <h2 className="font-display text-xl font-bold">Doplňovačka</h2>
          <p className="text-xs font-semibold text-brand-muted">
            Slovíčko {currentIndex + 1} z {queue.length}
          </p>
        </div>
        <button
          type="button"
          onClick={() => setUseKeyboardMode((v) => !v)}
          className={`flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-bold transition ${
            useKeyboardMode
              ? 'border-brand-indigo bg-brand-indigoLight text-brand-indigoDark'
              : 'border-brand-outline bg-white text-brand-ink'
          }`}
        >
          <Keyboard className="h-4 w-4" />
          <span>{useKeyboardMode ? 'Klávesnice' : 'Písmenka'}</span>
        </button>
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
        <button
          type="button"
          onClick={() => speakCzech(currentWord.czechTranslation)}
          className="mt-3 flex items-center gap-2 rounded-2xl bg-brand-coralLight/70 px-4 py-2 text-brand-ink transition active:scale-95"
        >
          <span className="font-display text-2xl font-bold">
            {currentWord.czechTranslation}
          </span>
          <Volume2 className="h-5 w-5 text-brand-indigo" />
        </button>
        <SpeakButton
          onClick={() => speakEnglish(currentWord.englishWord)}
          label="Poslechnout si slovo"
          className="mt-3"
        />
      </div>

      {useKeyboardMode ? (
        <div className="space-y-3">
          <div
            className={`flex flex-wrap justify-center gap-2 rounded-2xl border-2 p-3.5 ${
              isSolved
                ? 'border-brand-mint bg-brand-mintLight'
                : 'border-brand-outline bg-white'
            }`}
          >
            {Array.from({ length: targetLettersOnly.length }, (_, slotIdx) => {
              const isPrefilled = prefilledIndices.has(slotIdx);
              return (
                <div
                  key={slotIdx}
                  className={`flex h-10 w-10 items-center justify-center rounded-xl border font-display text-lg font-bold ${
                    isPrefilled
                      ? 'border-brand-mint bg-brand-mintLight text-brand-mintDark'
                      : 'border-brand-outline bg-slate-100 text-brand-muted'
                  }`}
                >
                  {isPrefilled ? targetLettersOnly[slotIdx] : '_'}
                </div>
              );
            })}
          </div>
          <input
            type="text"
            value={typedInput}
            onChange={(e) => {
              setTypedInput(e.target.value);
              setFeedbackMessage(null);
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && typedInput.trim() && !isSolved) {
                submitAnswer(typedInput);
              }
            }}
            placeholder="Napiš anglické slovíčko"
            className="w-full rounded-2xl border-2 border-brand-outline bg-white px-4 py-3 text-base font-bold outline-none focus:border-brand-indigo"
          />
          <button
            type="button"
            disabled={!typedInput.trim() || isSolved}
            onClick={() => submitAnswer(typedInput)}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-brand-indigo py-3.5 font-bold text-white disabled:opacity-50"
          >
            <Check className="h-5 w-5" />
            <span>Zkontrolovat</span>
          </button>
        </div>
      ) : (
        <>
          <div
            className={`flex flex-wrap justify-center gap-2 rounded-2xl border-2 p-4 ${
              isSolved
                ? 'border-brand-mint bg-brand-mintLight'
                : 'border-brand-outline bg-white'
            }`}
          >
            {Array.from({ length: targetLettersOnly.length }, (_, slotIdx) => {
              const isPrefilled = prefilledIndices.has(slotIdx);
              const picked = placedTiles[slotIdx];
              return (
                <button
                  key={slotIdx}
                  type="button"
                  disabled={isPrefilled || !picked || isSolved}
                  onClick={() => {
                    const next = { ...placedTiles };
                    delete next[slotIdx];
                    setPlacedTiles(next);
                    setFeedbackMessage(null);
                  }}
                  className={`flex h-12 w-12 items-center justify-center rounded-xl border-2 font-display text-xl font-bold transition ${
                    isPrefilled
                      ? 'border-brand-mint bg-brand-mintLight text-brand-mintDark'
                      : picked
                      ? isSolved
                        ? 'border-brand-mint bg-brand-mintLight text-brand-mintDark'
                        : 'border-brand-indigo bg-brand-indigoLight text-brand-indigoDark'
                      : 'border-brand-outline bg-slate-100 text-brand-muted'
                  }`}
                >
                  {isPrefilled ? targetLettersOnly[slotIdx] : picked ? picked.char : '_'}
                </button>
              );
            })}
          </div>

          <div className="flex flex-wrap justify-center gap-2.5">
            {tileBank.map((tile) => {
              const isUsed = usedTileIndices.has(tile.index);
              return (
                <button
                  key={tile.index}
                  type="button"
                  disabled={isUsed || isSolved}
                  onClick={() => handleTileTap(tile)}
                  className={`flex h-[52px] w-[52px] items-center justify-center rounded-2xl border-2 font-display text-xl font-bold transition active:scale-95 ${
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

          <div className="flex gap-3">
            <button
              type="button"
              disabled={Object.keys(placedTiles).length === 0 || isSolved}
              onClick={handleDelete}
              className="flex h-12 flex-1 items-center justify-center gap-2 rounded-2xl border border-brand-outline bg-white font-bold text-brand-ink disabled:opacity-45"
            >
              <Delete className="h-5 w-5" />
              <span>Smazat</span>
            </button>
            <button
              type="button"
              disabled={isSolved}
              onClick={handleHint}
              className="flex h-12 flex-1 items-center justify-center gap-2 rounded-2xl border border-brand-outline bg-white font-bold text-brand-ink disabled:opacity-45"
            >
              <Lightbulb className="h-5 w-5" />
              <span>Nápověda</span>
            </button>
          </div>
        </>
      )}

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
