import React, { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, CheckCircle2, HelpCircle, Volume2 } from 'lucide-react';
import { speakEnglish } from '../../../services/speech';
import { VocabWord } from '../../../types/vocab';
import { WordIllustration } from '../../WordIllustration';
import { PracticeOutcomeItem } from '../PracticeSummaryScreen';

interface MemoryCard {
  id: string;
  word: VocabWord;
  kind: 'IMAGE' | 'WORD';
}

interface MemoryGameProps {
  words: VocabWord[];
  onBack: () => void;
  onFinish: (
    outcomes: PracticeOutcomeItem[],
    starsEarned: number,
    score: number,
    movesCount: number
  ) => void;
}

function shuffle<T>(arr: T[]): T[] {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

export const MemoryGame: React.FC<MemoryGameProps> = ({ words, onBack, onFinish }) => {
  const roundWords = useMemo(() => shuffle(words).slice(0, 6), [words]);

  const cards = useMemo<MemoryCard[]>(() => {
    const raw: MemoryCard[] = roundWords.flatMap((w) => [
      { id: `${w.id}_img`, word: w, kind: 'IMAGE' },
      { id: `${w.id}_txt`, word: w, kind: 'WORD' },
    ]);
    return shuffle(raw);
  }, [roundWords]);

  const [flippedIds, setFlippedIds] = useState<string[]>([]);
  const [matchedWordIds, setMatchedWordIds] = useState<Set<string>>(new Set());
  const [movesCount, setMovesCount] = useState(0);
  const [isChecking, setIsChecking] = useState(false);
  const [lastSpoken, setLastSpoken] = useState<string | null>(null);

  useEffect(() => {
    if (flippedIds.length !== 2) return;
    setIsChecking(true);
    const [firstId, secondId] = flippedIds;
    const first = cards.find((c) => c.id === firstId);
    const second = cards.find((c) => c.id === secondId);

    const timer = window.setTimeout(() => {
      if (first && second && first.word.id === second.word.id) {
        setMatchedWordIds((prev) => new Set(prev).add(first.word.id));
        speakEnglish(first.word.englishWord);
        setLastSpoken(first.word.englishWord);
      }
      setFlippedIds([]);
      setIsChecking(false);
    }, 700);

    return () => window.clearTimeout(timer);
  }, [flippedIds, cards]);

  useEffect(() => {
    if (roundWords.length === 0 || matchedWordIds.size < roundWords.length) return;
    const timer = window.setTimeout(() => {
      const outcomes: PracticeOutcomeItem[] = roundWords.map((w) => ({
        word: w,
        isCorrect: true,
      }));
      const stars =
        movesCount <= roundWords.length * 2 ? 3 : movesCount <= roundWords.length * 3 ? 2 : 1;
      const score =
        roundWords.length * 25 + Math.max(0, (roundWords.length * 4 - movesCount) * 5);
      onFinish(outcomes, stars, score, movesCount);
    }, 600);
    return () => window.clearTimeout(timer);
  }, [matchedWordIds, roundWords, movesCount, onFinish]);

  const handleCardTap = (card: MemoryCard) => {
    if (isChecking) return;
    if (matchedWordIds.has(card.word.id) || flippedIds.includes(card.id)) return;
    if (flippedIds.length >= 2) return;

    const nextFlipped = [...flippedIds, card.id];
    setFlippedIds(nextFlipped);

    if (card.kind === 'WORD') {
      speakEnglish(card.word.englishWord);
      setLastSpoken(card.word.englishWord);
    }

    if (nextFlipped.length === 2) {
      setMovesCount((m) => m + 1);
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
          <h2 className="font-display text-xl font-bold">Pexeso</h2>
          <p className="text-xs font-semibold text-brand-muted">
            Najito {matchedWordIds.size} z {roundWords.length} dvojic • Tahů: {movesCount}
          </p>
        </div>
      </div>

      {lastSpoken && (
        <button
          type="button"
          onClick={() => speakEnglish(lastSpoken)}
          className="flex w-full items-center justify-between rounded-2xl bg-brand-indigoLight px-4 py-2.5 text-brand-indigoDark transition active:scale-[0.99]"
        >
          <span className="text-sm font-bold">Právě slyšíš: {lastSpoken}</span>
          <Volume2 className="h-5 w-5 text-brand-indigo" />
        </button>
      )}

      <div className="grid grid-cols-3 gap-2.5 sm:grid-cols-4">
        {cards.map((card) => {
          const isMatched = matchedWordIds.has(card.word.id);
          const isFlipped = flippedIds.includes(card.id) || isMatched;

          return (
            <button
              key={card.id}
              type="button"
              onClick={() => handleCardTap(card)}
              className={`relative flex aspect-square flex-col items-center justify-center rounded-2xl border-2 p-1 shadow-sm transition active:scale-95 ${
                isMatched
                  ? 'border-brand-mint bg-brand-mintLight'
                  : isFlipped
                  ? 'border-brand-indigo bg-white'
                  : 'border-brand-indigo bg-brand-indigo text-white hover:bg-brand-indigo/95'
              }`}
            >
              {isFlipped ? (
                card.kind === 'IMAGE' ? (
                  <WordIllustration
                    englishWord={card.word.englishWord}
                    czechTranslation={card.word.czechTranslation}
                    imageDataUrl={card.word.imageDataUrl}
                    className="h-full w-full border-none bg-transparent"
                  />
                ) : (
                  <div className="flex flex-col items-center justify-center gap-1 px-1 text-center">
                    <span className="font-display text-sm font-extrabold text-brand-ink sm:text-base break-words">
                      {card.word.englishWord}
                    </span>
                    <Volume2 className="h-4 w-4 text-brand-indigo" />
                  </div>
                )
              ) : (
                <HelpCircle className="h-9 w-9 text-white/80" />
              )}

              {isMatched && (
                <CheckCircle2 className="absolute right-1.5 top-1.5 h-4 w-4 text-brand-mint" />
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
};
