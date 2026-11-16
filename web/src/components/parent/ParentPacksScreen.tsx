import React, { useMemo, useRef, useState } from 'react';
import {
  ChevronRight,
  Edit3,
  ImagePlus,
  Plus,
  Search,
  Trash2,
  X,
} from 'lucide-react';
import { getPackPalette, PACK_PALETTES } from '../../data/palettes';
import { encodeImageFileToDataUrl } from '../../services/imageCodec';
import { downloadStockPhotoAsDataUrl } from '../../services/stockPhotos';
import {
  ChildProfile,
  DEFAULT_EXERCISE_IDS,
  EXERCISE_TYPES,
  ExerciseTypeId,
  VocabWord,
  WordPack,
} from '../../types/vocab';
import { StockPhotoPickerModal } from '../StockPhotoPickerModal';
import { WordIllustration } from '../WordIllustration';
import { WordEditorModal } from './WordEditorModal';

interface ParentPacksScreenProps {
  packs: WordPack[];
  words: VocabWord[];
  profiles: ChildProfile[];
  onOpenPackDetail: (packId: string) => void;
  onSavePack: (data: {
    id?: string;
    title: string;
    description: string;
    colorIndex: number;
    enabledExercises: ExerciseTypeId[];
    assignedChildIds: string[];
  }) => void;
  onDeletePack: (packId: string) => void;
  onSaveWord: (data: {
    id?: string;
    packId: string;
    englishWord: string;
    czechTranslation: string;
    imageDataUrl: string;
  }) => void;
  onDeleteWord: (wordId: string) => void;
}

export const ParentPacksScreen: React.FC<ParentPacksScreenProps> = ({
  packs,
  words,
  profiles,
  onOpenPackDetail,
  onSavePack,
  onDeletePack,
  onSaveWord,
  onDeleteWord,
}) => {
  const [editingPack, setEditingPack] = useState<WordPack | null>(null);
  const [creatingNew, setCreatingNew] = useState(false);
  const [confirmDeletePack, setConfirmDeletePack] = useState<WordPack | null>(null);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [colorIndex, setColorIndex] = useState(0);
  const [enabledExercises, setEnabledExercises] = useState<ExerciseTypeId[]>([
    ...DEFAULT_EXERCISE_IDS,
  ]);
  const [assignedChildIds, setAssignedChildIds] = useState<string[]>([]);
  const [wordSearch, setWordSearch] = useState('');

  const [wordModalState, setWordModalState] = useState<{
    open: boolean;
    packId: string;
    word?: VocabWord | null;
  }>({ open: false, packId: '' });
  const [stockTargetWord, setStockTargetWord] = useState<VocabWord | null>(null);
  const [busyWordId, setBusyWordId] = useState<string | null>(null);
  const quickFileRef = useRef<HTMLInputElement | null>(null);
  const uploadTargetWordRef = useRef<VocabWord | null>(null);

  const openCreateModal = () => {
    setEditingPack(null);
    setCreatingNew(true);
    setTitle('');
    setDescription('');
    setColorIndex(packs.length % PACK_PALETTES.length);
    setEnabledExercises([...DEFAULT_EXERCISE_IDS]);
    setAssignedChildIds([]);
    setWordSearch('');
  };

  const openEditModal = (pack: WordPack) => {
    setCreatingNew(false);
    setEditingPack(pack);
    setTitle(pack.title);
    setDescription(pack.description);
    setColorIndex(pack.colorIndex);
    setEnabledExercises([...pack.enabledExercises]);
    setAssignedChildIds([...pack.assignedChildIds]);
    setWordSearch('');
  };

  const closeModal = () => {
    setEditingPack(null);
    setCreatingNew(false);
  };

  const wordsCountByPack = useMemo(() => {
    const map = new Map<string, number>();
    for (const w of words) {
      map.set(w.packId, (map.get(w.packId) ?? 0) + 1);
    }
    return map;
  }, [words]);

  const modalPackWords = useMemo(() => {
    if (!editingPack) return [];
    const q = wordSearch.trim().toLowerCase();
    return words
      .filter((w) => w.packId === editingPack.id)
      .filter(
        (w) =>
          !q ||
          w.englishWord.toLowerCase().includes(q) ||
          w.czechTranslation.toLowerCase().includes(q)
      );
  }, [editingPack, words, wordSearch]);

  const toggleExercise = (id: ExerciseTypeId) => {
    setEnabledExercises((prev) => {
      if (prev.includes(id)) {
        return prev.length > 1 ? prev.filter((x) => x !== id) : prev;
      }
      return [...prev, id];
    });
  };

  const toggleChild = (childId: string) => {
    setAssignedChildIds((prev) =>
      prev.includes(childId) ? prev.filter((x) => x !== childId) : [...prev, childId]
    );
  };

  const handleQuickUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    const target = uploadTargetWordRef.current;
    e.target.value = '';
    if (!file || !target) return;
    setBusyWordId(target.id);
    try {
      const dataUrl = await encodeImageFileToDataUrl(file);
      onSaveWord({
        id: target.id,
        packId: target.packId,
        englishWord: target.englishWord,
        czechTranslation: target.czechTranslation,
        imageDataUrl: dataUrl,
      });
    } finally {
      setBusyWordId(null);
    }
  };

  return (
    <div className="mx-auto max-w-4xl space-y-4 px-3 py-4 pb-24 sm:px-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="font-display text-2xl font-bold text-brand-ink">Balíčky slovíček</h2>
          <p className="text-xs font-semibold text-brand-muted">
            {packs.length} balíčků • {words.length} slovíček celkem
          </p>
        </div>
        <button
          type="button"
          onClick={openCreateModal}
          className="inline-flex items-center gap-1.5 rounded-2xl bg-brand-indigo px-4 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-brand-indigo/90 active:scale-95"
        >
          <Plus className="h-5 w-5" />
          <span>Nový balíček</span>
        </button>
      </div>

      <div className="space-y-3">
        {packs.map((pack) => {
          const palette = getPackPalette(pack.colorIndex);
          const count = wordsCountByPack.get(pack.id) ?? 0;
          return (
            <div
              key={pack.id}
              className="flex flex-col gap-3 rounded-3xl border border-brand-outline/60 bg-white p-4 shadow-sm sm:flex-row sm:items-center"
            >
              <button
                type="button"
                onClick={() => onOpenPackDetail(pack.id)}
                className="flex min-w-0 flex-1 items-center gap-3 text-left"
              >
                <span className={`h-12 w-2.5 shrink-0 rounded-full ${palette.accentBg}`} />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="truncate font-display text-lg font-bold text-brand-ink">
                      {pack.title}
                    </h3>
                    <span
                      className={`rounded-full px-2.5 py-0.5 text-xs font-extrabold ${palette.containerBg} ${palette.onContainerText}`}
                    >
                      {count} slov
                    </span>
                  </div>
                  {pack.description && (
                    <p className="truncate text-xs text-brand-muted">{pack.description}</p>
                  )}
                  <p className="mt-1 text-[11px] font-bold text-brand-muted">
                    Cvičení: {pack.enabledExercises.length} •{' '}
                    {pack.assignedChildIds.length === 0
                      ? 'Pro všechny děti'
                      : `Pro ${pack.assignedChildIds.length} dětí`}
                  </p>
                </div>
                <ChevronRight className="h-5 w-5 shrink-0 text-brand-muted" />
              </button>

              <div className="flex items-center justify-end gap-2 border-t border-slate-100 pt-2 sm:border-t-0 sm:pt-0">
                <button
                  type="button"
                  onClick={() => openEditModal(pack)}
                  className="inline-flex h-10 items-center gap-1.5 rounded-xl bg-brand-indigoLight px-3 text-xs font-bold text-brand-indigoDark hover:bg-brand-indigoLight/80"
                >
                  <Edit3 className="h-4 w-4" />
                  <span>Nastavení & slova</span>
                </button>
                <button
                  type="button"
                  onClick={() => setConfirmDeletePack(pack)}
                  aria-label={`Smazat ${pack.title}`}
                  className="flex h-10 w-10 items-center justify-center rounded-xl bg-red-50 text-red-600 hover:bg-red-100"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Create / Edit Pack & Words Modal */}
      {(creatingNew || editingPack) && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-slate-900/50 p-3 backdrop-blur-sm">
          <div className="flex max-h-[90vh] w-full max-w-xl flex-col rounded-3xl bg-white p-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h2 className="font-display text-xl font-bold text-brand-ink">
                {editingPack ? `Upravit: ${editingPack.title}` : 'Nový balíček slovíček'}
              </h2>
              <button
                type="button"
                onClick={closeModal}
                aria-label="Zavřít"
                className="flex h-10 w-10 items-center justify-center rounded-full text-brand-muted hover:bg-slate-100"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mt-4 flex-1 space-y-4 overflow-y-auto pr-1">
              <div>
                <label className="block text-xs font-bold text-brand-muted">Název balíčku</label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="např. Unit 9 - Doprava"
                  className="mt-1 w-full rounded-2xl border border-brand-outline px-3.5 py-2.5 text-sm font-bold outline-none focus:border-brand-indigo"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-brand-muted">Popis</label>
                <input
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Krátký popis pro děti a rodiče"
                  className="mt-1 w-full rounded-2xl border border-brand-outline px-3.5 py-2.5 text-sm font-semibold outline-none focus:border-brand-indigo"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-brand-muted">Barva balíčku</label>
                <div className="mt-2 flex flex-wrap gap-2.5">
                  {PACK_PALETTES.map((pal, idx) => (
                    <button
                      key={pal.accentHex}
                      type="button"
                      onClick={() => setColorIndex(idx)}
                      aria-label={`Barva ${idx + 1}`}
                      className={`h-9 w-9 rounded-full ${pal.accentBg} transition ${
                        colorIndex === idx ? 'ring-4 ring-brand-ink/25 scale-110' : 'opacity-80'
                      }`}
                    />
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-brand-muted">
                  Povolená cvičení v balíčku
                </label>
                <div className="mt-2 flex flex-wrap gap-2">
                  {EXERCISE_TYPES.map((ex) => {
                    const active = enabledExercises.includes(ex.id);
                    return (
                      <button
                        key={ex.id}
                        type="button"
                        onClick={() => toggleExercise(ex.id)}
                        className={`rounded-full px-3 py-1.5 text-xs font-bold transition ${
                          active
                            ? 'bg-brand-indigo text-white'
                            : 'bg-slate-100 text-brand-muted'
                        }`}
                      >
                        {ex.titleCz}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-brand-muted">
                  Přiřazené děti (nevybráno = pro všechny)
                </label>
                <div className="mt-2 flex flex-wrap gap-2">
                  {profiles.map((child) => {
                    const active = assignedChildIds.includes(child.id);
                    return (
                      <button
                        key={child.id}
                        type="button"
                        onClick={() => toggleChild(child.id)}
                        className={`rounded-full px-3 py-1.5 text-xs font-bold transition ${
                          active
                            ? 'bg-brand-mint text-white'
                            : 'bg-slate-100 text-brand-muted'
                        }`}
                      >
                        {child.avatarEmoji} {child.name}
                      </button>
                    );
                  })}
                </div>
              </div>

              {editingPack && (
                <div className="border-t border-slate-200 pt-4">
                  <div className="flex items-center justify-between gap-2">
                    <h3 className="font-display text-base font-bold text-brand-ink">
                      Slovíčka v balíčku ({modalPackWords.length})
                    </h3>
                    <button
                      type="button"
                      onClick={() =>
                        setWordModalState({ open: true, packId: editingPack.id, word: null })
                      }
                      className="inline-flex items-center gap-1 rounded-full bg-brand-mintLight px-3 py-1.5 text-xs font-extrabold text-brand-mintDark"
                    >
                      <Plus className="h-4 w-4" />
                      <span>Přidat slovo</span>
                    </button>
                  </div>

                  <div className="relative mt-2.5">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-brand-muted" />
                    <input
                      type="text"
                      value={wordSearch}
                      onChange={(e) => setWordSearch(e.target.value)}
                      placeholder="Hledat slovíčko..."
                      className="w-full rounded-xl border border-brand-outline py-2 pl-9 pr-3 text-xs font-semibold outline-none focus:border-brand-indigo"
                    />
                  </div>

                  <div className="mt-3 space-y-2">
                    {modalPackWords.map((w) => (
                      <div
                        key={w.id}
                        className="flex items-center gap-2.5 rounded-2xl border border-slate-200 bg-slate-50/60 p-2"
                      >
                        <WordIllustration
                          englishWord={w.englishWord}
                          czechTranslation={w.czechTranslation}
                          imageDataUrl={w.imageDataUrl}
                          isLoading={busyWordId === w.id}
                          className="h-12 w-12 shrink-0 rounded-xl"
                        />
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-bold text-brand-ink">
                            {w.englishWord}
                          </p>
                          <p className="truncate text-xs text-brand-muted">{w.czechTranslation}</p>
                        </div>
                        <button
                          type="button"
                          onClick={() => setStockTargetWord(w)}
                          title="Vybrat obrázek z databanky"
                          className="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-brand-indigo shadow-sm"
                        >
                          <Search className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            uploadTargetWordRef.current = w;
                            quickFileRef.current?.click();
                          }}
                          title="Nahrát vlastní obrázek"
                          className="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-brand-coral shadow-sm"
                        >
                          <ImagePlus className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            setWordModalState({ open: true, packId: editingPack.id, word: w })
                          }
                          title="Upravit slovíčko"
                          className="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-brand-ink shadow-sm"
                        >
                          <Edit3 className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          onClick={() => onDeleteWord(w.id)}
                          title="Smazat slovíčko"
                          className="flex h-9 w-9 items-center justify-center rounded-xl bg-red-50 text-red-600"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="mt-4 flex gap-3 border-t border-slate-100 pt-3">
              <button
                type="button"
                onClick={closeModal}
                className="flex-1 rounded-2xl border border-brand-outline py-3 font-bold text-brand-muted"
              >
                Zrušit
              </button>
              <button
                type="button"
                disabled={!title.trim()}
                onClick={() => {
                  onSavePack({
                    id: editingPack?.id,
                    title: title.trim(),
                    description: description.trim(),
                    colorIndex,
                    enabledExercises,
                    assignedChildIds,
                  });
                  closeModal();
                }}
                className="flex-1 rounded-2xl bg-brand-indigo py-3 font-bold text-white shadow-sm disabled:opacity-50"
              >
                Uložit balíček
              </button>
            </div>
          </div>
        </div>
      )}

      <input
        ref={quickFileRef}
        type="file"
        accept="image/*"
        onChange={handleQuickUpload}
        className="hidden"
      />

      {wordModalState.open && (
        <WordEditorModal
          existingWord={wordModalState.word}
          onSave={(data) => {
            onSaveWord({
              id: wordModalState.word?.id,
              packId: wordModalState.packId,
              ...data,
            });
            setWordModalState({ open: false, packId: '' });
          }}
          onDismiss={() => setWordModalState({ open: false, packId: '' })}
        />
      )}

      {stockTargetWord && (
        <StockPhotoPickerModal
          initialQuery={stockTargetWord.englishWord || stockTargetWord.czechTranslation}
          onSelectPhoto={async (photo) => {
            const target = stockTargetWord;
            setStockTargetWord(null);
            setBusyWordId(target.id);
            try {
              const dataUrl = await downloadStockPhotoAsDataUrl(photo);
              onSaveWord({
                id: target.id,
                packId: target.packId,
                englishWord: target.englishWord,
                czechTranslation: target.czechTranslation,
                imageDataUrl: dataUrl,
              });
            } finally {
              setBusyWordId(null);
            }
          }}
          onDismiss={() => setStockTargetWord(null)}
        />
      )}

      {confirmDeletePack && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
          <div className="w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl">
            <h3 className="font-display text-xl font-bold text-brand-ink">Smazat balíček?</h3>
            <p className="mt-2 text-sm text-brand-muted">
              Opravdu chcete odstranit balíček <strong>{confirmDeletePack.title}</strong> včetně
              všech jeho slovíček?
            </p>
            <div className="mt-5 flex gap-3">
              <button
                type="button"
                onClick={() => setConfirmDeletePack(null)}
                className="flex-1 rounded-2xl border border-brand-outline py-3 font-bold text-brand-muted"
              >
                Zrušit
              </button>
              <button
                type="button"
                onClick={() => {
                  onDeletePack(confirmDeletePack.id);
                  setConfirmDeletePack(null);
                }}
                className="flex-1 rounded-2xl bg-red-600 py-3 font-bold text-white"
              >
                Smazat
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
