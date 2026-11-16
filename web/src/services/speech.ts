export interface PronunciationEvaluation {
  heardText: string;
  matchPercent: number;
  isAccepted: boolean;
}

function normalizeText(input: string): string {
  return input
    .toLowerCase()
    .replace(/[^a-z0-9 ]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function levenshtein(a: string, b: string): number {
  const m = a.length;
  const n = b.length;
  const dp: number[][] = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));
  for (let i = 0; i <= m; i++) dp[i][0] = i;
  for (let j = 0; j <= n; j++) dp[0][j] = j;
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      dp[i][j] = Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost);
    }
  }
  return dp[m][n];
}

function similarityPercent(a: string, b: string): number {
  if (a === b) return 100;
  if (!a || !b) return 0;
  const dist = levenshtein(a, b);
  const maxLen = Math.max(a.length, b.length);
  return Math.max(0, Math.min(100, Math.floor(((maxLen - dist) / maxLen) * 100)));
}

export function evaluatePronunciation(
  targetWord: string,
  candidates: string[]
): PronunciationEvaluation {
  const cleanTarget = normalizeText(targetWord);
  if (candidates.length === 0 || !cleanTarget) {
    return { heardText: '', matchPercent: 0, isAccepted: false };
  }

  let bestCandidate = candidates[0];
  let bestScore = 0;

  for (const candidate of candidates) {
    const normalized = normalizeText(candidate);
    const tokens = normalized.split(' ').filter(Boolean);
    const options = [normalized, ...tokens];
    for (const opt of options) {
      const sim = similarityPercent(cleanTarget, opt);
      if (sim > bestScore) {
        bestScore = sim;
        bestCandidate = candidate;
      }
    }
  }

  return {
    heardText: bestCandidate,
    matchPercent: bestScore,
    isAccepted: bestScore >= 72,
  };
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

interface SpeechRecognitionAlternativeLike {
  transcript: string;
}

interface SpeechRecognitionResultLike {
  length: number;
  [index: number]: SpeechRecognitionAlternativeLike;
}

interface SpeechRecognitionEventLike {
  results: {
    length: number;
    [index: number]: SpeechRecognitionResultLike;
  };
}

interface SpeechRecognitionErrorEventLike {
  error: string;
}

export interface BrowserSpeechRecognition {
  lang: string;
  interimResults: boolean;
  maxAlternatives: number;
  continuous: boolean;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onerror: ((event: SpeechRecognitionErrorEventLike) => void) | null;
  onend: (() => void) | null;
  start: () => void;
  stop: () => void;
  abort: () => void;
}

type SpeechRecognitionCtor = new () => BrowserSpeechRecognition;

function getSpeechRecognitionCtor(): SpeechRecognitionCtor | null {
  if (typeof window === 'undefined') return null;
  const w = window as unknown as {
    SpeechRecognition?: SpeechRecognitionCtor;
    webkitSpeechRecognition?: SpeechRecognitionCtor;
  };
  return w.SpeechRecognition ?? w.webkitSpeechRecognition ?? null;
}

export function isWebSpeechRecognitionSupported(): boolean {
  return getSpeechRecognitionCtor() !== null;
}

export function mapSpeechErrorCz(errorCode: string): string {
  switch (errorCode) {
    case 'no-speech':
      return 'Neslyšel jsem slovíčko, zkus to říct ještě jednou nahlas!';
    case 'not-allowed':
    case 'service-not-allowed':
      return 'Pro kontrolu výslovnosti je potřeba povolit přístup k mikrofonu v prohlížeči.';
    case 'audio-capture':
      return 'Problém s mikrofonem. Zkontroluj, zda není používán jinou aplikací.';
    case 'network':
      return 'Rozpoznávání řeči potřebuje připojení k internetu.';
    case 'aborted':
      return '';
    default:
      return `Nepodařilo se rozpoznat řeč (${errorCode}).`;
  }
}

export function createEnglishSpeechRecognizer(callbacks: {
  onResults: (candidates: string[]) => void;
  onError: (message: string) => void;
  onEnd: () => void;
}): BrowserSpeechRecognition | null {
  const Ctor = getSpeechRecognitionCtor();
  if (!Ctor) return null;

  const rec = new Ctor();
  rec.lang = 'en-US';
  rec.interimResults = false;
  rec.maxAlternatives = 5;
  rec.continuous = false;

  rec.onresult = (event) => {
    const candidates: string[] = [];
    for (let i = 0; i < event.results.length; i++) {
      const res = event.results[i];
      for (let j = 0; j < res.length; j++) {
        const transcript = res[j]?.transcript?.trim();
        if (transcript && !candidates.includes(transcript)) {
          candidates.push(transcript);
        }
      }
    }
    callbacks.onResults(candidates);
  };

  rec.onerror = (event) => {
    const msg = mapSpeechErrorCz(event.error);
    if (msg) callbacks.onError(msg);
  };

  rec.onend = () => {
    callbacks.onEnd();
  };

  return rec;
}
