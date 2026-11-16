import React from 'react';
import { getPackPalette } from '../../data/palettes';
import { speakEnglish } from '../../services/speech';
import { ChildProfile, ParentAnalytics } from '../../types/vocab';
import { SpeakButton } from '../SpeakButton';

interface ParentProgressScreenProps {
  profiles: ChildProfile[];
  activeChild: ChildProfile;
  analytics: ParentAnalytics;
  onSelectChild: (childId: string) => void;
}

export const ParentProgressScreen: React.FC<ParentProgressScreenProps> = ({
  profiles,
  activeChild,
  analytics,
  onSelectChild,
}) => {
  return (
    <div className="mx-auto max-w-4xl space-y-5 px-3 py-4 pb-24 sm:px-4">
      <div>
        <h2 className="font-display text-2xl font-bold text-brand-ink">Pokrok a výsledky</h2>
        <div className="mt-2.5 flex gap-2 overflow-x-auto pb-1">
          {profiles.map((child) => {
            const selected = child.id === activeChild.id;
            return (
              <button
                key={child.id}
                type="button"
                onClick={() => onSelectChild(child.id)}
                className={`inline-flex shrink-0 items-center gap-1.5 rounded-full px-3.5 py-2 text-sm font-bold transition ${
                  selected
                    ? 'bg-brand-indigo text-white shadow-sm'
                    : 'bg-white text-brand-muted border border-brand-outline'
                }`}
              >
                <span>{child.avatarEmoji}</span>
                <span>{child.name}</span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="grid grid-cols-3 gap-2.5 sm:gap-3">
        <div className="rounded-3xl bg-brand-indigoLight p-4 text-brand-indigoDark">
          <p className="text-xs font-bold opacity-80">Naučeno slov</p>
          <p className="mt-1 font-display text-2xl font-extrabold sm:text-3xl">
            {analytics.learnedWordsCount}/{analytics.totalWordsCount}
          </p>
        </div>
        <div className="rounded-3xl bg-brand-mintLight p-4 text-brand-mintDark">
          <p className="text-xs font-bold opacity-80">Úspěšnost</p>
          <p className="mt-1 font-display text-2xl font-extrabold sm:text-3xl">
            {analytics.overallAccuracyPercent} %
          </p>
        </div>
        <div className="rounded-3xl bg-brand-amberLight p-4 text-brand-amberDark">
          <p className="text-xs font-bold opacity-80">Hvězdičky</p>
          <p className="mt-1 font-display text-2xl font-extrabold sm:text-3xl">
            {activeChild.totalStars}
          </p>
        </div>
      </div>

      <section className="rounded-3xl bg-white p-4 shadow-sm sm:p-5">
        <h3 className="font-display text-lg font-bold text-brand-ink">
          Úspěšnost podle typu cvičení
        </h3>
        <div className="mt-3 space-y-3">
          {analytics.exerciseBreakdown.map((item) => {
            const isMemory = item.meta.id === 'MEMORY';
            return (
              <div key={item.meta.id}>
                <div className="flex items-center justify-between text-sm font-bold">
                  <span className="text-brand-ink">{item.meta.titleCz}</span>
                  <span className="text-brand-muted">
                    {isMemory
                      ? `${item.sessionsCompleted} her`
                      : item.attempts > 0
                      ? `${item.accuracyPercent} % (${item.attempts} pokusů)`
                      : 'Zatím nehráno'}
                  </span>
                </div>
                {!isMemory && (
                  <div className="mt-1.5 h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
                    <div
                      className={`h-full rounded-full transition-all ${
                        item.accuracyPercent >= 80
                          ? 'bg-brand-mint'
                          : item.accuracyPercent >= 50
                          ? 'bg-brand-amber'
                          : 'bg-brand-coral'
                      }`}
                      style={{ width: `${item.attempts > 0 ? item.accuracyPercent : 0}%` }}
                    />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </section>

      <section className="rounded-3xl bg-white p-4 shadow-sm sm:p-5">
        <h3 className="font-display text-lg font-bold text-brand-ink">
          Slovíčka k procvičení
        </h3>
        {analytics.weakWords.length === 0 ? (
          <p className="mt-2 text-sm text-brand-muted">
            {analytics.practicedWordsCount === 0
              ? 'Zatím nebyla procvičena žádná slovíčka.'
              : 'Skvělé! Momentálně tu nejsou žádná problematická slovíčka.'}
          </p>
        ) : (
          <div className="mt-3 space-y-2.5">
            {analytics.weakWords.slice(0, 8).map((wp) => (
              <div
                key={wp.id}
                className="flex items-center justify-between gap-3 rounded-2xl bg-slate-50 px-3.5 py-2.5"
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate font-bold text-brand-ink">
                    {wp.englishWord} — {wp.czechTranslation}
                  </p>
                  <p className="text-xs font-semibold text-brand-coralDark">
                    Úspěšnost: {wp.accuracyPercent} % ({wp.correctCount}/{wp.attempts})
                  </p>
                </div>
                <SpeakButton onClick={() => speakEnglish(wp.englishWord)} />
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="space-y-2.5">
        <h3 className="font-display text-lg font-bold text-brand-ink">Přehled podle balíčků</h3>
        {analytics.packSummaries.map((summary) => {
          const palette = getPackPalette(summary.pack.colorIndex);
          return (
            <div
              key={summary.pack.id}
              className="flex items-center justify-between gap-3 rounded-2xl bg-white p-3.5 shadow-sm"
            >
              <div className="flex min-w-0 flex-1 items-center gap-3">
                <span className={`h-10 w-2 shrink-0 rounded-full ${palette.accentBg}`} />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-bold text-brand-ink">{summary.pack.title}</p>
                  <p className="text-xs text-brand-muted">
                    Naučeno {summary.masteredWordsCount} z {summary.wordCount} slov
                  </p>
                </div>
              </div>
              <span
                className={`rounded-full px-3 py-1 text-xs font-extrabold ${palette.containerBg} ${palette.onContainerText}`}
              >
                {summary.accuracyPercent} %
              </span>
            </div>
          );
        })}
      </section>
    </div>
  );
};
