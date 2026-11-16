import React, { useMemo, useState } from 'react';
import { Lock } from 'lucide-react';
import { ParentSettings } from '../types/vocab';

interface ParentalGateModalProps {
  settings: ParentSettings;
  onUnlocked: () => void;
  onDismiss: () => void;
}

export const ParentalGateModal: React.FC<ParentalGateModalProps> = ({
  settings,
  onUnlocked,
  onDismiss,
}) => {
  const usePin = settings.parentPinEnabled && settings.parentPin.trim().length > 0;
  const mathPair = useMemo(() => {
    const a = 4 + Math.floor(Math.random() * 6);
    const b = 3 + Math.floor(Math.random() * 6);
    return { a, b };
  }, []);

  const expected = usePin ? settings.parentPin : String(mathPair.a * mathPair.b);
  const [input, setInput] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (input.trim() === expected) {
      onUnlocked();
    } else {
      setError(usePin ? 'Nesprávný PIN kód.' : 'Nesprávný výsledek, zkuste to znovu.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl"
      >
        <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-brand-indigoLight text-brand-indigo">
          <Lock className="h-6 w-6" />
        </div>
        <h2 className="text-center font-display text-xl font-bold text-brand-ink">
          Vstup pro rodiče
        </h2>
        <p className="mt-2 text-center text-sm text-brand-muted">
          {usePin
            ? 'Zadejte prosím rodičovský PIN kód:'
            : `Ověření pro rodiče: Kolik je ${mathPair.a} × ${mathPair.b}?`}
        </p>

        <input
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          autoFocus
          value={input}
          onChange={(e) => {
            setInput(e.target.value.replace(/\D/g, '').slice(0, 6));
            setError(null);
          }}
          placeholder={usePin ? 'Rodičovský PIN' : 'Výsledek'}
          className="mt-4 w-full rounded-2xl border-2 border-brand-outline px-4 py-3 text-center text-lg font-bold outline-none focus:border-brand-indigo"
        />
        {error && <p className="mt-2 text-center text-xs font-bold text-red-600">{error}</p>}

        <div className="mt-5 flex gap-3">
          <button
            type="button"
            onClick={onDismiss}
            className="flex-1 rounded-2xl border border-brand-outline py-3 font-bold text-brand-muted hover:bg-slate-50"
          >
            Zpět
          </button>
          <button
            type="submit"
            className="flex-1 rounded-2xl bg-brand-indigo py-3 font-bold text-white shadow-sm hover:bg-brand-indigo/90"
          >
            Odemknout
          </button>
        </div>
      </form>
    </div>
  );
};
