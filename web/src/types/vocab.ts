export type ExerciseTypeId =
  | 'MATCHING'
  | 'FILL_BLANK'
  | 'PRONUNCIATION'
  | 'SHOOTER'
  | 'MEMORY'
  | 'ANAGRAM'
  | 'QUIZ';

export interface ExerciseTypeMeta {
  id: ExerciseTypeId;
  titleCz: string;
  shortTitleCz: string;
  descriptionCz: string;
  skillTagCz: string;
}

export const EXERCISE_TYPES: ExerciseTypeMeta[] = [
  {
    id: 'MATCHING',
    titleCz: 'Spojovačka',
    shortTitleCz: 'Spojovačka',
    descriptionCz: 'Propoj anglické slovíčko s obrázkem a českým překladem.',
    skillTagCz: 'Čtení a význam',
  },
  {
    id: 'FILL_BLANK',
    titleCz: 'Doplňovačka',
    shortTitleCz: 'Doplňovačka',
    descriptionCz: 'Doplň chybějící písmena nebo napiš celé anglické slovíčko.',
    skillTagCz: 'Psaná forma',
  },
  {
    id: 'PRONUNCIATION',
    titleCz: 'Výslovnost',
    shortTitleCz: 'Výslovnost',
    descriptionCz: 'Poslechni si výslovnost a řekni slovíčko do mikrofonu.',
    skillTagCz: 'Mluvení',
  },
  {
    id: 'SHOOTER',
    titleCz: 'Střílečka',
    shortTitleCz: 'Střílečka',
    descriptionCz: 'Vesmírná raketa! Poslechni si české slovo a sestřel správný asteroid.',
    skillTagCz: 'Minihra',
  },
  {
    id: 'MEMORY',
    titleCz: 'Pexeso',
    shortTitleCz: 'Pexeso',
    descriptionCz: 'Otoč kartičky a najdi dvojice: obrázek a anglické slovíčko.',
    skillTagCz: 'Paměť',
  },
  {
    id: 'ANAGRAM',
    titleCz: 'Anagram',
    shortTitleCz: 'Anagram',
    descriptionCz: 'Poskládej přeházená písmenka do správného anglického slova.',
    skillTagCz: 'Přesmyčky',
  },
  {
    id: 'QUIZ',
    titleCz: 'Kvíz',
    shortTitleCz: 'Kvíz',
    descriptionCz: 'Vyber ze 4 možností správný anglický překlad k obrázku.',
    skillTagCz: 'Výběr',
  },
];

export const DEFAULT_EXERCISE_IDS: ExerciseTypeId[] = EXERCISE_TYPES.map((e) => e.id);

export function getExerciseMeta(id: ExerciseTypeId): ExerciseTypeMeta {
  return EXERCISE_TYPES.find((e) => e.id === id) ?? EXERCISE_TYPES[0];
}

export type AppMode = 'CHILD' | 'PARENT';
export type ParentTab = 'PACKS' | 'PROGRESS' | 'SETTINGS';

export interface ChildProfile {
  id: string;
  name: string;
  avatarEmoji: string;
  colorIndex: number;
  totalStars: number;
  createdAt: number;
  updatedAt: number;
}

export interface WordPack {
  id: string;
  title: string;
  description: string;
  colorIndex: number;
  enabledExercises: ExerciseTypeId[];
  assignedChildIds: string[];
  createdAt: number;
  updatedAt: number;
}

export interface VocabWord {
  id: string;
  packId: string;
  englishWord: string;
  czechTranslation: string;
  imageDataUrl: string;
  createdAt: number;
  updatedAt: number;
}

export interface WordProgress {
  id: string;
  childId: string;
  wordId: string;
  packId: string;
  englishWord: string;
  czechTranslation: string;
  attempts: number;
  correctCount: number;
  streak: number;
  lastPracticedAt: number | null;
}

export interface ExerciseTypeStats {
  id: string;
  childId: string;
  exerciseType: ExerciseTypeId;
  attempts: number;
  correctCount: number;
  sessionsCompleted: number;
  bestScore: number;
}

export interface ParentSettings {
  parentPinEnabled: boolean;
  parentPin: string;
  activeChildId: string;
  curriculumVersion: number;
}

export interface SlovickaStoreData {
  childProfiles: ChildProfile[];
  packs: WordPack[];
  words: VocabWord[];
  wordProgress: WordProgress[];
  exerciseStats: ExerciseTypeStats[];
  settings: ParentSettings;
}

export interface WordProgressComputed extends WordProgress {
  accuracyPercent: number;
  isMastered: boolean;
  needsReview: boolean;
}

export interface PackProgressSummary {
  pack: WordPack;
  wordCount: number;
  practicedWordsCount: number;
  masteredWordsCount: number;
  accuracyPercent: number;
}

export interface ExerciseStatSummary {
  meta: ExerciseTypeMeta;
  attempts: number;
  correctCount: number;
  sessionsCompleted: number;
  bestScore: number;
  accuracyPercent: number;
}

export interface ParentAnalytics {
  totalWordsCount: number;
  practicedWordsCount: number;
  learnedWordsCount: number;
  overallAccuracyPercent: number;
  weakWords: WordProgressComputed[];
  masteredWords: WordProgressComputed[];
  packSummaries: PackProgressSummary[];
  exerciseBreakdown: ExerciseStatSummary[];
}

export interface StockPhotoResult {
  id: string;
  title: string;
  thumbUrl: string;
  sourceName: string;
  license: string;
  fallbackUrl?: string;
}
