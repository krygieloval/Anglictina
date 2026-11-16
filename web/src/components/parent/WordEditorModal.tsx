import React, { useRef, useState } from 'react';
import { ImagePlus, RotateCcw, Search, X } from 'lucide-react';
import { encodeImageFileToDataUrl } from '../../services/imageCodec';
import { downloadStockPhotoAsDataUrl } from '../../services/stockPhotos';
import { VocabWord } from '../../types/vocab';
import { StockPhotoPickerModal } from '../StockPhotoPickerModal';
import { WordIllustration } from '../WordIllustration';

interface WordEditorModalProps {
  existingWord?: VocabWord | null;
  onSave: (data: {
    englishWord: string;
    czechTranslation: string;
    imageDataUrl: string;
  }) => void;
  onDismiss: () => void;
}

export const WordEditorModal: React.FC<WordEditorModalProps> = ({
  existingWord,
  onSave,
  onDismiss,
}) => {
  const [englishWord, setEnglishWord] = useState(existingWord?.englishWord ?? '');
  const [czechTranslation, setCzechTranslation] = useState(existingWord?.czechTranslation ?? '');
  const [imageDataUrl, setImageDataUrl] = useState(existingWord?.imageDataUrl ?? '');
  const [isDownloading, setIsDownloading] = useState(false);
  const [showStockPicker, setShowStockPicker] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!englishWord.trim() || !czechTranslation.trim()) {
      setError('Vyplňte prosím anglické slovo i český překlad.');
      return;
    }
    onSave({
      englishWord: englishWord.trim(),
      czechTranslation: czechTranslation.trim(),
      imageDataUrl,
    });
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setError(null);
    setIsDownloading(true);
    try {
      const encoded = await encodeImageFileToDataUrl(file);
      setImageDataUrl(encoded);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Obrázek se nepodařilo nahrát.');
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <>
      <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/50 p-3 backdrop-blur-sm">
        <form
          onSubmit={handleSubmit}
          className="flex max-h-[90vh] w-full max-w-md flex-col overflow-y-auto rounded-3xl bg-white p-5 shadow-2xl"
        >
          <div className="flex items-center justify-between">
            <h2 className="font-display text-xl font-bold text-brand-ink">
              {existingWord ? 'Upravit slovíčko' : 'Nové slovíčko'}
            </h2>
            <button
              type="button"
              onClick={onDismiss}
              aria-label="Zavřít"
              className="flex h-10 w-10 items-center justify-center rounded-full text-brand-muted hover:bg-slate-100"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="mt-4 flex flex-col items-center gap-3">
            <WordIllustration
              englishWord={englishWord || 'word'}
              czechTranslation={czechTranslation}
              imageDataUrl={imageDataUrl}
              isLoading={isDownloading}
              className="h-32 w-32"
            />

            <div className="flex flex-wrap justify-center gap-2">
              <button
                type="button"
                disabled={isDownloading}
                onClick={() => setShowStockPicker(true)}
                className="inline-flex items-center gap-1.5 rounded-full bg-brand-indigoLight px-3.5 py-2 text-xs font-extrabold text-brand-indigoDark hover:bg-brand-indigoLight/80"
              >
                <Search className="h-4 w-4" />
                <span>Vybrat z databanky</span>
              </button>

              <button
                type="button"
                disabled={isDownloading}
                onClick={() => fileInputRef.current?.click()}
                className="inline-flex items-center gap-1.5 rounded-full bg-brand-coralLight px-3.5 py-2 text-xs font-extrabold text-brand-coralDark hover:bg-brand-coralLight/80"
              >
                <ImagePlus className="h-4 w-4" />
                <span>Vlastní obrázek</span>
              </button>

              {imageDataUrl && (
                <button
                  type="button"
                  onClick={() => setImageDataUrl('')}
                  className="inline-flex items-center gap-1 rounded-full border border-brand-outline px-3 py-2 text-xs font-bold text-brand-muted hover:bg-slate-50"
                >
                  <RotateCcw className="h-3.5 w-3.5" />
                  <span>Výchozí</span>
                </button>
              )}
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              className="hidden"
            />
          </div>

          <div className="mt-4 space-y-3">
            <div>
              <label className="block text-xs font-bold text-brand-muted">Anglické slovíčko</label>
              <input
                type="text"
                value={englishWord}
                onChange={(e) => {
                  setEnglishWord(e.target.value);
                  setError(null);
                }}
                placeholder="např. Butterfly"
                className="mt-1 w-full rounded-2xl border border-brand-outline px-3.5 py-2.5 text-sm font-bold outline-none focus:border-brand-indigo"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-brand-muted">Český překlad</label>
              <input
                type="text"
                value={czechTranslation}
                onChange={(e) => {
                  setCzechTranslation(e.target.value);
                  setError(null);
                }}
                placeholder="např. Motýl"
                className="mt-1 w-full rounded-2xl border border-brand-outline px-3.5 py-2.5 text-sm font-bold outline-none focus:border-brand-indigo"
              />
            </div>
          </div>

          {error && <p className="mt-3 text-center text-xs font-bold text-red-600">{error}</p>}

          <div className="mt-5 flex gap-3">
            <button
              type="button"
              onClick={onDismiss}
              className="flex-1 rounded-2xl border border-brand-outline py-3 font-bold text-brand-muted"
            >
              Zrušit
            </button>
            <button
              type="submit"
              disabled={isDownloading}
              className="flex-1 rounded-2xl bg-brand-indigo py-3 font-bold text-white shadow-sm disabled:opacity-60"
            >
              Uložit
            </button>
          </div>
        </form>
      </div>

      {showStockPicker && (
        <StockPhotoPickerModal
          initialQuery={englishWord || czechTranslation}
          onSelectPhoto={async (photo) => {
            setShowStockPicker(false);
            setIsDownloading(true);
            setError(null);
            try {
              const dataUrl = await downloadStockPhotoAsDataUrl(photo);
              setImageDataUrl(dataUrl);
            } catch (err) {
              setError(
                err instanceof Error ? err.message : 'Obrázek z databanky se nepodařilo uložit.'
              );
            } finally {
              setIsDownloading(false);
            }
          }}
          onDismiss={() => setShowStockPicker(false)}
        />
      )}
    </>
  );
};
