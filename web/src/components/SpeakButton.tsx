import React from 'react';
import { Volume2 } from 'lucide-react';

interface SpeakButtonProps {
  onClick: () => void;
  label?: string;
  className?: string;
}

export const SpeakButton: React.FC<SpeakButtonProps> = ({
  onClick,
  label,
  className = '',
}) => (
  <button
    type="button"
    onClick={onClick}
    className={`inline-flex min-h-[44px] items-center justify-center gap-1.5 rounded-full bg-brand-indigoLight px-3.5 py-2 text-sm font-bold text-brand-indigoDark transition active:scale-95 hover:bg-brand-indigoLight/80 ${className}`}
  >
    <Volume2 className="h-5 w-5 text-brand-indigo shrink-0" />
    {label ? <span>{label}</span> : null}
  </button>
);
