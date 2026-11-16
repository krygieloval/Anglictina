import React from 'react';
import { CheckCircle2 } from 'lucide-react';
import { getPackPalette } from '../data/palettes';
import { ChildProfile } from '../types/vocab';

interface ChildProfileSwitcherModalProps {
  profiles: ChildProfile[];
  activeChildId: string;
  onSelectProfile: (childId: string) => void;
  onDismiss: () => void;
}

export const ChildProfileSwitcherModal: React.FC<ChildProfileSwitcherModalProps> = ({
  profiles,
  activeChildId,
  onSelectProfile,
  onDismiss,
}) => (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
    <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
      <h2 className="font-display text-xl font-bold text-brand-ink">Kdo teď hraje?</h2>

      <div className="mt-4 max-h-80 space-y-2.5 overflow-y-auto">
        {profiles.map((profile) => {
          const isActive = profile.id === activeChildId;
          const palette = getPackPalette(profile.colorIndex);
          return (
            <button
              key={profile.id}
              type="button"
              onClick={() => {
                onSelectProfile(profile.id);
                onDismiss();
              }}
              className={`flex w-full items-center gap-3 rounded-2xl border-2 p-3 text-left transition ${
                isActive
                  ? `${palette.containerBg} ${palette.border}`
                  : 'border-transparent bg-slate-100 hover:bg-slate-200/70'
              }`}
            >
              <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white text-2xl shadow-sm">
                {profile.avatarEmoji}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate font-bold text-brand-ink">{profile.name}</p>
                <p className="text-xs text-brand-muted">{profile.totalStars} hvězdiček</p>
              </div>
              {isActive && <CheckCircle2 className="h-6 w-6 shrink-0 text-brand-indigo" />}
            </button>
          );
        })}
      </div>

      <p className="mt-3 text-xs text-brand-muted">
        Nové profily dětí můžete přidat nebo upravit v Rodičovském režimu → Nastavení.
      </p>

      <button
        type="button"
        onClick={onDismiss}
        className="mt-4 w-full rounded-2xl bg-brand-indigoLight py-3 font-bold text-brand-indigoDark"
      >
        Zavřít
      </button>
    </div>
  </div>
);
