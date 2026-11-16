const MAX_IMAGE_DIM = 320;

export async function encodeImageFileToDataUrl(file: File): Promise<string> {
  const objectUrl = URL.createObjectURL(file);
  try {
    return await renderImageUrlToDataUrl(objectUrl, false);
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

export async function renderImageUrlToDataUrl(
  url: string,
  fillCreamBackdrop: boolean = true
): Promise<string> {
  const img = new Image();
  img.crossOrigin = 'anonymous';

  await new Promise<void>((resolve, reject) => {
    img.onload = () => resolve();
    img.onerror = () => reject(new Error('Obrázek se nepodařilo načíst.'));
    img.src = url;
  });

  const srcW = img.naturalWidth || MAX_IMAGE_DIM;
  const srcH = img.naturalHeight || MAX_IMAGE_DIM;
  const scale = Math.min(1, MAX_IMAGE_DIM / Math.max(srcW, srcH));
  const targetW = Math.max(1, Math.round(srcW * scale));
  const targetH = Math.max(1, Math.round(srcH * scale));

  const canvas = document.createElement('canvas');
  canvas.width = targetW;
  canvas.height = targetH;
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('Canvas není dostupný.');

  if (fillCreamBackdrop) {
    ctx.fillStyle = '#FFFBF5';
    ctx.fillRect(0, 0, targetW, targetH);
  }
  ctx.drawImage(img, 0, 0, targetW, targetH);

  return canvas.toDataURL('image/webp', 0.85);
}

export async function svgTextToDataUrl(svgText: string): Promise<string> {
  const blob = new Blob([svgText], { type: 'image/svg+xml;charset=utf-8' });
  const blobUrl = URL.createObjectURL(blob);
  try {
    return await renderImageUrlToDataUrl(blobUrl, true);
  } finally {
    URL.revokeObjectURL(blobUrl);
  }
}
