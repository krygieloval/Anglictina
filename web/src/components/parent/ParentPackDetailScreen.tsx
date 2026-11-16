import React, { useRef, useState } from 'react';
import {
  ArrowLeft,
  Edit3,
  ImagePlus,
  Plus,
  Search,
  Sparkles,
  Trash2,
} from 'lucide-react';
import { getPackPalette } from '../../data/palettes';
import { encodeImageFileToDataUrl } from '../../services/imageCodec';
import { downloadStockPhotoAsDataUrl, searchStockPhotos } from '../../services/stockPhotos';
import { speakEnglish } from '../../services/speech';
import {
  ChildProfile,
  EXERCISE_TYPES,
  ExerciseTypeId,
  VocabWord,
  WordPack,
} from '../../types/vocab';
import { SpeakButton } from '../SpeakButton';
import { StockPhotoPickerModal } from '../StockPhotoPickerModal';
import { WordIllustration } from '../WordIllustration';
import { WordEditorModal } from './WordEditorModal';

interface ParentPackDetailScreenProps {
  pack: WordPack;
  words: VocabWord[];
  profiles: ChildProfile[];
  onBack: () => void;
  onUpdatePack: (pack: WordPack) => void;
  onSaveWord: (data: {
    id?: string;
    packId: string;
    englishWord: string;
    czechTranslation: string;
    imageDataUrl: string;
  }) => void;
  onDeleteWord: (wordId: string) => void;
  onNotify: (message: string) => void;
}

export const ParentPackDetailScreen: React.FC<ParentPackDetailScreenProps> = ({
  pack,
  words,
  profiles,
  onBack,
  onUpdatePack,
  onSaveWord,
  onDeleteWord,
  onNotify,
}) => {
  const palette = getPackPalette(pack.colorIndex);
  const [editingWord, setEditingWord] = useState<VocabWord | null>(null);
  const [creatingWord, setCreatingWord] = useState(false);
  const [stockTargetWord, setStockTargetWord] = useState<VocabWord | null>(null);
  const [busyWordId, setBusyWordId] = useState<string | null>(null);
  const [bulkBusy, setBulkBusy] = useState(false);

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const uploadTargetWordRef = useRef<VocabWord | null>(null);

  const toggleExercise = (exId: ExerciseTypeId) => {
    const exists = pack.enabledExercises.includes(exId);
    if (exists && pack.enabledExercises.length <= 1) return;
    const next = exists
      ? pack.enabledExercises.filter((x) => x !== exId)
      : [...pack.enabledExercises, exId];
    onUpdatePack({ ...pack, enabledExercises: next, updatedAt: Date.now() });
  };

  const toggleChild = (childId: string) => {
    const exists = pack.assignedChildIds.includes(childId);
    const next = exists
      ? pack.assignedChildIds.filter((x) => x !== childId)
      : [...pack.assignedChildIds, childId];
    onUpdatePack({ ...pack, assignedChildIds: next, updatedAt: Date.now() });
  };

  const handleUploadFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    const target = uploadTargetWordRef.current;
    e.target.value = '';
    if (!file || !target) return;
    setBusyWordId(target.id);
    try {
      const dataUrl = await encodeImageFileToDataUrl(file);
      onSaveWord({
        id: target.id,
        packId: pack.id,
        englishWord: target.englishWord,
        czechTranslation: target.czechTranslation,
        imageDataUrl: dataUrl,
      });
      onNotify(`Vlastní obrázek uložen pro „${target.englishWord}“.`);
    } catch (err) {
      onNotify(err instanceof Error ? err.message : 'Obrázek se nepodařilo nahrát.');
    } finally {
      setBusyWordId(null);
    }
  };

  const handleBulkFetch = async () => {
    const missing = words.filter((w) => !w.imageDataUrl);
    if (missing.length === 0) {
      onNotify('Všechna slovíčka v tomto balíčku už mají vlastní obrázek.');
      return;
    }
    setBulkBusy(true);
    let updated = 0;
    try {
      for (const word of missing) {
        setBusyWordId(word.id);
        try {
          const results = await searchStockPhotos(word.englishWord);
          const first = results[0];
          if (first) {
            const dataUrl = await downloadStockPhotoAsDataUrl(first);
            onSaveWord({
              id: word.id,
              packId: pack.id,
              englishWord: word.englishWord,
              czechTranslation: word.czechTranslation,
              imageDataUrl: dataUrl,
            });
            updated++;
          }
        } catch {
          // Continue with remaining words
        }
      }
      onNotify(`Doplněno obrázků z databanky: ${updated} z ${missing.length}.`);
    } finally {
      setBusyWordId(null);
      setBulkBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-4xl space-y-4 px-3 py-4 pb-24 sm:px-4">
      <div className={`flex items-center gap-3 rounded-3xl ${palette.containerBg} p-4`}>
        <button
          type="button"
          onClick={onBack}
          aria-label="Zpět na balíčky"
          className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-white/75 ${palette.onContainerText}`}
        >
          <ArrowLeft className="h-6 w-6" />
        </button>
        <div className="min-w-0 flex-1">
          <h2 className={`truncate font-display text-2xl font-bold ${palette.onContainerText}`}>
            {pack.title}
          </h2>
          {pack.description && (
            <p className={`truncate text-sm ${palette.onContainerText} opacity-80`}>
              {pack.description}
            </p>
          )}
        </div>
        <button
          type="button"
          onClick={() => setCreatingWord(true)}
          className="inline-flex h-11 items-center gap-1.5 rounded-2xl bg-brand-indigo px-3.5 text-sm font-bold text-white shadow-sm"
        >
          <Plus className="h-5 w-5" />
          <span className="hidden sm:inline">Přidat slovo</span>
        </button>
      </div>

      <div className="space-y-3 rounded-3xl bg-white p-4 shadow-sm">
        <div>
          <p className="text-xs font-bold text-brand-muted">Povolená cvičení</p>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {EXERCISE_TYPES.map((ex) => {
              const active = pack.enabledExercises.includes(ex.id);
              return (
                <button
                  key={ex.id}
                  type="button"
                  onClick={() => toggleExercise(ex.id)}
                  className={`rounded-full px-3 py-1 text-xs font-bold transition ${
                    active ? 'bg-brand-indigo text-white' : 'bg-slate-100 text-brand-muted'
                  }`}
                >
                  {ex.titleCz}
                </button>
              );
            })}
          </div>
        </div>

        <div>
          <p className="text-xs font-bold text-brand-muted">
            Přiřazené děti (nevybráno = pro všechny)
          </p>
          <div className="mt-2 flex flex-wrap gap-1.5">
            {profiles.map((child) => {
              const active = pack.assignedChildIds.includes(child.id);
              return (
                <button
                  key={child.id}
                  type="button"
                  onClick={() => toggleChild(child.id)}
                  className={`rounded-full px-3 py-1 text-xs font-bold transition ${
                    active ? 'bg-brand-mint text-white' : 'bg-slate-100 text-brand-muted'
                  }`}
                >
                  {child.avatarEmoji} {child.name}
                </button>
              );
            })}
          </div>
        </div>

        <button
          type="button"
          disabled={bulkBusy}
          onClick={handleBulkFetch}
          className="flex w-full items-center justify-center gap-2 rounded-2xl bg-brand-indigoLight py-2.5 text-sm font-bold text-brand-indigoDark transition hover:bg-brand-indigoLight/80 disabled:opacity-60"
        >
          <Sparkles className="h-4 w-4 text-brand-indigo" />
          <span>
            {bulkBusy
              ? 'Stahuji obrázky z databanky...'
              : 'Doplnit fotky z databanky pro balíček'}
          </span>
        </button>
      </div>

      <div className="space-y-2.5">
        {words.length === 0 ? (
          <div className="rounded-3xl bg-white p-8 text-center text-sm font-semibold text-brand-muted shadow-sm">
            Balíček je prázdný. Přidejte první slovíčko tlačítkem výše.
          </div>
        ) : (
          words.map((word) => (
            <div
              key={word.id}
              className="flex flex-wrap items-center gap-3 rounded-3xl bg-white p-3.5 shadow-sm"
            >
              <WordIllustration
                englishWord={word.englishWord}
                czechTranslation={word.czechTranslation}
                imageDataUrl={word.imageDataUrl}
                isLoading={busyWordId === word.id}
                className="h-16 w-16 shrink-0"
              />

              <div className="min-w-0 flex-1">
                <p className="truncate font-display text-lg font-bold text-brand-ink">
                  {word.englishWord}
                </p>
                <p className="truncate text-sm text-brand-muted">{word.czechTranslation}</p>
              </div>

              <div className="flex items-center gap-1.5">
                <SpeakButton onClick={() => speakEnglish(word.englishWord)} />
                <button
                  type="button"
                  onClick={() => setStockTargetWord(word)}
                  aria-label="Obrázek z databanky"
                  title="Obrázek z databanky"
                  className="flex h-11 w-11 items-center justify-center rounded-full bg-brand-indigoLight text-brand-indigo"
                >
                  <Search className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={() => {
                    uploadTargetWordRef.current = word;
                    fileInputRef.current?.click();
                  }}
                  aria-label="Nahrát vlastní obrázek"
                  title="Nahrát vlastní obrázek"
                  className="flex h-11 w-11 items-center justify-center rounded-full bg-brand-coralLight text-brand-coralDark"
                >
                  <ImagePlus className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={() => setEditingWord(word)}
                  aria-label="Upravit slovíčko"
                  className="flex h-11 w-11 items-center justify-center rounded-full bg-slate-100 text-brand-ink"
                >
                  <Edit3 className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={() => onDeleteWord(word.id)}
                  aria-label="Smazat slovíčko"
                  className="flex h-11 w-11 items-center justify-center rounded-full bg-red-50 text-red-600"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={handleUploadFile}
        className="hidden"
      />

      {(creatingWord || editingWord) && (
        <WordEditorModal
          existingWord={editingWord}
          onSave={(data) => {
            onSaveWord({
              id: editingWord?.id,
              packId: pack.id,
              ...data,
            });
            setCreatingWord(false);
            setEditingWord(null);
          }}
          onDismiss={() => {
            setCreatingWord(false);
            setEditingWord(null);
          }}
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
                packId: pack.id,
                englishWord: target.englishWord,
                czechTranslation: target.czechTranslation,
                imageDataUrl: dataUrl,
              });
              onNotify(`Obrázek z databanky uložen pro „${target.englishWord}“.`);
            } catch (err) {
              onNotify(err instanceof Error ? err.message : 'Obrázek se nepodařilo stáhnout.');
            } finally {
              setBusyWordId(null);
            }
          }}
          onDismiss={() => setStockTargetWord(null)}
        />
      )}
    </div>
  );
};
