import React, { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, CheckCircle2, Volume2, XCircle } from 'lucide-react';
import { speakEnglish } from '../../../services/speech';
import { VocabWord } from '../../../types/vocab';
import { WordIllustration } from '../../WordIllustration';
import { PracticeOutcomeItem } from '../PracticeSummaryScreen';

interface QuizExerciseProps {
  words: VocabWord[];
  onBack: () => void;
  onFinish: (outcomes: PracticeOutcomeItem[], starsEarned: number, score: number) => void;
}

interface QuizQuestion {
  target: VocabWord;
  options: VocabWord[];
}

function shuffle<T>(arr: T[]): T[] {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

export const QuizExercise: React.FC<QuizExerciseProps> = ({ words, onBack, onFinish }) => {
  const questions: QuizQuestion[] = useMemo(() => {
    const picked = shuffle(words).slice(0, 10);
    return picked.map((target) => {
      const distractors = shuffle(words.filter((w) => w.id !== target.id)).slice(0, 3);
      return {
        target,
        options: shuffle([target, ...distractors]),
      };
    });
  }, [words]);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [outcomes, setOutcomes] = useState<PracticeOutcomeItem[]>([]);

  const question = questions[currentIndex];

  useEffect(() => {
    if (!selectedId || !question) return;
    const isCorrect = selectedId === question.target.id;
    const nextOutcomes = [...outcomes, { word: question.target, isCorrect }];

    const timer = window.setTimeout(() => {
      if (currentIndex + 1 < questions.length) {
        setOutcomes(nextOutcomes);
        setSelectedId(null);
        setCurrentIndex((i) => i + 1);
      } else {
        const correct = nextOutcomes.filter((o) => o.isCorrect).length;
        const pct = Math.floor((correct * 100) / Math.max(1, questions.length));
        const stars = pct >= 85 ? 3 : pct >= 55 ? 2 : 1;
        onFinish(nextOutcomes, stars, correct * 20);
      }
    }, 950);

    return () => window.clearTimeout(timer);
  }, [selectedId]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!question) return null;

  const progress = Math.round(((currentIndex + 1) / Math.max(1, questions.length)) * 100);

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
          <h2 className="font-display text-xl font-bold">Kvíz</h2>
          <p className="text-xs font-semibold text-brand-muted">
            Otázka {currentIndex + 1} z {questions.length}
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
          englishWord={question.target.englishWord}
          czechTranslation={question.target.czechTranslation}
          imageDataUrl={question.target.imageDataUrl}
          className="h-44 w-44"
        />
        <p className="mt-4 text-center font-display text-2xl font-extrabold text-brand-ink">
          {question.target.czechTranslation}
        </p>
      </div>

      <div className="space-y-2.5">
        {question.options.map((opt) => {
          const isPicked = selectedId === opt.id;
          const isRightAnswer = opt.id === question.target.id;
          const showResult = selectedId !== null;

          let cardStyle = 'border-brand-outline/80 bg-white hover:border-brand-indigo/60';
          if (showResult && isRightAnswer) {
            cardStyle = 'border-brand-mint bg-brand-mintLight';
          } else if (showResult && isPicked && !isRightAnswer) {
            cardStyle = 'border-red-400 bg-red-100';
          }

          return (
            <div
              key={opt.id}
              className={`flex items-center gap-2 rounded-2xl border-2 p-2 shadow-sm transition ${cardStyle}`}
            >
              <button
                type="button"
                disabled={showResult}
                onClick={() => {
                  setSelectedId(opt.id);
                  speakEnglish(opt.englishWord);
                }}
                className="flex min-h-[52px] flex-1 items-center justify-between px-3 text-left font-display text-xl font-bold text-brand-ink"
              >
                <span>{opt.englishWord}</span>
                {showResult && isRightAnswer && (
                  <CheckCircle2 className="h-6 w-6 shrink-0 text-brand-mint" />
                )}
                {showResult && isPicked && !isRightAnswer && (
                  <XCircle className="h-6 w-6 shrink-0 text-red-600" />
                )}
              </button>

              <button
                type="button"
                onClick={() => speakEnglish(opt.englishWord)}
                aria-label={`Poslechnout ${opt.englishWord}`}
                className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-brand-indigoLight text-brand-indigo transition hover:bg-brand-indigoLight/80 active:scale-95"
              >
                <Volume2 className="h-5 w-5" />
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
};
