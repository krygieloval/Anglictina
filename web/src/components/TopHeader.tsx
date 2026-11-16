import React from 'react';
import { ChevronDown, Star } from 'lucide-react';
import { getPackPalette } from '../data/palettes';
import { AppMode, ChildProfile } from '../types/vocab';

interface TopHeaderProps {
  activeChild: ChildProfile;
  currentMode: AppMode;
  onOpenProfileSwitcher: () => void;
  onSwitchMode: (target: AppMode) => void;
}

export const TopHeader: React.FC<TopHeaderProps> = ({
  activeChild,
  currentMode,
  onOpenProfileSwitcher,
  onSwitchMode,
}) => {
  const palette = getPackPalette(activeChild.colorIndex);

  return (
    <header className="sticky top-0 z-30 border-b border-brand-outline/60 bg-white/95 pt-safe backdrop-blur">
      <div className="mx-auto flex max-w-4xl items-center justify-between gap-2 px-3 py-2.5 sm:px-4 sm:py-3">
        {currentMode === 'CHILD' ? (
          <button
            type="button"
            onClick={onOpenProfileSwitcher}
            className={`flex min-w-0 flex-1 items-center gap-2.5 rounded-2xl ${palette.containerBg} px-2.5 py-1.5 text-left transition active:scale-[0.99]`}
          >
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/90 text-xl shadow-sm">
              {activeChild.avatarEmoji}
            </div>
            <div className="min-w-0 flex-1">
              <div className={`flex items-center gap-1 font-display text-base font-bold ${palette.onContainerText}`}>
                <span className="truncate">Ahoj, {activeChild.name}!</span>
                <ChevronDown className="h-4 w-4 shrink-0" />
              </div>
              <p className={`truncate text-xs ${palette.onContainerText} opacity-80`}>
                Vyber si balíček a pojď si hrát
              </p>
            </div>
          </button>
        ) : (
          <div className="min-w-0 flex-1">
            <h1 className="font-display text-lg font-bold text-brand-ink sm:text-xl">
              Rodičovský režim
            </h1>
            <p className="truncate text-xs text-brand-muted">
              Aktivní profil: {activeChild.avatarEmoji} {activeChild.name}
            </p>
          </div>
        )}

        <div className="flex shrink-0 items-center gap-2">
          <div className="flex items-center gap-1 rounded-full bg-brand-amberLight px-2.5 py-1.5 text-sm font-extrabold text-brand-amberDark">
            <Star className="h-4 w-4 fill-brand-amber text-brand-amber" />
            <span>{activeChild.totalStars}</span>
          </div>

          <div className="flex items-center rounded-full bg-slate-100 p-1">
            <button
              type="button"
              onClick={() => currentMode !== 'CHILD' && onSwitchMode('CHILD')}
              className={`rounded-full px-3 py-1.5 text-xs font-extrabold transition ${
                currentMode === 'CHILD'
                  ? 'bg-brand-indigo text-white shadow-sm'
                  : 'text-brand-muted hover:text-brand-ink'
              }`}
            >
              Dítě
            </button>
            <button
              type="button"
              onClick={() => currentMode !== 'PARENT' && onSwitchMode('PARENT')}
              className={`rounded-full px-3 py-1.5 text-xs font-extrabold transition ${
                currentMode === 'PARENT'
                  ? 'bg-brand-coral text-white shadow-sm'
                  : 'text-brand-muted hover:text-brand-ink'
              }`}
            >
              Rodič
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
