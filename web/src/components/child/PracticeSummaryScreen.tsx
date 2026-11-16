import React from 'react';
import { CheckCircle2, RotateCcw, Star } from 'lucide-react';
import { ExerciseTypeId, getExerciseMeta, VocabWord } from '../../types/vocab';

export interface PracticeOutcomeItem {
  word: VocabWord;
  isCorrect: boolean;
}

interface PracticeSummaryScreenProps {
  exerciseType: ExerciseTypeId;
  outcomes: PracticeOutcomeItem[];
  starsEarned: number;
  score: number;
  movesCount?: number;
  onPlayAgain: () => void;
  onDone: () => void;
}

export const PracticeSummaryScreen: React.FC<PracticeSummaryScreenProps> = ({
  exerciseType,
  outcomes,
  starsEarned,
  score,
  movesCount,
  onPlayAgain,
  onDone,
}) => {
  const isMemory = exerciseType === 'MEMORY';
  const meta = getExerciseMeta(exerciseType);
  const correctCount = outcomes.filter((o) => o.isCorrect).length;
  const totalCount = Math.max(1, outcomes.length);
  const accuracy = Math.floor((correctCount * 100) / totalCount);

  return (
    <div className="mx-auto flex max-w-2xl flex-col items-center space-y-5 px-4 py-6">
      <div className="mt-2 flex items-center gap-3">
        {[0, 1, 2].map((idx) => {
          const earned = idx < starsEarned;
          return (
            <div
              key={idx}
              className={`flex h-16 w-16 items-center justify-center rounded-full ${
                earned ? 'bg-brand-amberLight' : 'bg-slate-200'
              }`}
            >
              <Star
                className={`h-9 w-9 ${
                  earned ? 'fill-brand-amber text-brand-amber' : 'text-slate-400'
                }`}
              />
            </div>
          );
        })}
      </div>

      <h2 className="text-center font-display text-3xl font-bold text-brand-indigo">
        {starsEarned === 3
          ? 'Skvělá práce! Jsi hvězda!'
          : starsEarned === 2
          ? 'Super! Už ti to jde!'
          : 'Dobrý pokus! Zkus to znovu!'}
      </h2>

      <p className="text-base font-bold text-brand-muted">
        {meta.titleCz} • Získáno hvězdiček: +{starsEarned}
      </p>

      <div className="grid w-full grid-cols-3 gap-3 rounded-3xl bg-white p-5 text-center shadow-sm">
        {isMemory ? (
          <>
            <div>
              <div className="font-display text-2xl font-bold text-brand-indigo">
                {movesCount ?? 0}
              </div>
              <div className="text-xs font-semibold text-brand-muted">Počet tahů</div>
            </div>
            <div>
              <div className="font-display text-2xl font-bold text-brand-mint">
                {outcomes.length}
              </div>
              <div className="text-xs font-semibold text-brand-muted">Dvojic</div>
            </div>
          </>
        ) : (
          <>
            <div>
              <div className="font-display text-2xl font-bold text-brand-indigo">
                {accuracy} %
              </div>
              <div className="text-xs font-semibold text-brand-muted">Úspěšnost</div>
            </div>
            <div>
              <div className="font-display text-2xl font-bold text-brand-mint">
                {correctCount} / {totalCount}
              </div>
              <div className="text-xs font-semibold text-brand-muted">Správně</div>
            </div>
          </>
        )}
        <div>
          <div className="font-display text-2xl font-bold text-brand-amber">{score}</div>
          <div className="text-xs font-semibold text-brand-muted">Bodů</div>
        </div>
      </div>

      {outcomes.length > 0 && (
        <div className="w-full rounded-3xl bg-white p-4 shadow-sm">
          <h3 className="mb-3 font-display text-lg font-bold text-brand-ink">
            Procvičená slovíčka
          </h3>
          <div className="space-y-2">
            {outcomes.map(({ word, isCorrect }) => {
              const ok = isMemory || isCorrect;
              return (
                <div
                  key={word.id}
                  className={`flex items-center justify-between rounded-2xl px-3.5 py-2.5 ${
                    ok
                      ? 'bg-brand-mintLight text-brand-mintDark'
                      : 'bg-red-100 text-red-900'
                  }`}
                >
                  <span className="font-bold">
                    {word.englishWord} — {word.czechTranslation}
                  </span>
                  {ok ? (
                    <CheckCircle2 className="h-5 w-5 shrink-0 text-brand-mint" />
                  ) : (
                    <span className="text-xs font-extrabold uppercase">Procvičit</span>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      <div className="flex w-full gap-3 pb-4">
        <button
          type="button"
          onClick={onPlayAgain}
          className="flex flex-1 items-center justify-center gap-2 rounded-2xl border-2 border-brand-outline bg-white py-3.5 font-bold text-brand-ink active:scale-95"
        >
          <RotateCcw className="h-5 w-5" />
          <span>Hrát znovu</span>
        </button>
        <button
          type="button"
          onClick={onDone}
          className="flex-1 rounded-2xl bg-brand-indigo py-3.5 font-bold text-white shadow-sm active:scale-95"
        >
          Zpět do balíčku
        </button>
      </div>
    </div>
  );
};
