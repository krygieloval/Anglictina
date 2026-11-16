import React, { useEffect, useMemo, useState } from 'react';
import { BarChart3, FolderKanban, Settings } from 'lucide-react';
import { ChildProfileSwitcherModal } from './components/ChildProfileSwitcherModal';
import { ParentalGateModal } from './components/ParentalGateModal';
import { TopHeader } from './components/TopHeader';
import { ChildHomeScreen } from './components/child/ChildHomeScreen';
import { ChildPackHubScreen } from './components/child/ChildPackHubScreen';
import { PracticeOutcomeItem, PracticeSummaryScreen } from './components/child/PracticeSummaryScreen';
import { AnagramExercise } from './components/child/exercises/AnagramExercise';
import { FillBlankExercise } from './components/child/exercises/FillBlankExercise';
import { MatchingExercise } from './components/child/exercises/MatchingExercise';
import { MemoryGame } from './components/child/exercises/MemoryGame';
import { PronunciationExercise } from './components/child/exercises/PronunciationExercise';
import { QuizExercise } from './components/child/exercises/QuizExercise';
import { WordShooterGame } from './components/child/exercises/WordShooterGame';
import { ParentPackDetailScreen } from './components/parent/ParentPackDetailScreen';
import { ParentPacksScreen } from './components/parent/ParentPacksScreen';
import { ParentProgressScreen } from './components/parent/ParentProgressScreen';
import { ParentSettingsScreen } from './components/parent/ParentSettingsScreen';
import {
  computeParentAnalytics,
  createInitialStore,
  loadStore,
  makeId,
  recordPracticeToStore,
  saveStore,
} from './services/storage';
import {
  AppMode,
  ExerciseTypeId,
  ParentTab,
  SlovickaStoreData,
  WordPack,
} from './types/vocab';

type ChildScreen =
  | { kind: 'HOME' }
  | { kind: 'PACK_HUB'; packId: string }
  | { kind: 'EXERCISE'; packId: string; exerciseType: ExerciseTypeId; sessionKey: number }
  | {
      kind: 'SUMMARY';
      packId: string;
      exerciseType: ExerciseTypeId;
      outcomes: PracticeOutcomeItem[];
      starsEarned: number;
      score: number;
      movesCount?: number;
    };

export function App(): React.ReactElement {
  const [store, setStore] = useState<SlovickaStoreData>(() => loadStore());
  const [mode, setMode] = useState<AppMode>('CHILD');
  const [childScreen, setChildScreen] = useState<ChildScreen>({ kind: 'HOME' });
  const [parentTab, setParentTab] = useState<ParentTab>('PACKS');
  const [parentDetailPackId, setParentDetailPackId] = useState<string | null>(null);

  const [showGate, setShowGate] = useState(false);
  const [showProfileSwitcher, setShowProfileSwitcher] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    saveStore(store);
  }, [store]);

  useEffect(() => {
    if (!toastMessage) return;
    const t = window.setTimeout(() => setToastMessage(null), 3200);
    return () => window.clearTimeout(t);
  }, [toastMessage]);

  const activeChild = useMemo(
    () =>
      store.childProfiles.find((p) => p.id === store.settings.activeChildId) ??
      store.childProfiles[0],
    [store.childProfiles, store.settings.activeChildId]
  );

  const wordsByPack = useMemo(() => {
    const map = new Map<string, typeof store.words>();
    for (const w of store.words) {
      const list = map.get(w.packId) ?? [];
      list.push(w);
      map.set(w.packId, list);
    }
    return map;
  }, [store.words]);

  const packWordCounts = useMemo(() => {
    const map = new Map<string, number>();
    for (const [packId, list] of wordsByPack.entries()) {
      map.set(packId, list.length);
    }
    return map;
  }, [wordsByPack]);

  const childVisiblePacks = useMemo(
    () =>
      store.packs.filter(
        (p) =>
          p.assignedChildIds.length === 0 || p.assignedChildIds.includes(activeChild.id)
      ),
    [store.packs, activeChild.id]
  );

  const analytics = useMemo(
    () => computeParentAnalytics(store, activeChild.id),
    [store, activeChild.id]
  );

  const packSummariesMap = useMemo(
    () => new Map(analytics.packSummaries.map((s) => [s.pack.id, s])),
    [analytics.packSummaries]
  );

  const handleSelectChild = (childId: string) => {
    setStore((prev) => ({
      ...prev,
      settings: { ...prev.settings, activeChildId: childId },
    }));
    setChildScreen({ kind: 'HOME' });
  };

  const handleSwitchMode = (target: AppMode) => {
    if (target === 'PARENT' && mode !== 'PARENT') {
      setShowGate(true);
    } else if (target === 'CHILD') {
      setMode('CHILD');
      setChildScreen({ kind: 'HOME' });
    }
  };

  const handleExerciseFinish = (
    packId: string,
    exerciseType: ExerciseTypeId,
    outcomes: PracticeOutcomeItem[],
    starsEarned: number,
    score: number,
    movesCount?: number
  ) => {
    setStore((prev) =>
      recordPracticeToStore(
        prev,
        activeChild.id,
        exerciseType,
        outcomes,
        starsEarned,
        score
      )
    );
    setChildScreen({
      kind: 'SUMMARY',
      packId,
      exerciseType,
      outcomes,
      starsEarned,
      score,
      movesCount,
    });
  };

  const handleSavePack = (data: {
    id?: string;
    title: string;
    description: string;
    colorIndex: number;
    enabledExercises: ExerciseTypeId[];
    assignedChildIds: string[];
  }) => {
    const now = Date.now();
    if (data.id) {
      setStore((prev) => ({
        ...prev,
        packs: prev.packs.map((p) =>
          p.id === data.id ? { ...p, ...data, updatedAt: now } : p
        ),
      }));
      setToastMessage(`Balíček „${data.title}“ byl aktualizován.`);
    } else {
      const newPack: WordPack = {
        id: makeId(),
        title: data.title,
        description: data.description,
        colorIndex: data.colorIndex,
        enabledExercises: data.enabledExercises,
        assignedChildIds: data.assignedChildIds,
        createdAt: now,
        updatedAt: now,
      };
      setStore((prev) => ({ ...prev, packs: [...prev.packs, newPack] }));
      setToastMessage(`Balíček „${data.title}“ byl vytvořen.`);
    }
  };

  const handleUpdatePack = (updated: WordPack) => {
    setStore((prev) => ({
      ...prev,
      packs: prev.packs.map((p) => (p.id === updated.id ? updated : p)),
    }));
  };

  const handleDeletePack = (packId: string) => {
    setStore((prev) => ({
      ...prev,
      packs: prev.packs.filter((p) => p.id !== packId),
      words: prev.words.filter((w) => w.packId !== packId),
    }));
    if (parentDetailPackId === packId) setParentDetailPackId(null);
    setToastMessage('Balíček byl odstraněn.');
  };

  const handleSaveWord = (data: {
    id?: string;
    packId: string;
    englishWord: string;
    czechTranslation: string;
    imageDataUrl: string;
  }) => {
    const now = Date.now();
    if (data.id) {
      setStore((prev) => ({
        ...prev,
        words: prev.words.map((w) =>
          w.id === data.id
            ? {
                ...w,
                englishWord: data.englishWord,
                czechTranslation: data.czechTranslation,
                imageDataUrl: data.imageDataUrl,
                updatedAt: now,
              }
            : w
        ),
      }));
    } else {
      setStore((prev) => ({
        ...prev,
        words: [
          ...prev.words,
          {
            id: makeId(),
            packId: data.packId,
            englishWord: data.englishWord,
            czechTranslation: data.czechTranslation,
            imageDataUrl: data.imageDataUrl,
            createdAt: now,
            updatedAt: now,
          },
        ],
      }));
      setToastMessage(`Slovíčko „${data.englishWord}“ bylo přidáno.`);
    }
  };

  const handleDeleteWord = (wordId: string) => {
    setStore((prev) => ({
      ...prev,
      words: prev.words.filter((w) => w.id !== wordId),
    }));
  };

  const handleSaveChild = (data: {
    id?: string;
    name: string;
    avatarEmoji: string;
    colorIndex: number;
  }) => {
    const now = Date.now();
    if (data.id) {
      setStore((prev) => ({
        ...prev,
        childProfiles: prev.childProfiles.map((c) =>
          c.id === data.id ? { ...c, ...data, updatedAt: now } : c
        ),
      }));
    } else {
      const created = {
        id: makeId(),
        name: data.name,
        avatarEmoji: data.avatarEmoji,
        colorIndex: data.colorIndex,
        totalStars: 0,
        createdAt: now,
        updatedAt: now,
      };
      setStore((prev) => ({
        ...prev,
        childProfiles: [...prev.childProfiles, created],
        settings: { ...prev.settings, activeChildId: created.id },
      }));
    }
  };

  const handleDeleteChild = (childId: string) => {
    setStore((prev) => {
      if (prev.childProfiles.length <= 1) return prev;
      const remaining = prev.childProfiles.filter((c) => c.id !== childId);
      const nextActive =
        prev.settings.activeChildId === childId ? remaining[0].id : prev.settings.activeChildId;
      return {
        ...prev,
        childProfiles: remaining,
        wordProgress: prev.wordProgress.filter((wp) => wp.childId !== childId),
        exerciseStats: prev.exerciseStats.filter((es) => es.childId !== childId),
        settings: { ...prev.settings, activeChildId: nextActive },
      };
    });
  };

  const renderChildContent = () => {
    if (childScreen.kind === 'HOME') {
      return (
        <ChildHomeScreen
          packs={childVisiblePacks}
          packWordCounts={packWordCounts}
          packSummaries={packSummariesMap}
          onOpenPack={(packId) => setChildScreen({ kind: 'PACK_HUB', packId })}
        />
      );
    }

    const pack = store.packs.find((p) => p.id === childScreen.packId);
    const packWords = wordsByPack.get(childScreen.packId) ?? [];
    if (!pack) {
      return (
        <ChildHomeScreen
          packs={childVisiblePacks}
          packWordCounts={packWordCounts}
          packSummaries={packSummariesMap}
          onOpenPack={(packId) => setChildScreen({ kind: 'PACK_HUB', packId })}
        />
      );
    }

    if (childScreen.kind === 'PACK_HUB') {
      return (
        <ChildPackHubScreen
          pack={pack}
          words={packWords}
          onBack={() => setChildScreen({ kind: 'HOME' })}
          onStartExercise={(exerciseType) =>
            setChildScreen({
              kind: 'EXERCISE',
              packId: pack.id,
              exerciseType,
              sessionKey: Date.now(),
            })
          }
        />
      );
    }

    if (childScreen.kind === 'SUMMARY') {
      return (
        <PracticeSummaryScreen
          exerciseType={childScreen.exerciseType}
          outcomes={childScreen.outcomes}
          starsEarned={childScreen.starsEarned}
          score={childScreen.score}
          movesCount={childScreen.movesCount}
          onPlayAgain={() =>
            setChildScreen({
              kind: 'EXERCISE',
              packId: pack.id,
              exerciseType: childScreen.exerciseType,
              sessionKey: Date.now(),
            })
          }
          onDone={() => setChildScreen({ kind: 'PACK_HUB', packId: pack.id })}
        />
      );
    }

    const goBackToPack = () => setChildScreen({ kind: 'PACK_HUB', packId: pack.id });

    switch (childScreen.exerciseType) {
      case 'MATCHING':
        return (
          <MatchingExercise
            key={childScreen.sessionKey}
            words={packWords}
            onBack={goBackToPack}
            onFinish={(o, s, sc) => handleExerciseFinish(pack.id, 'MATCHING', o, s, sc)}
          />
        );
      case 'FILL_BLANK':
        return (
          <FillBlankExercise
            key={childScreen.sessionKey}
            words={packWords}
            onBack={goBackToPack}
            onFinish={(o, s, sc) => handleExerciseFinish(pack.id, 'FILL_BLANK', o, s, sc)}
          />
        );
      case 'PRONUNCIATION':
        return (
          <PronunciationExercise
            key={childScreen.sessionKey}
            words={packWords}
            onBack={goBackToPack}
            onFinish={(o, s, sc) => handleExerciseFinish(pack.id, 'PRONUNCIATION', o, s, sc)}
          />
        );
      case 'SHOOTER':
        return (
          <WordShooterGame
            key={childScreen.sessionKey}
            words={packWords}
            onBack={goBackToPack}
            onFinish={(o, s, sc) => handleExerciseFinish(pack.id, 'SHOOTER', o, s, sc)}
          />
        );
      case 'MEMORY':
        return (
          <MemoryGame
            key={childScreen.sessionKey}
            words={packWords}
            onBack={goBackToPack}
            onFinish={(o, s, sc, moves) =>
              handleExerciseFinish(pack.id, 'MEMORY', o, s, sc, moves)
            }
          />
        );
      case 'ANAGRAM':
        return (
          <AnagramExercise
            key={childScreen.sessionKey}
            words={packWords}
            onBack={goBackToPack}
            onFinish={(o, s, sc) => handleExerciseFinish(pack.id, 'ANAGRAM', o, s, sc)}
          />
        );
      case 'QUIZ':
        return (
          <QuizExercise
            key={childScreen.sessionKey}
            words={packWords}
            onBack={goBackToPack}
            onFinish={(o, s, sc) => handleExerciseFinish(pack.id, 'QUIZ', o, s, sc)}
          />
        );
    }
  };

  const renderParentContent = () => {
    if (parentTab === 'PACKS') {
      const detailPack = parentDetailPackId
        ? store.packs.find((p) => p.id === parentDetailPackId)
        : null;
      if (detailPack) {
        return (
          <ParentPackDetailScreen
            pack={detailPack}
            words={wordsByPack.get(detailPack.id) ?? []}
            profiles={store.childProfiles}
            onBack={() => setParentDetailPackId(null)}
            onUpdatePack={handleUpdatePack}
            onSaveWord={handleSaveWord}
            onDeleteWord={handleDeleteWord}
            onNotify={setToastMessage}
          />
        );
      }
      return (
        <ParentPacksScreen
          packs={store.packs}
          words={store.words}
          profiles={store.childProfiles}
          onOpenPackDetail={(id) => setParentDetailPackId(id)}
          onSavePack={handleSavePack}
          onDeletePack={handleDeletePack}
          onSaveWord={handleSaveWord}
          onDeleteWord={handleDeleteWord}
        />
      );
    }

    if (parentTab === 'PROGRESS') {
      return (
        <ParentProgressScreen
          profiles={store.childProfiles}
          activeChild={activeChild}
          analytics={analytics}
          onSelectChild={handleSelectChild}
        />
      );
    }

    return (
      <ParentSettingsScreen
        profiles={store.childProfiles}
        settings={store.settings}
        onSelectChild={handleSelectChild}
        onSaveChild={handleSaveChild}
        onDeleteChild={handleDeleteChild}
        onSavePin={(enabled, pin) => {
          setStore((prev) => ({
            ...prev,
            settings: { ...prev.settings, parentPinEnabled: enabled, parentPin: pin },
          }));
          setToastMessage(enabled ? 'Rodičovský PIN uložen.' : 'Rodičovský PIN vypnut.');
        }}
        onResetAllData={() => {
          setStore(createInitialStore());
          setParentDetailPackId(null);
          setToastMessage('Výchozí lekce byly obnoveny.');
        }}
      />
    );
  };

  return (
    <div className="min-h-screen bg-brand-skyBg text-brand-ink pb-safe">
      <TopHeader
        activeChild={activeChild}
        currentMode={mode}
        onOpenProfileSwitcher={() => setShowProfileSwitcher(true)}
        onSwitchMode={handleSwitchMode}
      />

      <main>{mode === 'CHILD' ? renderChildContent() : renderParentContent()}</main>

      {mode === 'PARENT' && (
        <nav className="fixed bottom-0 left-0 right-0 z-20 border-t border-brand-outline/70 bg-white/95 pb-safe backdrop-blur">
          <div className="mx-auto flex max-w-4xl items-center justify-around py-1.5">
            {(
              [
                { id: 'PACKS', label: 'Balíčky', Icon: FolderKanban },
                { id: 'PROGRESS', label: 'Pokrok', Icon: BarChart3 },
                { id: 'SETTINGS', label: 'Nastavení', Icon: Settings },
              ] as const
            ).map(({ id, label, Icon }) => {
              const active = parentTab === id;
              return (
                <button
                  key={id}
                  type="button"
                  onClick={() => {
                    setParentTab(id);
                    if (id !== 'PACKS') setParentDetailPackId(null);
                  }}
                  className={`flex min-w-[84px] flex-col items-center gap-0.5 rounded-2xl px-4 py-1.5 text-xs font-extrabold transition ${
                    active ? 'text-brand-indigo' : 'text-brand-muted'
                  }`}
                >
                  <Icon className="h-5 w-5" />
                  <span>{label}</span>
                </button>
              );
            })}
          </div>
        </nav>
      )}

      {toastMessage && (
        <div className="fixed bottom-20 left-1/2 z-50 -translate-x-1/2 rounded-full bg-brand-ink px-4 py-2 text-xs font-bold text-white shadow-lg">
          {toastMessage}
        </div>
      )}

      {showGate && (
        <ParentalGateModal
          settings={store.settings}
          onUnlocked={() => {
            setShowGate(false);
            setMode('PARENT');
          }}
          onDismiss={() => setShowGate(false)}
        />
      )}

      {showProfileSwitcher && (
        <ChildProfileSwitcherModal
          profiles={store.childProfiles}
          activeChildId={activeChild.id}
          onSelectProfile={handleSelectChild}
          onDismiss={() => setShowProfileSwitcher(false)}
        />
      )}
    </div>
  );
}

export default App;
