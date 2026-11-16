import React, { useEffect, useState } from 'react';
import { Search, X } from 'lucide-react';
import { searchStockPhotos } from '../services/stockPhotos';
import { StockPhotoResult } from '../types/vocab';

interface StockPhotoPickerModalProps {
  initialQuery: string;
  onSelectPhoto: (photo: StockPhotoResult) => void;
  onDismiss: () => void;
}

export const StockPhotoPickerModal: React.FC<StockPhotoPickerModalProps> = ({
  initialQuery,
  onSelectPhoto,
  onDismiss,
}) => {
  const [query, setQuery] = useState(initialQuery);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [results, setResults] = useState<StockPhotoResult[]>([]);

  const runSearch = async (q: string) => {
    const clean = q.trim();
    if (!clean) {
      setError('Zadejte hledané slovo.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const items = await searchStockPhotos(clean);
      setResults(items);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Nepodařilo se načíst databanku obrázků.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (initialQuery.trim()) {
      void runSearch(initialQuery);
    }
  }, [initialQuery]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/55 p-3 backdrop-blur-sm">
      <div className="flex max-h-[88vh] w-full max-w-lg flex-col rounded-3xl bg-white p-5 shadow-2xl">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-xl font-bold text-brand-ink">
            Vybrat obrázek z databanky
          </h2>
          <button
            type="button"
            onClick={onDismiss}
            className="flex h-10 w-10 items-center justify-center rounded-full text-brand-muted hover:bg-slate-100"
            aria-label="Zavřít"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            void runSearch(query);
          }}
          className="mt-3 flex gap-2"
        >
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-brand-muted" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Hledat anglicky (např. cat, trampoline)..."
              className="w-full rounded-2xl border border-brand-outline py-2.5 pl-9 pr-3 text-sm font-semibold outline-none focus:border-brand-indigo"
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="rounded-2xl bg-brand-indigo px-4 py-2.5 text-sm font-bold text-white disabled:opacity-60"
          >
            Hledat
          </button>
        </form>

        <div className="mt-4 flex-1 overflow-y-auto">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-12 text-brand-indigo">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-indigo border-t-transparent" />
              <p className="mt-2 text-sm font-bold">Hledám v Iconify a fotobance...</p>
            </div>
          ) : error ? (
            <div className="rounded-2xl bg-red-50 p-4 text-center text-sm font-semibold text-red-700">
              {error}
            </div>
          ) : results.length === 0 ? (
            <p className="py-10 text-center text-sm text-brand-muted">
              Žádné výsledky. Zkuste zadat jiné anglické slovo.
            </p>
          ) : (
            <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3">
              {results.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => onSelectPhoto(item)}
                  className="flex flex-col items-center rounded-2xl border border-brand-outline/80 bg-[#FBF8F1] p-2.5 text-left transition hover:border-brand-indigo hover:shadow-md active:scale-95"
                >
                  <img
                    src={item.thumbUrl}
                    alt={item.title}
                    loading="lazy"
                    className="h-20 w-20 object-contain"
                  />
                  <span className="mt-1.5 line-clamp-1 w-full text-center text-xs font-bold text-brand-ink">
                    {item.title}
                  </span>
                  <span className="line-clamp-1 w-full text-center text-[10px] text-brand-muted">
                    {item.sourceName}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
