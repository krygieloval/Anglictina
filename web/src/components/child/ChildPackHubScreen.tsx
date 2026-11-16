import React from 'react';
import {
  ArrowLeft,
  BookOpen,
  Gamepad2,
  Grid2x2,
  Mic,
  Pencil,
  Play,
  Puzzle,
  Rocket,
  Shuffle,
  Volume2,
} from 'lucide-react';
import { getPackPalette } from '../../data/palettes';
import { speakEnglish } from '../../services/speech';
import { ExerciseTypeId, getExerciseMeta, VocabWord, WordPack } from '../../types/vocab';
import { WordIllustration } from '../WordIllustration';

interface ChildPackHubScreenProps {
  pack: WordPack;
  words: VocabWord[];
  onBack: () => void;
  onStartExercise: (type: ExerciseTypeId) => void;
}

function getExerciseStyle(type: ExerciseTypeId) {
  switch (type) {
    case 'MATCHING':
      return { Icon: Puzzle, bg: 'bg-[#E0F2FE]/80', accent: 'bg-[#0284C7]', iconText: 'text-[#0284C7]' };
    case 'FILL_BLANK':
      return { Icon: Pencil, bg: 'bg-[#FEF3C7]/80', accent: 'bg-[#D97706]', iconText: 'text-[#D97706]' };
    case 'PRONUNCIATION':
      return { Icon: Mic, bg: 'bg-[#EDE9FE]/80', accent: 'bg-[#7C3AED]', iconText: 'text-[#7C3AED]' };
    case 'SHOOTER':
      return { Icon: Rocket, bg: 'bg-[#FFE5DF]/80', accent: 'bg-[#FF6B4A]', iconText: 'text-[#FF6B4A]' };
    case 'MEMORY':
      return { Icon: Grid2x2, bg: 'bg-[#FCE7F3]/80', accent: 'bg-[#DB2777]', iconText: 'text-[#DB2777]' };
    case 'ANAGRAM':
      return { Icon: Shuffle, bg: 'bg-[#E0E7FF]/80', accent: 'bg-[#4F46E5]', iconText: 'text-[#4F46E5]' };
    case 'QUIZ':
      return { Icon: BookOpen, bg: 'bg-[#CCFBF1]/80', accent: 'bg-[#0D9488]', iconText: 'text-[#0D9488]' };
    default:
      return { Icon: Gamepad2, bg: 'bg-slate-100', accent: 'bg-brand-indigo', iconText: 'text-brand-indigo' };
  }
}

export const ChildPackHubScreen: React.FC<ChildPackHubScreenProps> = ({
  pack,
  words,
  onBack,
  onStartExercise,
}) => {
  const palette = getPackPalette(pack.colorIndex);

  return (
    <div className="mx-auto max-w-4xl space-y-5 px-3 py-4 sm:px-4 sm:py-6">
      <div className={`flex items-center gap-3 rounded-3xl ${palette.containerBg} p-4`}>
        <button
          type="button"
          onClick={onBack}
          aria-label="Zpět na balíčky"
          className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-white/70 ${palette.onContainerText} transition hover:bg-white`}
        >
          <ArrowLeft className="h-6 w-6" />
        </button>
        <div className="min-w-0 flex-1">
          <h2 className={`font-display text-2xl font-bold ${palette.onContainerText}`}>
            {pack.title}
          </h2>
          {pack.description ? (
            <p className={`text-sm font-semibold ${palette.onContainerText} opacity-85`}>
              {pack.description}
            </p>
          ) : null}
        </div>
      </div>

      <section className="space-y-2.5">
        <h3 className="font-display text-xl font-bold text-brand-ink">
          Vyber si způsob procvičování
        </h3>

        {words.length === 0 ? (
          <div className="rounded-2xl bg-white p-5 text-sm font-semibold text-brand-muted shadow-sm">
            V tomto balíčku zatím nejsou žádná slovíčka. Rodiče je mohou přidat v rodičovském režimu.
          </div>
        ) : (
          <div className="space-y-2.5">
            {pack.enabledExercises.map((typeId) => {
              const meta = getExerciseMeta(typeId);
              const style = getExerciseStyle(typeId);
              const needsMore =
                (typeId === 'MATCHING' || typeId === 'MEMORY') && words.length < 2;
              const Icon = style.Icon;

              return (
                <button
                  key={typeId}
                  type="button"
                  disabled={needsMore}
                  onClick={() => onStartExercise(typeId)}
                  className={`flex w-full items-center gap-3.5 rounded-2xl ${style.bg} px-4 py-3.5 text-left shadow-sm transition active:scale-[0.99] disabled:opacity-60`}
                >
                  <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white/90 shadow-sm">
                    <Icon className={`h-6 w-6 ${style.iconText}`} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="font-display text-lg font-bold text-brand-ink">
                      {meta.titleCz}
                    </div>
                    {needsMore ? (
                      <p className="text-xs font-bold text-red-600">
                        Pro hru {meta.titleCz} jsou potřeba alespoň 2 slovíčka v balíčku.
                      </p>
                    ) : null}
                  </div>
                  <div
                    className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full ${
                      needsMore ? 'bg-slate-300' : style.accent
                    } text-white`}
                  >
                    <Play className="h-6 w-6 fill-current ml-0.5" />
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </section>

      {words.length > 0 && (
        <section className="space-y-2.5 pb-4">
          <h3 className="font-display text-xl font-bold text-brand-ink">
            Kartičky ke prohlížení ({words.length})
          </h3>
          <div className="flex gap-3 overflow-x-auto pb-2 snap-x">
            {words.map((word) => (
              <div
                key={word.id}
                className="flex w-48 shrink-0 snap-start flex-col items-center rounded-3xl bg-white p-3 shadow-sm border border-brand-outline/50"
              >
                <WordIllustration
                  englishWord={word.englishWord}
                  czechTranslation={word.czechTranslation}
                  imageDataUrl={word.imageDataUrl}
                  className="h-36 w-full"
                />
                <p className="mt-2.5 w-full truncate text-center text-base font-bold text-brand-muted">
                  {word.czechTranslation}
                </p>
                <button
                  type="button"
                  onClick={() => speakEnglish(word.englishWord)}
                  className="mt-2 flex min-h-[48px] w-full items-center justify-center gap-1.5 rounded-full bg-brand-indigoLight px-3 py-2 text-brand-indigoDark transition hover:bg-brand-indigoLight/80 active:scale-95"
                >
                  <Volume2 className="h-5 w-5 shrink-0 text-brand-indigo" />
                  <span className="truncate font-display text-base font-bold">
                    {word.englishWord}
                  </span>
                </button>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
};
