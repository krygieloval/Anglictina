export interface PronunciationEvaluation {
  heardText: string;
  matchPercent: number;
  isAccepted: boolean;
  isPhoneticMatch: boolean;
}

const DIGIT_TO_WORD: Record<string, string> = {
  '0': 'zero',
  '1': 'one',
  '2': 'two',
  '3': 'three',
  '4': 'four',
  '5': 'five',
  '6': 'six',
  '7': 'seven',
  '8': 'eight',
  '9': 'nine',
  '10': 'ten',
};

function normalizeText(input: string): string {
  const cleaned = input
    .toLowerCase()
    .replace(/['’]/g, '')
    .replace(/[^a-z0-9 ]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  if (!cleaned) return '';
  return cleaned
    .split(' ')
    .map((tok) => DIGIT_TO_WORD[tok] ?? tok)
    .join(' ');
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

/**
 * American Soundex encoding for English words.
 */
export function soundex(raw: string): string {
  const word = raw.toUpperCase().replace(/[^A-Z]/g, '');
  if (!word) return '';
  const mapChar = (c: string): string => {
    if ('BFPV'.includes(c)) return '1';
    if ('CGJKQSXZ'.includes(c)) return '2';
    if ('DT'.includes(c)) return '3';
    if (c === 'L') return '4';
    if ('MN'.includes(c)) return '5';
    if (c === 'R') return '6';
    return '';
  };

  let code = word[0];
  let prev = mapChar(word[0]);
  for (let i = 1; i < word.length && code.length < 4; i++) {
    const ch = word[i];
    if ('HW'.includes(ch)) continue;
    const mapped = mapChar(ch);
    if (mapped && mapped !== prev) {
      code += mapped;
    }
    prev = mapped;
  }
  return (code + '000').slice(0, 4);
}

/**
 * Simplified English Metaphone encoder handling silent prefixes/clusters and homophones
 * (e.g. write/right, know/no, sea/see, blue/blew, four/for, eight/ate).
 */
export function metaphone(raw: string): string {
  let w = raw.toUpperCase().replace(/[^A-Z]/g, '');
  if (!w) return '';

  // Silent starting clusters
  if (/^(KN|GN|PN|AE|WR)/.test(w)) {
    w = w.slice(1);
  } else if (w.startsWith('X')) {
    w = 'S' + w.slice(1);
  } else if (w.startsWith('WH')) {
    w = 'W' + w.slice(2);
  }

  // Collapse adjacent identical letters (except C)
  w = w.replace(/([ABDEFGHIJKLMNOPQRSTUVWXYZ])\1+/g, '$1');

  let out = '';
  const vowels = 'AEIOU';

  for (let i = 0; i < w.length; i++) {
    const c = w[i];
    const prev = i > 0 ? w[i - 1] : '';
    const next = i + 1 < w.length ? w[i + 1] : '';
    const next2 = i + 2 < w.length ? w[i + 2] : '';

    if (vowels.includes(c)) {
      if (i === 0) out += 'A';
      continue;
    }

    switch (c) {
      case 'B':
        if (!(i === w.length - 1 && prev === 'M')) out += 'B';
        break;
      case 'C':
        if (next === 'I' && next2 === 'A') out += 'X';
        else if (next === 'H') {
          out += 'X';
          i++;
        } else if ('EIY'.includes(next)) out += 'S';
        else out += 'K';
        break;
      case 'D':
        if (next === 'G' && 'EIY'.includes(next2)) {
          out += 'J';
          i++;
        } else out += 'T';
        break;
      case 'F':
        out += 'F';
        break;
      case 'G':
        if (next === 'H') {
          if (i > 0 && !vowels.includes(prev)) {
            i++;
          } else if (i === 0) {
            out += 'K';
            i++;
          } else {
            i++;
          }
        } else if (next === 'N' && i + 2 === w.length) {
          break;
        } else if ('EIY'.includes(next)) {
          out += 'J';
        } else {
          out += 'K';
        }
        break;
      case 'H':
        if (vowels.includes(next) && !'CSPTG'.includes(prev)) out += 'H';
        break;
      case 'J':
        out += 'J';
        break;
      case 'K':
        if (prev !== 'C') out += 'K';
        break;
      case 'L':
        out += 'L';
        break;
      case 'M':
        out += 'M';
        break;
      case 'N':
        out += 'N';
        break;
      case 'P':
        if (next === 'H') {
          out += 'F';
          i++;
        } else {
          out += 'P';
        }
        break;
      case 'Q':
        out += 'K';
        break;
      case 'R':
        out += 'R';
        break;
      case 'S':
        if (next === 'H' || (next === 'I' && (next2 === 'O' || next2 === 'A'))) {
          out += 'X';
          if (next === 'H') i++;
        } else {
          out += 'S';
        }
        break;
      case 'T':
        if (next === 'H') {
          out += '0';
          i++;
        } else if (next === 'I' && (next2 === 'O' || next2 === 'A')) {
          out += 'X';
        } else {
          out += 'T';
        }
        break;
      case 'V':
        out += 'F';
        break;
      case 'W':
      case 'Y':
        if (vowels.includes(next)) out += c;
        break;
      case 'X':
        out += 'KS';
        break;
      case 'Z':
        out += 'S';
        break;
      default:
        break;
    }
  }

  return out;
}

function arePhoneticallyEqual(target: string, candidate: string): boolean {
  if (!target || !candidate) return false;
  if (target === candidate) return true;

  // Compact multi-word phrases cleanly
  const tCompact = target.replace(/\s+/g, '');
  const cCompact = candidate.replace(/\s+/g, '');
  if (!tCompact || !cCompact) return false;

  const mTarget = metaphone(tCompact);
  const mCand = metaphone(cCompact);
  const sim = similarityPercent(tCompact, cCompact);

  if (mTarget && mTarget === mCand) {
    if (mTarget.length >= 2 || sim >= 50) {
      return true;
    }
  }

  if (tCompact.length >= 3 && cCompact.length >= 3 && Math.abs(tCompact.length - cCompact.length) <= 2) {
    const sTarget = soundex(tCompact);
    const sCand = soundex(cCompact);
    if (sTarget && sTarget === sCand && sim >= 50) {
      return true;
    }
  }

  return false;
}

export function evaluatePronunciation(
  targetWord: string,
  candidates: string[]
): PronunciationEvaluation {
  const cleanTarget = normalizeText(targetWord);
  if (candidates.length === 0 || !cleanTarget) {
    return { heardText: '', matchPercent: 0, isAccepted: false, isPhoneticMatch: false };
  }

  let bestCandidate = candidates[0];
  let bestScore = 0;
  let phoneticMatched = false;

  for (const candidate of candidates) {
    const normalized = normalizeText(candidate);
    if (!normalized) continue;
    const tokens = normalized.split(' ').filter(Boolean);
    const options = Array.from(new Set([normalized, ...tokens]));

    for (const opt of options) {
      const sim = similarityPercent(cleanTarget, opt);
      const phonetic = arePhoneticallyEqual(cleanTarget, opt);

      if (phonetic && (!phoneticMatched || sim > bestScore)) {
        phoneticMatched = true;
        bestScore = Math.max(bestScore, sim);
        bestCandidate = candidate;
      } else if (sim > bestScore && !phoneticMatched) {
        bestScore = sim;
        bestCandidate = candidate;
      }
    }
  }

  const isAccepted = bestScore >= 70 || phoneticMatched;
  const displayPercent = phoneticMatched ? Math.max(bestScore, 85) : bestScore;

  return {
    heardText: bestCandidate,
    matchPercent: displayPercent,
    isAccepted,
    isPhoneticMatch: phoneticMatched && bestScore < 70,
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
  isFinal?: boolean;
  [index: number]: SpeechRecognitionAlternativeLike;
}

interface SpeechRecognitionEventLike {
  resultIndex?: number;
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
  onstart: (() => void) | null;
  onaudiostart: (() => void) | null;
  onspeechstart: (() => void) | null;
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

export interface SpeechRecognitionBatch {
  candidates: string[];
  isFinal: boolean;
}

export function createEnglishSpeechRecognizer(callbacks: {
  onStart?: () => void;
  onResults: (batch: SpeechRecognitionBatch) => void;
  onError: (message: string) => void;
  onEnd: () => void;
}): BrowserSpeechRecognition | null {
  const Ctor = getSpeechRecognitionCtor();
  if (!Ctor) return null;

  const rec = new Ctor();
  rec.lang = 'en-US';
  rec.interimResults = true;
  rec.maxAlternatives = 5;
  rec.continuous = false;

  rec.onstart = () => {
    callbacks.onStart?.();
  };

  rec.onaudiostart = () => {
    callbacks.onStart?.();
  };

  rec.onresult = (event) => {
    const candidates: string[] = [];
    let anyFinal = false;

    for (let i = 0; i < event.results.length; i++) {
      const res = event.results[i];
      if (res?.isFinal) anyFinal = true;
      for (let j = 0; j < res.length; j++) {
        const transcript = res[j]?.transcript?.trim();
        if (transcript && !candidates.includes(transcript)) {
          candidates.push(transcript);
        }
      }
    }

    if (candidates.length > 0) {
      callbacks.onResults({ candidates, isFinal: anyFinal });
    }
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
