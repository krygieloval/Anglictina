import { GoogleGenAI, Type } from '@google/genai';

export interface GeminiPronunciationResult {
  passed: boolean;
  score: number;
  feedback: string;
}

export class MissingGeminiKeyError extends Error {
  constructor() {
    super('Chybí proměnná prostředí VITE_GEMINI_API_KEY.');
    this.name = 'MissingGeminiKeyError';
  }
}

export function isAudioRecordingSupported(): boolean {
  return (
    typeof navigator !== 'undefined' &&
    Boolean(navigator.mediaDevices?.getUserMedia) &&
    typeof MediaRecorder !== 'undefined'
  );
}

const MIME_CANDIDATES = [
  'audio/mp4;codecs=mp4a.40.2',
  'audio/mp4',
  'audio/webm;codecs=opus',
  'audio/webm',
  'audio/aac',
  'audio/ogg;codecs=opus',
];

export function pickCompatibleAudioMimeType(): string | undefined {
  if (typeof MediaRecorder === 'undefined' || typeof MediaRecorder.isTypeSupported !== 'function') {
    return undefined;
  }
  return MIME_CANDIDATES.find((mime) => {
    try {
      return MediaRecorder.isTypeSupported(mime);
    } catch {
      return false;
    }
  });
}

export function normalizeAudioMimeType(rawMime: string): string {
  const base = (rawMime || '').split(';')[0].trim().toLowerCase();
  if (!base) return 'audio/mp4';
  if (base === 'audio/x-m4a' || base === 'video/mp4') return 'audio/mp4';
  if (base === 'video/webm') return 'audio/webm';
  return base;
}

export function blobToBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const result = typeof reader.result === 'string' ? reader.result : '';
      const commaIdx = result.indexOf(',');
      if (commaIdx === -1) {
        reject(new Error('Nepodařilo se převést zvukovou nahrávku.'));
        return;
      }
      resolve(result.slice(commaIdx + 1));
    };
    reader.onerror = () => reject(new Error('Nepodařilo se přečíst zvukovou nahrávku.'));
    reader.onabort = () => reject(new Error('Čtení zvukové nahrávky bylo přerušeno.'));
    reader.readAsDataURL(blob);
  });
}

export async function evaluatePronunciationAudio(
  targetWord: string,
  audioBlob: Blob,
  rawMimeType: string,
  onStep?: (stepLabel: string) => void
): Promise<GeminiPronunciationResult> {
  if (!audioBlob || audioBlob.size <= 0) {
    throw new Error('Nahrávka byla prázdná, zkus to znovu');
  }

  const apiKey = import.meta.env.VITE_GEMINI_API_KEY?.trim();
  if (!apiKey) {
    throw new MissingGeminiKeyError();
  }

  onStep?.('1. Zpracovávám audio...');
  const cleanMimeType = normalizeAudioMimeType(rawMimeType || audioBlob.type);
  const base64Audio = await blobToBase64(audioBlob);

  const prompt = `Poslechni si nahrávku. Uživatel se snaží anglicky vyslovit slovo: ${targetWord}. Pokud je výslovnost srozumitelná (alespoň na 70 %), vrať JSON: {"passed": true, "score": číslo 0-100, "feedback": "krátká pochvala nebo tip česky"}. Pokud ne, vrať passed: false a krátkou radu česky.`;

  onStep?.('2. Odesílám do Gemini...');
  const controller = new AbortController();
  let timeoutId: number | undefined;

  try {
    const ai = new GoogleGenAI({ apiKey });

    const timeoutPromise = new Promise<never>((_, reject) => {
      timeoutId = window.setTimeout(() => {
        controller.abort();
        reject(new Error('Vypršel časový limit 12 sekund při čekání na odpověď Gemini.'));
      }, 12000);
    });

    const generatePromise = ai.models.generateContent({
      model: 'gemini-3.5-flash-lite',
      contents: [
        {
          role: 'user',
          parts: [
            { text: prompt },
            {
              inlineData: {
                mimeType: cleanMimeType,
                data: base64Audio,
              },
            },
          ],
        },
      ],
      config: {
        abortSignal: controller.signal,
        responseMimeType: 'application/json',
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            passed: { type: Type.BOOLEAN },
            score: { type: Type.NUMBER },
            feedback: { type: Type.STRING },
          },
          required: ['passed', 'score', 'feedback'],
        },
      },
    });

    const response = await Promise.race([generatePromise, timeoutPromise]);

    onStep?.('3. Hotovo');

    const rawText = (response.text ?? '').trim();
    const jsonText = rawText
      .replace(/^```(?:json)?\s*/i, '')
      .replace(/\s*```$/i, '')
      .trim();

    const parsed = JSON.parse(jsonText) as Partial<GeminiPronunciationResult>;
    const score = Math.max(
      0,
      Math.min(100, Math.round(typeof parsed.score === 'number' ? parsed.score : 0))
    );
    const passed = Boolean(parsed.passed);
    const feedback =
      typeof parsed.feedback === 'string' && parsed.feedback.trim()
        ? parsed.feedback.trim()
        : passed
        ? 'Výborná výslovnost!'
        : 'Zkus slovíčko vyslovit ještě jednou zřetelněji.';

    return { passed, score, feedback };
  } catch (err) {
    const rawMessage = err instanceof Error ? err.message : String(err);
    const sanitized = apiKey ? rawMessage.split(apiKey).join('***') : rawMessage;
    throw new Error(sanitized || 'Neznámá chyba při volání Gemini API');
  } finally {
    if (timeoutId !== undefined) {
      window.clearTimeout(timeoutId);
    }
  }
}

function pickVoice(langPrefix: string): SpeechSynthesisVoice | null {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) return null;
  const voices = window.speechSynthesis.getVoices();
  if (!voices.length) return null;
  const lower = langPrefix.toLowerCase();
  return (
    voices.find((v) => v.lang.toLowerCase() === lower) ??
    voices.find((v) => v.lang.toLowerCase().startsWith(lower.split('-')[0])) ??
    null
  );
}

export function speakEnglish(text: string, onComplete?: () => void): void {
  const trimmed = text.trim();
  let completed = false;
  let fallbackTimer: number | undefined;

  const finish = () => {
    if (completed) return;
    completed = true;
    if (fallbackTimer !== undefined && typeof window !== 'undefined') {
      window.clearTimeout(fallbackTimer);
    }
    onComplete?.();
  };

  if (!trimmed || typeof window === 'undefined' || !('speechSynthesis' in window)) {
    finish();
    return;
  }

  try {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(trimmed);
    utterance.lang = 'en-US';
    utterance.rate = 0.85;
    utterance.pitch = 1.05;
    const voice = pickVoice('en-US') ?? pickVoice('en-GB');
    if (voice) utterance.voice = voice;

    if (onComplete) {
      const estimatedMaxMs = Math.max(2000, Math.min(5000, 1000 + trimmed.length * 140));
      fallbackTimer = window.setTimeout(finish, estimatedMaxMs);
      utterance.onend = finish;
      utterance.onerror = finish;
    }

    window.speechSynthesis.speak(utterance);
  } catch {
    finish();
  }
}

export function speakCzech(text: string): void {
  if (!text.trim() || typeof window === 'undefined' || !('speechSynthesis' in window)) return;
  try {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'cs-CZ';
    utterance.rate = 0.95;
    utterance.pitch = 1.03;
    const voice = pickVoice('cs-CZ');
    if (voice) utterance.voice = voice;
    window.speechSynthesis.speak(utterance);
  } catch {
    // Ignore Web Speech errors on restricted browsers
  }
}
