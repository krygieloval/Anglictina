import React from 'react';
import { COLOR_HEX_MAP, NUMBER_VALUE_MAP, resolveEmoji } from '../data/wordVisuals';

interface WordIllustrationProps {
  englishWord: string;
  czechTranslation?: string;
  imageDataUrl?: string;
  isLoading?: boolean;
  className?: string;
}

const ROOM_AND_VERB_SVG_MAP: Record<string, string> = {
  bath: '/word-art/img_word_bath.svg',
  bathroom: '/word-art/img_word_bathroom.svg',
  bed: '/word-art/img_word_bed.svg',
  bedroom: '/word-art/img_word_bedroom.svg',
  'dining room': '/word-art/img_word_dining_room.svg',
  garden: '/word-art/img_word_garden.svg',
  kitchen: '/word-art/img_word_kitchen.svg',
  'living room': '/word-art/img_word_living_room.svg',
  sofa: '/word-art/img_word_sofa.svg',
  table: '/word-art/img_word_table.svg',
  catch: '/word-art/img_word_catch.svg',
  dive: '/word-art/img_word_dive.svg',
  sing: '/word-art/img_word_sing.svg',
  song: '/word-art/img_word_sing.svg',
};

function CustomWordSvg({
  englishWord,
  czechTranslation,
}: {
  englishWord: string;
  czechTranslation: string;
}): React.ReactElement | null {
  const key = englishWord.trim().toLowerCase();
  const cz = czechTranslation.trim().toLowerCase();

  // Colors (circles — Orange color vs Orange fruit disambiguated)
  if (key === 'orange' && cz.includes('oranž')) {
    const c = COLOR_HEX_MAP.orange_color;
    return (
      <svg viewBox="0 0 100 100" className="w-full h-full scale-[1.06]">
        <circle cx="50" cy="50" r="38" fill={c.fill} stroke={c.ring} strokeWidth="4" />
        <ellipse cx="38" cy="36" rx="10" ry="6" fill="white" fillOpacity="0.3" transform="rotate(-25 38 36)" />
      </svg>
    );
  }
  if (COLOR_HEX_MAP[key]) {
    const c = COLOR_HEX_MAP[key];
    return (
      <svg viewBox="0 0 100 100" className="w-full h-full scale-[1.06]">
        <circle cx="50" cy="50" r="38" fill={c.fill} stroke={c.ring} strokeWidth="4" />
        <ellipse cx="38" cy="36" rx="10" ry="6" fill="white" fillOpacity={key === 'white' ? '0' : '0.3'} transform="rotate(-25 38 36)" />
      </svg>
    );
  }

  // Numbers 1-10
  if (NUMBER_VALUE_MAP[key]) {
    const n = NUMBER_VALUE_MAP[key];
    return (
      <svg viewBox="0 0 100 100" className="w-full h-full scale-[1.06]">
        <rect x="12" y="12" width="76" height="76" rx="22" fill={n.bg} stroke={n.fg} strokeWidth="3.5" />
        <text
          x="50"
          y="67"
          textAnchor="middle"
          fill={n.fg}
          fontSize="44"
          fontWeight="800"
          fontFamily="Baloo 2, sans-serif"
        >
          {n.num}
        </text>
      </svg>
    );
  }

  // Roast chicken in Unit 7 vs live Chicken in Unit 6
  if (key === 'chicken' && (cz.includes('maso') || cz.includes('pečen'))) {
    return (
      <svg viewBox="0 0 100 100" className="w-full h-full scale-[1.06]">
        <ellipse cx="50" cy="73" rx="38" ry="12" fill="#E2E8F0" stroke="#94A3B8" strokeWidth="2.5" />
        <ellipse cx="50" cy="70" rx="32" ry="8" fill="#F8FAFC" />
        <path d="M25,62 C25,40 42,32 56,34 C72,36 78,50 75,64 C65,70 35,70 25,62 Z" fill="#D97706" stroke="#92400E" strokeWidth="2.5" />
        <path d="M38,44 C46,38 58,39 64,45" stroke="#FBBF24" strokeWidth="3" strokeLinecap="round" fill="none" />
        <path d="M68,54 L84,42" stroke="#FEF3C7" strokeWidth="6" strokeLinecap="round" />
        <circle cx="85" cy="40" r="4.5" fill="#FEF3C7" stroke="#CBD5E1" strokeWidth="1.5" />
        <circle cx="32" cy="64" r="5" fill="#22C55E" />
        <circle cx="67" cy="65" r="4.5" fill="#EF4444" />
      </svg>
    );
  }

  // Trampoline
  if (key === 'trampoline') {
    return (
      <svg viewBox="0 0 100 100" className="w-full h-full scale-[1.05]">
        <ellipse cx="50" cy="84" rx="40" ry="7" fill="#BBF7D0" />
        <path d="M16,22 C28,15 72,15 84,22 L84,56 C72,49 28,49 16,56 Z" fill="#93C5FD" fillOpacity="0.22" />
        <path d="M26,20 L26,53 M38,18 L38,51 M50,17 L50,50 M62,18 L62,51 M74,20 L74,53" stroke="#93C5FD" strokeWidth="1.2" />
        <path d="M15,20 L15,62 M34,17 L34,55 M66,17 L66,55 M85,20 L85,62" stroke="#2563EB" strokeWidth="2.8" strokeLinecap="round" />
        <path d="M15,21 C28,15 72,15 85,21" stroke="#1E3A8A" strokeWidth="2.2" fill="none" />
        <path d="M17,67 L17,81 C17,83 22,84 27,82 L27,69" stroke="#64748B" strokeWidth="3" fill="none" strokeLinecap="round" />
        <path d="M43,72 L43,85 C43,87 57,87 57,85 L57,72" stroke="#475569" strokeWidth="3" fill="none" strokeLinecap="round" />
        <path d="M73,69 L73,82 C78,84 83,83 83,81 L83,67" stroke="#64748B" strokeWidth="3" fill="none" strokeLinecap="round" />
        <path d="M10,59 C10,69 90,69 90,59 L90,65 C90,76 10,76 10,65 Z" fill="#1D4ED8" stroke="#1E3A8A" strokeWidth="1.3" />
        <ellipse cx="50" cy="59" rx="40" ry="14" fill="#3B82F6" stroke="#1E40AF" strokeWidth="1.5" />
        <ellipse cx="50" cy="59" rx="31" ry="10" fill="#94A3B8" />
        <ellipse cx="50" cy="59" rx="28" ry="8.5" fill="#1E293B" stroke="#0F172A" strokeWidth="1" />
        <ellipse cx="50" cy="59" rx="7" ry="2.2" fill="#334155" stroke="#FBBF24" strokeWidth="1" />
        <path d="M44,44 C46,38 54,38 56,44 M50,34 L50,41 M46,37 L50,34 L54,37" stroke="#F59E0B" strokeWidth="2" fill="none" strokeLinecap="round" />
      </svg>
    );
  }

  // Skipping rope
  if (key === 'skipping rope') {
    return (
      <svg viewBox="0 0 100 100" className="w-full h-full scale-[1.06]">
        <path d="M22,26 C10,55 24,86 50,86 C76,86 90,55 78,26" stroke="#F97316" strokeWidth="5.5" strokeLinecap="round" fill="none" />
        <path d="M22,26 C10,55 24,86 50,86 C76,86 90,55 78,26" stroke="#FDBA74" strokeWidth="2" strokeLinecap="round" fill="none" strokeDasharray="4 4" />
        <rect x="17" y="12" width="10" height="22" rx="4" fill="#3B82F6" stroke="#1E40AF" strokeWidth="2" />
        <rect x="73" y="12" width="10" height="22" rx="4" fill="#3B82F6" stroke="#1E40AF" strokeWidth="2" />
      </svg>
    );
  }

  // Sweater
  if (key === 'sweater') {
    return (
      <svg viewBox="0 0 100 100" className="w-full h-full scale-[1.06]">
        <path d="M34,18 L66,18 L86,38 L76,52 L68,42 L68,82 L32,82 L32,42 L24,52 L14,38 Z" fill="#EF4444" stroke="#991B1B" strokeWidth="2.5" strokeLinejoin="round" />
        <path d="M36,18 C42,26 58,26 64,18" fill="none" stroke="#FEE2E2" strokeWidth="3.5" strokeLinecap="round" />
        <path d="M32,46 L68,46 M32,56 L68,56" stroke="#FDE047" strokeWidth="4" />
        <rect x="32" y="76" width="36" height="6" rx="2" fill="#B91C1C" />
      </svg>
    );
  }

  // Skirt
  if (key === 'skirt') {
    return (
      <svg viewBox="0 0 100 100" className="w-full h-full scale-[1.06]">
        <rect x="31" y="18" width="38" height="10" rx="3" fill="#BE185D" />
        <path d="M31,28 L69,28 L84,80 C65,86 35,86 16,80 Z" fill="#F472B6" stroke="#BE185D" strokeWidth="2.5" strokeLinejoin="round" />
        <path d="M42,28 L34,82 M50,28 L50,84 M58,28 L66,82" stroke="#DB2777" strokeWidth="2" />
      </svg>
    );
  }

  return null;
}

export const WordIllustration: React.FC<WordIllustrationProps> = ({
  englishWord,
  czechTranslation = '',
  imageDataUrl = '',
  isLoading = false,
  className = 'w-32 h-32',
}) => {
  const key = englishWord.trim().toLowerCase();
  const bundledSvgUrl = !imageDataUrl ? ROOM_AND_VERB_SVG_MAP[key] : undefined;
  const customSvg = !imageDataUrl && !bundledSvgUrl
    ? CustomWordSvg({ englishWord, czechTranslation })
    : null;
  const emoji = resolveEmoji(englishWord);

  const hasArt = Boolean(imageDataUrl || bundledSvgUrl || customSvg);

  return (
    <div
      className={`word-art-container relative flex items-center justify-center overflow-hidden rounded-2xl border border-brand-outline/70 ${
        hasArt
          ? 'bg-gradient-to-br from-[#FBF8F1] to-[#F6F2E9]'
          : 'bg-gradient-to-br from-brand-indigoLight/50 to-brand-coralLight/40'
      } ${className}`}
    >
      {isLoading ? (
        <div className="flex flex-col items-center justify-center gap-1.5 p-2 text-brand-indigo">
          <div className="h-7 w-7 animate-spin rounded-full border-[3px] border-brand-indigo border-t-transparent" />
          <span className="text-xs font-bold">Načítám...</span>
        </div>
      ) : imageDataUrl ? (
        <img
          src={imageDataUrl}
          alt={czechTranslation || englishWord}
          className="h-full w-full object-cover scale-[1.06]"
        />
      ) : bundledSvgUrl ? (
        <img
          src={bundledSvgUrl}
          alt={czechTranslation || englishWord}
          className="h-full w-full object-contain"
        />
      ) : customSvg ? (
        customSvg
      ) : (
        <span
          role="img"
          aria-label={czechTranslation || englishWord}
          className="word-art-emoji select-none"
        >
          {emoji}
        </span>
      )}
    </div>
  );
};
