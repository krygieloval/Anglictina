import { STARTER_PACKS } from '../data/starterPacks';
import {
  ChildProfile,
  DEFAULT_EXERCISE_IDS,
  EXERCISE_TYPES,
  ExerciseStatSummary,
  ExerciseTypeId,
  PackProgressSummary,
  ParentAnalytics,
  SlovickaStoreData,
  VocabWord,
  WordPack,
  WordProgressComputed,
} from '../types/vocab';

const STORAGE_KEY = 'slovicka_web_store_v1';
const CURRICULUM_VERSION = 6;
export const DEFAULT_CHILD_ID = 'default_child';

function uuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `id_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 9)}`;
}

export function createInitialStore(): SlovickaStoreData {
  const now = Date.now();
  let offset = 0;

  const defaultChild: ChildProfile = {
    id: DEFAULT_CHILD_ID,
    name: 'Dítě 1',
    avatarEmoji: '🦁',
    colorIndex: 0,
    totalStars: 0,
    createdAt: now,
    updatedAt: now,
  };

  const packs: WordPack[] = [];
  const words: VocabWord[] = [];

  for (const seed of STARTER_PACKS) {
    const packId = uuid();
    const packTime = now + offset++;
    packs.push({
      id: packId,
      title: seed.title,
      description: seed.description,
      colorIndex: seed.colorIndex,
      enabledExercises: [...DEFAULT_EXERCISE_IDS],
      assignedChildIds: [],
      createdAt: packTime,
      updatedAt: packTime,
    });
    for (const w of seed.words) {
      const wordTime = now + offset++;
      words.push({
        id: uuid(),
        packId,
        englishWord: w.en,
        czechTranslation: w.cz,
        imageDataUrl: '',
        createdAt: wordTime,
        updatedAt: wordTime,
      });
    }
  }

  return {
    childProfiles: [defaultChild],
    packs,
    words,
    wordProgress: [],
    exerciseStats: [],
    settings: {
      parentPinEnabled: false,
      parentPin: '',
      activeChildId: DEFAULT_CHILD_ID,
      curriculumVersion: CURRICULUM_VERSION,
    },
  };
}

export function loadStore(): SlovickaStoreData {
  if (typeof window === 'undefined') return createInitialStore();
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      const seeded = createInitialStore();
      saveStore(seeded);
      return seeded;
    }
    const parsed = JSON.parse(raw) as Partial<SlovickaStoreData>;
    if (
      !Array.isArray(parsed.packs) ||
      !Array.isArray(parsed.words) ||
      !Array.isArray(parsed.childProfiles) ||
      parsed.childProfiles.length === 0
    ) {
      const seeded = createInitialStore();
      saveStore(seeded);
      return seeded;
    }
    return {
      childProfiles: parsed.childProfiles,
      packs: parsed.packs,
      words: parsed.words,
      wordProgress: Array.isArray(parsed.wordProgress) ? parsed.wordProgress : [],
      exerciseStats: Array.isArray(parsed.exerciseStats) ? parsed.exerciseStats : [],
      settings: parsed.settings ?? {
        parentPinEnabled: false,
        parentPin: '',
        activeChildId: parsed.childProfiles[0].id,
        curriculumVersion: CURRICULUM_VERSION,
      },
    };
  } catch {
    const seeded = createInitialStore();
    saveStore(seeded);
    return seeded;
  }
}

export function saveStore(data: SlovickaStoreData): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  } catch {
    // Handle storage quota exceeded gracefully
  }
}

export function computeParentAnalytics(
  store: SlovickaStoreData,
  activeChildId: string
): ParentAnalytics {
  const childPacks = store.packs.filter(
    (p) => p.assignedChildIds.length === 0 || p.assignedChildIds.includes(activeChildId)
  );
  const wordsByPack = new Map<string, VocabWord[]>();
  for (const w of store.words) {
    const arr = wordsByPack.get(w.packId) ?? [];
    arr.push(w);
    wordsByPack.set(w.packId, arr);
  }

  const totalWordsCount = childPacks.reduce(
    (sum, p) => sum + (wordsByPack.get(p.id)?.length ?? 0),
    0
  );

  const childWordStats: WordProgressComputed[] = store.wordProgress
    .filter((wp) => wp.childId === activeChildId)
    .map((wp) => {
      const accuracyPercent =
        wp.attempts <= 0
          ? 0
          : Math.min(100, Math.max(0, Math.floor((wp.correctCount * 100) / wp.attempts)));
      const isMastered = wp.streak >= 3 || (wp.attempts >= 3 && accuracyPercent >= 80);
      const needsReview = wp.attempts >= 2 && accuracyPercent < 65;
      return {
        ...wp,
        accuracyPercent,
        isMastered,
        needsReview,
      };
    });

  const practicedWordsCount = childWordStats.filter((w) => w.attempts > 0).length;
  const learnedWordsCount = childWordStats.filter((w) => w.isMastered).length;

  const totalAttempts = childWordStats.reduce((s, w) => s + w.attempts, 0);
  const totalCorrect = childWordStats.reduce((s, w) => s + w.correctCount, 0);
  const overallAccuracyPercent =
    totalAttempts > 0 ? Math.min(100, Math.floor((totalCorrect * 100) / totalAttempts)) : 0;

  const weakWords = childWordStats
    .filter((w) => w.attempts > 0 && (w.needsReview || w.accuracyPercent < 75))
    .sort((a, b) => a.accuracyPercent - b.accuracyPercent || b.attempts - a.attempts);

  const masteredWords = childWordStats
    .filter((w) => w.isMastered)
    .sort((a, b) => b.streak - a.streak);

  const statsByPack = new Map<string, WordProgressComputed[]>();
  for (const wp of childWordStats) {
    const arr = statsByPack.get(wp.packId) ?? [];
    arr.push(wp);
    statsByPack.set(wp.packId, arr);
  }

  const packSummaries: PackProgressSummary[] = childPacks
    .map((pack) => {
      const pStats = statsByPack.get(pack.id) ?? [];
      const attempts = pStats.reduce((s, w) => s + w.attempts, 0);
      const correct = pStats.reduce((s, w) => s + w.correctCount, 0);
      const accuracyPercent = attempts > 0 ? Math.floor((correct * 100) / attempts) : 0;
      return {
        pack,
        wordCount: wordsByPack.get(pack.id)?.length ?? 0,
        practicedWordsCount: pStats.filter((w) => w.attempts > 0).length,
        masteredWordsCount: pStats.filter((w) => w.isMastered).length,
        accuracyPercent,
      };
    })
    .sort((a, b) => a.accuracyPercent - b.accuracyPercent);

  const exStatsMap = new Map(
    store.exerciseStats
      .filter((s) => s.childId === activeChildId)
      .map((s) => [s.exerciseType, s])
  );

  const exerciseBreakdown: ExerciseStatSummary[] = EXERCISE_TYPES.map((meta) => {
    const stat = exStatsMap.get(meta.id);
    const attempts = stat?.attempts ?? 0;
    const correctCount = stat?.correctCount ?? 0;
    return {
      meta,
      attempts,
      correctCount,
      sessionsCompleted: stat?.sessionsCompleted ?? 0,
      bestScore: stat?.bestScore ?? 0,
      accuracyPercent: attempts > 0 ? Math.min(100, Math.floor((correctCount * 100) / attempts)) : 0,
    };
  });

  return {
    totalWordsCount,
    practicedWordsCount,
    learnedWordsCount,
    overallAccuracyPercent,
    weakWords,
    masteredWords,
    packSummaries,
    exerciseBreakdown,
  };
}

export function recordPracticeToStore(
  prev: SlovickaStoreData,
  childId: string,
  exerciseType: ExerciseTypeId,
  outcomes: Array<{ word: VocabWord; isCorrect: boolean }>,
  starsEarned: number,
  score: number
): SlovickaStoreData {
  if (outcomes.length === 0) return prev;
  const now = Date.now();
  const isMemory = exerciseType === 'MEMORY';

  const nextWordProgress = [...prev.wordProgress];
  if (!isMemory) {
    for (const { word, isCorrect } of outcomes) {
      const progressId = `${childId}_${word.id}`;
      const idx = nextWordProgress.findIndex((wp) => wp.id === progressId);
      if (idx >= 0) {
        const cur = nextWordProgress[idx];
        nextWordProgress[idx] = {
          ...cur,
          englishWord: word.englishWord,
          czechTranslation: word.czechTranslation,
          attempts: cur.attempts + 1,
          correctCount: cur.correctCount + (isCorrect ? 1 : 0),
          streak: isCorrect ? cur.streak + 1 : 0,
          lastPracticedAt: now,
        };
      } else {
        nextWordProgress.push({
          id: progressId,
          childId,
          wordId: word.id,
          packId: word.packId,
          englishWord: word.englishWord,
          czechTranslation: word.czechTranslation,
          attempts: 1,
          correctCount: isCorrect ? 1 : 0,
          streak: isCorrect ? 1 : 0,
          lastPracticedAt: now,
        });
      }
    }
  }

  const nextExerciseStats = [...prev.exerciseStats];
  const exId = `${childId}_${exerciseType}`;
  const exIdx = nextExerciseStats.findIndex((s) => s.id === exId);
  const deltaAttempts = isMemory ? 0 : outcomes.length;
  const deltaCorrect = isMemory ? 0 : outcomes.filter((o) => o.isCorrect).length;

  if (exIdx >= 0) {
    const cur = nextExerciseStats[exIdx];
    nextExerciseStats[exIdx] = {
      ...cur,
      attempts: cur.attempts + deltaAttempts,
      correctCount: cur.correctCount + deltaCorrect,
      sessionsCompleted: cur.sessionsCompleted + 1,
      bestScore: Math.max(cur.bestScore, score),
    };
  } else {
    nextExerciseStats.push({
      id: exId,
      childId,
      exerciseType,
      attempts: deltaAttempts,
      correctCount: deltaCorrect,
      sessionsCompleted: 1,
      bestScore: score,
    });
  }

  const nextProfiles = prev.childProfiles.map((p) =>
    p.id === childId
      ? { ...p, totalStars: p.totalStars + Math.max(0, starsEarned), updatedAt: now }
      : p
  );

  return {
    ...prev,
    childProfiles: nextProfiles,
    wordProgress: nextWordProgress,
    exerciseStats: nextExerciseStats,
  };
}

export function makeId(): string {
  return uuid();
}
