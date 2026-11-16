export interface PackPalette {
  containerBg: string;
  accentBg: string;
  accentHex: string;
  onContainerText: string;
  border: string;
}

export const PACK_PALETTES: PackPalette[] = [
  {
    containerBg: 'bg-[#E0F2FE]',
    accentBg: 'bg-[#0284C7]',
    accentHex: '#0284C7',
    onContainerText: 'text-[#0C4A6E]',
    border: 'border-[#0284C7]',
  },
  {
    containerBg: 'bg-[#FEF3C7]',
    accentBg: 'bg-[#D97706]',
    accentHex: '#D97706',
    onContainerText: 'text-[#78350F]',
    border: 'border-[#D97706]',
  },
  {
    containerBg: 'bg-[#D1FAE5]',
    accentBg: 'bg-[#059669]',
    accentHex: '#059669',
    onContainerText: 'text-[#064E3B]',
    border: 'border-[#059669]',
  },
  {
    containerBg: 'bg-[#FCE7F3]',
    accentBg: 'bg-[#DB2777]',
    accentHex: '#DB2777',
    onContainerText: 'text-[#831843]',
    border: 'border-[#DB2777]',
  },
  {
    containerBg: 'bg-[#EDE9FE]',
    accentBg: 'bg-[#7C3AED]',
    accentHex: '#7C3AED',
    onContainerText: 'text-[#4C1D95]',
    border: 'border-[#7C3AED]',
  },
  {
    containerBg: 'bg-[#FFEDD5]',
    accentBg: 'bg-[#EA580C]',
    accentHex: '#EA580C',
    onContainerText: 'text-[#7C2D12]',
    border: 'border-[#EA580C]',
  },
];

export const CHILD_AVATARS = ['🦁', '🦊', '🐼', '🐰', '🐸', '🦄', '🚀', '🌟', '🐱', '🐶'];

export function getPackPalette(colorIndex: number): PackPalette {
  const idx = Math.abs(Math.floor(colorIndex)) % PACK_PALETTES.length;
  return PACK_PALETTES[idx];
}
