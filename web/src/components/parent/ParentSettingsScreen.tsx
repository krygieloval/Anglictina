import React, { useState } from 'react';
import { Check, Edit3, Lock, Plus, RotateCcw, Trash2, X } from 'lucide-react';
import { CHILD_AVATARS, getPackPalette, PACK_PALETTES } from '../../data/palettes';
import { ChildProfile, ParentSettings } from '../../types/vocab';

interface ParentSettingsScreenProps {
  profiles: ChildProfile[];
  settings: ParentSettings;
  onSelectChild: (childId: string) => void;
  onSaveChild: (data: {
    id?: string;
    name: string;
    avatarEmoji: string;
    colorIndex: number;
  }) => void;
  onDeleteChild: (childId: string) => void;
  onSavePin: (enabled: boolean, pin: string) => void;
  onResetAllData: () => void;
}

export const ParentSettingsScreen: React.FC<ParentSettingsScreenProps> = ({
  profiles,
  settings,
  onSelectChild,
  onSaveChild,
  onDeleteChild,
  onSavePin,
  onResetAllData,
}) => {
  const [pinEnabled, setPinEnabled] = useState(settings.parentPinEnabled);
  const [pinValue, setPinValue] = useState(settings.parentPin);

  const [childModalOpen, setChildModalOpen] = useState(false);
  const [editingChild, setEditingChild] = useState<ChildProfile | null>(null);
  const [childName, setChildName] = useState('');
  const [childAvatar, setChildAvatar] = useState(CHILD_AVATARS[0]);
  const [childColor, setChildColor] = useState(0);

  const [confirmReset, setConfirmReset] = useState(false);

  const openAddChild = () => {
    setEditingChild(null);
    setChildName('');
    setChildAvatar(CHILD_AVATARS[profiles.length % CHILD_AVATARS.length]);
    setChildColor(profiles.length % PACK_PALETTES.length);
    setChildModalOpen(true);
  };

  const openEditChild = (child: ChildProfile) => {
    setEditingChild(child);
    setChildName(child.name);
    setChildAvatar(child.avatarEmoji);
    setChildColor(child.colorIndex);
    setChildModalOpen(true);
  };

  return (
    <div className="mx-auto max-w-4xl space-y-5 px-3 py-4 pb-24 sm:px-4">
      <div className="flex items-center justify-between">
        <h2 className="font-display text-2xl font-bold text-brand-ink">Nastavení a profily</h2>
      </div>

      {/* Child Profiles */}
      <section className="rounded-3xl bg-white p-4 shadow-sm sm:p-5">
        <div className="flex items-center justify-between">
          <h3 className="font-display text-lg font-bold text-brand-ink">Profily dětí</h3>
          <button
            type="button"
            onClick={openAddChild}
            className="inline-flex items-center gap-1 rounded-full bg-brand-indigo px-3.5 py-1.5 text-xs font-bold text-white"
          >
            <Plus className="h-4 w-4" />
            <span>Přidat dítě</span>
          </button>
        </div>

        <div className="mt-3 space-y-2.5">
          {profiles.map((child) => {
            const isActive = child.id === settings.activeChildId;
            const palette = getPackPalette(child.colorIndex);
            return (
              <div
                key={child.id}
                className={`flex items-center gap-3 rounded-2xl border-2 p-3 ${
                  isActive
                    ? `${palette.containerBg} ${palette.border}`
                    : 'border-slate-100 bg-slate-50/70'
                }`}
              >
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white text-2xl shadow-sm">
                  {child.avatarEmoji}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="truncate font-bold text-brand-ink">{child.name}</p>
                    {isActive && (
                      <span className="rounded-full bg-brand-indigo px-2 py-0.5 text-[10px] font-extrabold text-white">
                        Aktivní
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-brand-muted">{child.totalStars} hvězdiček</p>
                </div>

                {!isActive && (
                  <button
                    type="button"
                    onClick={() => onSelectChild(child.id)}
                    className="rounded-xl bg-white px-3 py-1.5 text-xs font-bold text-brand-indigo shadow-sm"
                  >
                    Přepnout
                  </button>
                )}

                <button
                  type="button"
                  onClick={() => openEditChild(child)}
                  aria-label={`Upravit ${child.name}`}
                  className="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-brand-ink shadow-sm"
                >
                  <Edit3 className="h-4 w-4" />
                </button>

                {profiles.length > 1 && (
                  <button
                    type="button"
                    onClick={() => onDeleteChild(child.id)}
                    aria-label={`Smazat ${child.name}`}
                    className="flex h-9 w-9 items-center justify-center rounded-xl bg-red-50 text-red-600"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                )}
              </div>
            );
          })}
        </div>
      </section>

      {/* Parental PIN */}
      <section className="rounded-3xl bg-white p-4 shadow-sm sm:p-5">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-indigoLight text-brand-indigo">
              <Lock className="h-5 w-5" />
            </div>
            <div>
              <h3 className="font-display text-lg font-bold text-brand-ink">Rodičovský zámek</h3>
              <p className="text-xs text-brand-muted">
                {pinEnabled
                  ? 'Vyžaduje váš vlastní PIN kód.'
                  : 'Výchozí ochrana: násobilková otázka.'}
              </p>
            </div>
          </div>

          <button
            type="button"
            role="switch"
            aria-checked={pinEnabled}
            onClick={() => {
              const next = !pinEnabled;
              setPinEnabled(next);
              if (!next) {
                onSavePin(false, pinValue);
              }
            }}
            className={`relative h-7 w-12 rounded-full transition ${
              pinEnabled ? 'bg-brand-indigo' : 'bg-slate-300'
            }`}
          >
            <span
              className={`absolute top-1 h-5 w-5 rounded-full bg-white shadow transition ${
                pinEnabled ? 'left-6' : 'left-1'
              }`}
            />
          </button>
        </div>

        {pinEnabled && (
          <div className="mt-4 flex flex-col gap-2.5 sm:flex-row">
            <input
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={pinValue}
              onChange={(e) => setPinValue(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="Zadejte 4–6 číslic"
              className="flex-1 rounded-2xl border border-brand-outline px-4 py-2.5 text-sm font-bold outline-none focus:border-brand-indigo"
            />
            <button
              type="button"
              disabled={pinValue.length < 4}
              onClick={() => onSavePin(true, pinValue)}
              className="inline-flex items-center justify-center gap-1.5 rounded-2xl bg-brand-indigo px-5 py-2.5 text-sm font-bold text-white disabled:opacity-50"
            >
              <Check className="h-4 w-4" />
              <span>Uložit PIN</span>
            </button>
          </div>
        )}
      </section>

      {/* Local Storage / Reset */}
      <section className="rounded-3xl bg-white p-4 shadow-sm sm:p-5">
        <h3 className="font-display text-lg font-bold text-brand-ink">Uložení dat v prohlížeči</h3>
        <p className="mt-1 text-xs text-brand-muted">
          Veškeré lekce, vlastní obrázky i pokrok dětí se ukládají lokálně přímo v tomto prohlížeči
          (LocalStorage).
        </p>
        <button
          type="button"
          onClick={() => setConfirmReset(true)}
          className="mt-3 inline-flex items-center gap-1.5 rounded-2xl border border-red-200 bg-red-50 px-4 py-2 text-xs font-bold text-red-700 hover:bg-red-100"
        >
          <RotateCcw className="h-4 w-4" />
          <span>Obnovit výchozí balíčky a vymazat pokrok</span>
        </button>
      </section>

      {childModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-3xl bg-white p-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="font-display text-xl font-bold text-brand-ink">
                {editingChild ? 'Upravit profil dítěte' : 'Nové dítě'}
              </h3>
              <button
                type="button"
                onClick={() => setChildModalOpen(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-slate-100"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mt-4 space-y-4">
              <div>
                <label className="block text-xs font-bold text-brand-muted">Jméno</label>
                <input
                  type="text"
                  value={childName}
                  onChange={(e) => setChildName(e.target.value)}
                  placeholder="např. Eliška"
                  className="mt-1 w-full rounded-2xl border border-brand-outline px-3.5 py-2.5 text-sm font-bold outline-none focus:border-brand-indigo"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-brand-muted">Avatár</label>
                <div className="mt-2 flex flex-wrap gap-2">
                  {CHILD_AVATARS.map((emoji) => (
                    <button
                      key={emoji}
                      type="button"
                      onClick={() => setChildAvatar(emoji)}
                      className={`flex h-11 w-11 items-center justify-center rounded-2xl text-2xl transition ${
                        childAvatar === emoji
                          ? 'bg-brand-indigoLight ring-2 ring-brand-indigo'
                          : 'bg-slate-100'
                      }`}
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-brand-muted">Barva profilu</label>
                <div className="mt-2 flex flex-wrap gap-2.5">
                  {PACK_PALETTES.map((pal, idx) => (
                    <button
                      key={pal.accentHex}
                      type="button"
                      onClick={() => setChildColor(idx)}
                      className={`h-8 w-8 rounded-full ${pal.accentBg} ${
                        childColor === idx ? 'ring-4 ring-brand-ink/25 scale-110' : 'opacity-80'
                      }`}
                    />
                  ))}
                </div>
              </div>
            </div>

            <div className="mt-5 flex gap-3">
              <button
                type="button"
                onClick={() => setChildModalOpen(false)}
                className="flex-1 rounded-2xl border border-brand-outline py-3 font-bold text-brand-muted"
              >
                Zrušit
              </button>
              <button
                type="button"
                disabled={!childName.trim()}
                onClick={() => {
                  onSaveChild({
                    id: editingChild?.id,
                    name: childName.trim(),
                    avatarEmoji: childAvatar,
                    colorIndex: childColor,
                  });
                  setChildModalOpen(false);
                }}
                className="flex-1 rounded-2xl bg-brand-indigo py-3 font-bold text-white disabled:opacity-50"
              >
                Uložit
              </button>
            </div>
          </div>
        </div>
      )}

      {confirmReset && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
          <div className="w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl">
            <h3 className="font-display text-xl font-bold text-brand-ink">Obnovit všechna data?</h3>
            <p className="mt-2 text-sm text-brand-muted">
              Smažou se vlastní úpravy balíčků i nasbírané hvězdičky a obnoví se výchozích 12
              lekcí.
            </p>
            <div className="mt-5 flex gap-3">
              <button
                type="button"
                onClick={() => setConfirmReset(false)}
                className="flex-1 rounded-2xl border border-brand-outline py-3 font-bold text-brand-muted"
              >
                Zrušit
              </button>
              <button
                type="button"
                onClick={() => {
                  onResetAllData();
                  setConfirmReset(false);
                }}
                className="flex-1 rounded-2xl bg-red-600 py-3 font-bold text-white"
              >
                Obnovit
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
