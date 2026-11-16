import { StockPhotoResult } from '../types/vocab';
import { renderImageUrlToDataUrl, svgTextToDataUrl } from './imageCodec';

const ICONIFY_COLORED_PREFIXES =
  'fluent-emoji-flat,noto,twemoji,icon-park,streamline-emojis,flat-color-icons';

const EXCLUDED_KEYWORDS = [
  'commons-logo',
  'ambox',
  'flag_of_',
  'question_book',
  'disambig',
  'symbol_',
  'padlock',
  'wikimedia-logo',
  'crystal_clear',
];

function isUnwantedUrl(url: string): boolean {
  const lower = url.toLowerCase();
  if (lower.endsWith('.svg') || lower.includes('.svg/')) return true;
  return EXCLUDED_KEYWORDS.some((kw) => lower.includes(kw));
}

async function fetchIconifyIcons(query: string): Promise<StockPhotoResult[]> {
  const parseIcons = (data: {
    icons?: string[];
    collections?: Record<string, { name?: string; license?: { title?: string } }>;
  }): StockPhotoResult[] => {
    const icons = data.icons ?? [];
    const collections = data.collections ?? {};
    return icons
      .map((fullName): StockPhotoResult | null => {
        const colonIdx = fullName.indexOf(':');
        if (colonIdx <= 0 || colonIdx >= fullName.length - 1) return null;
        const prefix = fullName.slice(0, colonIdx);
        const name = fullName.slice(colonIdx + 1);
        const title = name
          .replace(/[-_]/g, ' ')
          .replace(/^\w/, (c) => c.toUpperCase());
        const col = collections[prefix];
        return {
          id: `iconify_${fullName}`,
          title,
          thumbUrl: `https://api.iconify.design/${prefix}/${name}.svg`,
          sourceName: `Iconify • ${col?.name?.trim() || prefix}`,
          license: col?.license?.title?.trim() || 'Iconify',
        };
      })
      .filter((x): x is StockPhotoResult => x !== null);
  };

  const preferredParams = new URLSearchParams({
    query,
    prefixes: ICONIFY_COLORED_PREFIXES,
    limit: '16',
  });
  const res = await fetch(`https://api.iconify.design/search?${preferredParams.toString()}`);
  if (!res.ok) throw new Error(`Iconify HTTP ${res.status}`);
  const preferred = parseIcons(await res.json());
  if (preferred.length >= 4) return preferred;

  const fallbackParams = new URLSearchParams({ query, limit: '16' });
  const fbRes = await fetch(`https://api.iconify.design/search?${fallbackParams.toString()}`).catch(
    () => null
  );
  if (!fbRes || !fbRes.ok) return preferred;
  const fallback = parseIcons(await fbRes.json());
  const seen = new Set(preferred.map((p) => p.thumbUrl));
  return [...preferred, ...fallback.filter((f) => !seen.has(f.thumbUrl))];
}

async function fetchOpenversePhotos(query: string): Promise<StockPhotoResult[]> {
  const params = new URLSearchParams({
    q: query,
    mature: 'false',
    page_size: '12',
    category: 'photograph,illustration',
  });
  const res = await fetch(`https://api.openverse.org/v1/images/?${params.toString()}`);
  if (!res.ok) throw new Error(`Openverse HTTP ${res.status}`);
  const data = (await res.json()) as {
    results?: Array<{
      id?: string;
      filetype?: string;
      thumbnail?: string;
      url?: string;
      title?: string;
      source?: string;
      provider?: string;
      license?: string;
    }>;
  };
  return (data.results ?? [])
    .map((item, idx): StockPhotoResult | null => {
      if ((item.filetype ?? '').toLowerCase() === 'svg') return null;
      const thumb = (item.thumbnail ?? '').trim();
      const full = (item.url ?? '').trim();
      const primary = thumb || full;
      if (!primary || isUnwantedUrl(primary)) return null;
      const fallback = thumb && full && full !== thumb && !isUnwantedUrl(full) ? full : undefined;
      const rawSrc = (item.source || item.provider || '').trim();
      return {
        id: `ov_${item.id || idx}`,
        title: item.title?.trim() || 'Obrázek',
        thumbUrl: primary,
        sourceName: rawSrc ? `Openverse • ${rawSrc}` : 'Openverse',
        license: (item.license || 'CC').toUpperCase(),
        fallbackUrl: fallback,
      };
    })
    .filter((x): x is StockPhotoResult => x !== null);
}

async function fetchWikipediaPhotos(query: string): Promise<StockPhotoResult[]> {
  const hasCzech = /[áčďéěíňóřšťúůýžÁČĎÉĚÍŇÓŘŠŤÚŮÝŽ]/.test(query);
  const host = hasCzech ? 'cs.wikipedia.org' : 'en.wikipedia.org';
  const params = new URLSearchParams({
    action: 'query',
    generator: 'search',
    gsrsearch: query,
    gsrlimit: '6',
    prop: 'pageimages',
    piprop: 'thumbnail',
    pithumbsize: '480',
    format: 'json',
    origin: '*',
  });
  const res = await fetch(`https://${host}/w/api.php?${params.toString()}`);
  if (!res.ok) throw new Error(`Wikipedia HTTP ${res.status}`);
  const data = (await res.json()) as {
    query?: {
      pages?: Record<
        string,
        { title?: string; index?: number; thumbnail?: { source?: string } }
      >;
    };
  };
  const pages = Object.entries(data.query?.pages ?? {});
  return pages
    .map(([pageId, page]) => ({
      index: page.index ?? 999,
      thumbUrl: page.thumbnail?.source ?? '',
      title: page.title || 'Foto',
      pageId,
    }))
    .filter((p) => p.thumbUrl && !isUnwantedUrl(p.thumbUrl))
    .sort((a, b) => a.index - b.index)
    .map((p) => ({
      id: `wiki_${p.pageId}`,
      title: p.title,
      thumbUrl: p.thumbUrl,
      sourceName: 'Wikipedia',
      license: 'CC / Public',
    }));
}

export async function searchStockPhotos(query: string): Promise<StockPhotoResult[]> {
  const clean = query.trim();
  if (!clean) return [];

  const settled = await Promise.allSettled([
    fetchIconifyIcons(clean),
    fetchOpenversePhotos(clean),
    fetchWikipediaPhotos(clean),
  ]);

  const anyOk = settled.some((r) => r.status === 'fulfilled');
  if (!anyOk) {
    throw new Error(
      'Nepodařilo se spojit s databankou obrázků. Zkontrolujte internetové připojení nebo nahrajte vlastní obrázek.'
    );
  }

  const all = settled.flatMap((r) => (r.status === 'fulfilled' ? r.value : []));
  const deduped = new Map<string, StockPhotoResult>();
  for (const item of all) {
    if (!deduped.has(item.thumbUrl)) {
      deduped.set(item.thumbUrl, item);
    }
  }
  return Array.from(deduped.values()).slice(0, 20);
}

export async function downloadStockPhotoAsDataUrl(photo: StockPhotoResult): Promise<string> {
  const urls = [photo.thumbUrl, photo.fallbackUrl].filter((u): u is string => Boolean(u?.trim()));
  let lastErr: unknown = null;

  for (const url of urls) {
    try {
      if (url.includes('.svg') || url.startsWith('https://api.iconify.design/')) {
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const svgText = await res.text();
        return await svgTextToDataUrl(svgText);
      }
      return await renderImageUrlToDataUrl(url, false);
    } catch (err) {
      lastErr = err;
    }
  }

  throw lastErr instanceof Error
    ? lastErr
    : new Error('Nepodařilo se stáhnout vybraný obrázek.');
}
