import { GoogleGenAI, Type } from '@google/genai';

export interface GeminiPronunciationResult {
  passed: boolean;
  score: number;
  feedback: string;
}

export function getGeminiKeyDiagnostic(): string {
  const apiKey = import.meta.env.VITE_GEMINI_API_KEY?.trim() ?? '';
  if (!apiKey) return 'VITE_GEMINI_API_KEY načten: ne';
  return `VITE_GEMINI_API_KEY načten: ano (${apiKey.slice(0, 4)}...)`;
}

export class MissingGeminiKeyError extends Error {
  readonly technicalDetails: string;
  readonly keyDiagnostic: string;

  constructor() {
    super('Zkus to prosím za chvilku znovu');
    this.name = 'MissingGeminiKeyError';
    this.technicalDetails = 'Chybí proměnná prostředí VITE_GEMINI_API_KEY.';
    this.keyDiagnostic = getGeminiKeyDiagnostic();
  }
}

export class GeminiEvaluationError extends Error {
  readonly technicalDetails: string;
  readonly keyDiagnostic: string;

  constructor(technicalDetails: string, keyDiagnostic: string) {
    super('Zkus to prosím za chvilku znovu');
    this.name = 'GeminiEvaluationError';
    this.technicalDetails = technicalDetails;
    this.keyDiagnostic = keyDiagnostic;
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
    reader.readAsDataURL(blob);
  });
}

export async function evaluatePronunciationAudio(
  targetWord: string,
  audioBlob: Blob,
  rawMimeType: string
): Promise<GeminiPronunciationResult> {
  const apiKey = import.meta.env.VITE_GEMINI_API_KEY?.trim();
  if (!apiKey) {
    throw new MissingGeminiKeyError();
  }

  const cleanMimeType = normalizeAudioMimeType(rawMimeType || audioBlob.type);
  const base64Audio = await blobToBase64(audioBlob);

  const prompt = `Poslechni si nahrávku. Uživatel se snaží anglicky vyslovit slovo: ${targetWord}. Pokud je výslovnost srozumitelná (alespoň na 70 %), vrať JSON: {"passed": true, "score": číslo 0-100, "feedback": "krátká pochvala nebo tip česky"}. Pokud ne, vrať passed: false a krátkou radu česky.`;

  try {
    const ai = new GoogleGenAI({ apiKey });
    const response = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
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
    const errObj = err as { status?: unknown; statusCode?: unknown; code?: unknown; message?: unknown };
    const status = errObj?.status ?? errObj?.statusCode ?? errObj?.code;
    const rawMessage =
      err instanceof Error
        ? err.message
        : typeof errObj?.message === 'string'
        ? errObj.message
        : String(err);
    const sanitizedMessage = apiKey ? rawMessage.split(apiKey).join('***') : rawMessage;
    const technicalDetails =
      status !== undefined && status !== null && String(status).trim() !== ''
        ? `Status ${String(status)}: ${sanitizedMessage}`
        : sanitizedMessage || 'Neznámá chyba API';

    throw new GeminiEvaluationError(technicalDetails, getGeminiKeyDiagnostic());
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

export function speakEnglish(text: string): void {
  if (!text.trim() || typeof window === 'undefined' || !('speechSynthesis' in window)) return;
  try {
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'en-US';
    utterance.rate = 0.85;
    utterance.pitch = 1.05;
    const voice = pickVoice('en-US') ?? pickVoice('en-GB');
    if (voice) utterance.voice = voice;
    window.speechSynthesis.speak(utterance);
  } catch {
    // Ignore Web Speech errors on restricted browsers
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
