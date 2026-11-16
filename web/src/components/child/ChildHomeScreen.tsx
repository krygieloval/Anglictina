import React from 'react';
import { GraduationCap, Play } from 'lucide-react';
import { getPackPalette } from '../../data/palettes';
import { getExerciseMeta, PackProgressSummary, WordPack } from '../../types/vocab';

interface ChildHomeScreenProps {
  packs: WordPack[];
  packWordCounts: Map<string, number>;
  packSummaries: Map<string, PackProgressSummary>;
  onOpenPack: (packId: string) => void;
}

export const ChildHomeScreen: React.FC<ChildHomeScreenProps> = ({
  packs,
  packWordCounts,
  packSummaries,
  onOpenPack,
}) => {
  return (
    <div className="mx-auto max-w-4xl px-3 py-4 sm:px-4 sm:py-6">
      <h2 className="font-display text-2xl font-bold text-brand-ink sm:text-3xl">
        Tvoje balíčky slovíček
      </h2>

      {packs.length === 0 ? (
        <div className="mt-4 flex flex-col items-center rounded-3xl bg-white p-8 text-center shadow-sm">
          <GraduationCap className="h-12 w-12 text-brand-indigo" />
          <h3 className="mt-3 font-display text-xl font-bold text-brand-ink">
            Zatím tu nejsou žádné balíčky
          </h3>
          <p className="mt-1.5 text-sm text-brand-muted">
            Popros rodiče, aby v rodičovském režimu vytvořili první balíček slovíček!
          </p>
        </div>
      ) : (
        <div className="mt-4 grid grid-cols-1 gap-3.5 sm:grid-cols-2">
          {packs.map((pack) => {
            const palette = getPackPalette(pack.colorIndex);
            const wordCount = packWordCounts.get(pack.id) ?? 0;
            const summary = packSummaries.get(pack.id);
            const mastered = summary?.masteredWordsCount ?? 0;
            const progressPct = Math.min(
              100,
              Math.max(0, Math.round((mastered / Math.max(1, wordCount)) * 100))
            );

            return (
              <button
                key={pack.id}
                type="button"
                onClick={() => onOpenPack(pack.id)}
                className={`flex flex-col rounded-3xl ${palette.containerBg} p-4 text-left shadow-sm transition hover:shadow-md active:scale-[0.985]`}
              >
                <div className="flex w-full items-center justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <h3
                      className={`truncate font-display text-xl font-bold ${palette.onContainerText}`}
                    >
                      {pack.title}
                    </h3>
                    <p className={`text-sm font-semibold ${palette.onContainerText} opacity-80`}>
                      {wordCount} slovíček • {pack.enabledExercises.length} her
                    </p>
                  </div>
                  <div
                    className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full ${palette.accentBg} text-white shadow-sm`}
                  >
                    <Play className="h-5 w-5 fill-current ml-0.5" />
                  </div>
                </div>

                <div className="mt-3.5 h-2 w-full overflow-hidden rounded-full bg-white/70">
                  <div
                    className={`h-full rounded-full ${palette.accentBg} transition-all`}
                    style={{ width: `${progressPct}%` }}
                  />
                </div>

                <div className="mt-2.5 flex flex-wrap gap-1.5">
                  {pack.enabledExercises.map((exId) => (
                    <span
                      key={exId}
                      className={`rounded-full bg-white/75 px-2.5 py-0.5 text-xs font-bold ${palette.onContainerText}`}
                    >
                      {getExerciseMeta(exId).shortTitleCz}
                    </span>
                  ))}
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};
