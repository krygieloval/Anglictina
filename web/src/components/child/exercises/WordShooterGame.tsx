import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ArrowLeft, ChevronLeft, ChevronRight, Crosshair, Heart, Trophy, Volume2 } from 'lucide-react';
import { speakCzech, speakEnglish } from '../../../services/speech';
import { VocabWord } from '../../../types/vocab';
import { PracticeOutcomeItem } from '../PracticeSummaryScreen';

interface WordShooterGameProps {
  words: VocabWord[];
  onBack: () => void;
  onFinish: (outcomes: PracticeOutcomeItem[], starsEarned: number, score: number) => void;
}

interface AsteroidEntity {
  id: string;
  word: VocabWord;
  xNorm: number; // 0.14 .. 0.86
  yNorm: number; // -0.05 .. 1.0
  speedPerSec: number;
  radiusPx: number;
  colorIndex: number;
  wobblePhase: number;
  hitFlash: boolean;
}

interface LaserBolt {
  xNorm: number;
  yNorm: number;
}

interface Particle {
  xNorm: number;
  yNorm: number;
  vx: number;
  vy: number;
  life: number;
  color: string;
}

interface BgStar {
  x: number;
  y: number;
  r: number;
  alpha: number;
}

const ASTEROID_COLORS = [
  ['#4F46E5', '#818CF8'],
  ['#DB2777', '#F472B6'],
  ['#059669', '#34D399'],
  ['#D97706', '#FBBF24'],
];

function shuffle<T>(arr: T[]): T[] {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

export const WordShooterGame: React.FC<WordShooterGameProps> = ({
  words,
  onBack,
  onFinish,
}) => {
  const roundQueue = useMemo(() => {
    const base = shuffle(words);
    const total = Math.min(10, Math.max(words.length, 6));
    const list: VocabWord[] = [];
    while (list.length < total && base.length > 0) {
      list.push(...shuffle(base));
    }
    return list.slice(0, total);
  }, [words]);

  const [roundIndex, setRoundIndex] = useState(0);
  const [hearts, setHearts] = useState(3);
  const [score, setScore] = useState(0);
  const [combo, setCombo] = useState(0);
  const [banner, setBanner] = useState<{ text: string; ok: boolean } | null>(null);

  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const shipXRef = useRef(0.5);
  const shipTiltRef = useRef(0);
  const moveDirRef = useRef<-1 | 0 | 1>(0);
  const asteroidsRef = useRef<AsteroidEntity[]>([]);
  const lasersRef = useRef<LaserBolt[]>([]);
  const particlesRef = useRef<Particle[]>([]);
  const lastShotAtRef = useRef(0);
  const outcomesRef = useRef<Map<string, PracticeOutcomeItem>>(new Map());
  const roundResolvedRef = useRef(false);
  const gameOverRef = useRef(false);
  const heartsRef = useRef(3);
  const scoreRef = useRef(0);
  const comboRef = useRef(0);
  const roundIndexRef = useRef(0);

  const bgStars = useMemo<BgStar[]>(
    () =>
      Array.from({ length: 55 }, () => ({
        x: Math.random(),
        y: Math.random(),
        r: 1 + Math.random() * 1.8,
        alpha: 0.35 + Math.random() * 0.55,
      })),
    []
  );

  const currentTarget = roundQueue[roundIndex];

  const completeGame = useCallback(() => {
    if (gameOverRef.current) return;
    gameOverRef.current = true;
    const all = Array.from(outcomesRef.current.values());
    const finalOutcomes =
      all.length > 0 ? all : roundQueue.slice(0, 1).map((w) => ({ word: w, isCorrect: false }));
    const correct = finalOutcomes.filter((o) => o.isCorrect).length;
    const pct = Math.floor((correct * 100) / Math.max(1, finalOutcomes.length));
    const stars = heartsRef.current > 0 && pct >= 80 ? 3 : pct >= 50 ? 2 : 1;
    onFinish(finalOutcomes, stars, scoreRef.current);
  }, [onFinish, roundQueue]);

  const spawnRound = useCallback(
    (idx: number) => {
      const target = roundQueue[idx];
      if (!target) {
        completeGame();
        return;
      }
      roundResolvedRef.current = false;
      lasersRef.current = [];
      const distractors = shuffle(words.filter((w) => w.id !== target.id)).slice(0, 2);
      const wave = shuffle([target, ...distractors]);
      const lanes = wave.length === 1 ? [0.5] : wave.length === 2 ? [0.3, 0.7] : [0.2, 0.5, 0.8];

      asteroidsRef.current = wave.map((w, i) => ({
        id: `${idx}_${w.id}`,
        word: w,
        xNorm: lanes[i],
        yNorm: 0.04 + (i % 2) * 0.06,
        speedPerSec: 0.075 + Math.min(0.05, idx * 0.005),
        radiusPx: 30,
        colorIndex: i % ASTEROID_COLORS.length,
        wobblePhase: Math.random() * Math.PI * 2,
        hitFlash: false,
      }));

      speakCzech(target.czechTranslation);
    },
    [roundQueue, words, completeGame]
  );

  useEffect(() => {
    roundIndexRef.current = roundIndex;
    spawnRound(roundIndex);
  }, [roundIndex, spawnRound]);

  const fireLaser = useCallback(() => {
    if (gameOverRef.current || roundResolvedRef.current) return;
    const now = performance.now();
    if (now - lastShotAtRef.current < 240) return;
    lastShotAtRef.current = now;
    lasersRef.current.push({ xNorm: shipXRef.current, yNorm: 0.82 });
  }, []);

  // Keyboard support for desktop testing
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft' || e.key === 'a' || e.key === 'A') moveDirRef.current = -1;
      if (e.key === 'ArrowRight' || e.key === 'd' || e.key === 'D') moveDirRef.current = 1;
      if (e.key === ' ') {
        e.preventDefault();
        fireLaser();
      }
    };
    const onKeyUp = (e: KeyboardEvent) => {
      if (
        (e.key === 'ArrowLeft' || e.key === 'a' || e.key === 'A') &&
        moveDirRef.current === -1
      ) {
        moveDirRef.current = 0;
      }
      if (
        (e.key === 'ArrowRight' || e.key === 'd' || e.key === 'D') &&
        moveDirRef.current === 1
      ) {
        moveDirRef.current = 0;
      }
    };
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('keyup', onKeyUp);
    };
  }, [fireLaser]);

  useEffect(() => {
    let rafId = 0;
    let lastTime = performance.now();
    let cancelled = false;
    let roundAdvanceTimer: number | null = null;
    let bannerTimer: number | null = null;

    const clearRoundTimers = () => {
      if (roundAdvanceTimer !== null) {
        window.clearTimeout(roundAdvanceTimer);
        roundAdvanceTimer = null;
      }
    };

    const triggerBanner = (text: string, ok: boolean, durationMs = 1100) => {
      if (bannerTimer !== null) {
        window.clearTimeout(bannerTimer);
      }
      setBanner({ text, ok });
      bannerTimer = window.setTimeout(() => {
        if (!cancelled) setBanner(null);
      }, durationMs);
    };

    const goToNextStep = () => {
      if (cancelled) return;
      if (heartsRef.current <= 0 || roundIndexRef.current + 1 >= roundQueue.length) {
        completeGame();
      } else {
        setBanner(null);
        setRoundIndex((r) => r + 1);
      }
    };

    const advanceAfterDelay = (delayMs: number) => {
      clearRoundTimers();
      roundAdvanceTimer = window.setTimeout(goToNextStep, delayMs);
    };

    const advanceAfterSpeechAndDelay = (spokenText: string, minDelayMs = 950) => {
      clearRoundTimers();
      let speechDone = false;
      let minDelayDone = false;
      let queuedAdvance = false;

      const tryAdvance = () => {
        if (cancelled || !speechDone || !minDelayDone || queuedAdvance) return;
        queuedAdvance = true;
        roundAdvanceTimer = window.setTimeout(() => {
          goToNextStep();
        }, 250);
      };

      roundAdvanceTimer = window.setTimeout(() => {
        minDelayDone = true;
        tryAdvance();
      }, minDelayMs);

      speakEnglish(spokenText, () => {
        speechDone = true;
        tryAdvance();
      });
    };

    const spawnBurst = (xNorm: number, yNorm: number, color: string) => {
      for (let i = 0; i < 16; i++) {
        const angle = (Math.PI * 2 * i) / 16;
        const speed = 0.25 + Math.random() * 0.35;
        particlesRef.current.push({
          xNorm,
          yNorm,
          vx: Math.cos(angle) * speed,
          vy: Math.sin(angle) * speed,
          life: 1,
          color,
        });
      }
    };

    const step = (now: number) => {
      const dt = Math.min(0.05, (now - lastTime) / 1000);
      lastTime = now;

      const canvas = canvasRef.current;
      if (canvas) {
        const rect = canvas.getBoundingClientRect();
        const dpr = Math.min(2, window.devicePixelRatio || 1);
        const w = Math.max(200, Math.round(rect.width * dpr));
        const h = Math.max(200, Math.round(rect.height * dpr));
        if (canvas.width !== w || canvas.height !== h) {
          canvas.width = w;
          canvas.height = h;
        }
        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.save();
          ctx.scale(dpr, dpr);
          const cw = rect.width;
          const ch = rect.height;

          // Update ship movement
          if (moveDirRef.current !== 0) {
            shipXRef.current = Math.max(
              0.1,
              Math.min(0.9, shipXRef.current + moveDirRef.current * 0.65 * dt)
            );
            shipTiltRef.current = moveDirRef.current * 0.16;
          } else {
            shipTiltRef.current *= 0.85;
          }

          // Update lasers & asteroids
          lasersRef.current = lasersRef.current
            .map((l) => ({ ...l, yNorm: l.yNorm - 1.25 * dt }))
            .filter((l) => l.yNorm > -0.05);

          particlesRef.current = particlesRef.current
            .map((p) => ({
              ...p,
              xNorm: p.xNorm + p.vx * dt,
              yNorm: p.yNorm + p.vy * dt,
              life: p.life - dt * 2.2,
            }))
            .filter((p) => p.life > 0);

          const targetWord = roundQueue[roundIndexRef.current];

          if (!roundResolvedRef.current && targetWord) {
            for (const a of asteroidsRef.current) {
              a.yNorm += a.speedPerSec * dt;
              a.wobblePhase += dt * 2.2;
            }

            // Laser hit checks
            for (let li = lasersRef.current.length - 1; li >= 0; li--) {
              const l = lasersRef.current[li];
              const lx = l.xNorm * cw;
              const ly = l.yNorm * ch;
              let hitIndex = -1;
              for (let ai = 0; ai < asteroidsRef.current.length; ai++) {
                const a = asteroidsRef.current[ai];
                const ax = a.xNorm * cw;
                const ay = a.yNorm * ch;
                const dist = Math.hypot(lx - ax, ly - ay);
                if (dist < a.radiusPx + 12) {
                  hitIndex = ai;
                  break;
                }
              }

              if (hitIndex >= 0) {
                const hit = asteroidsRef.current[hitIndex];
                lasersRef.current.splice(li, 1);

                if (hit.word.id === targetWord.id) {
                  roundResolvedRef.current = true;
                  comboRef.current += 1;
                  scoreRef.current += 20 + comboRef.current * 5;
                  setCombo(comboRef.current);
                  setScore(scoreRef.current);
                  if (!outcomesRef.current.has(targetWord.id)) {
                    outcomesRef.current.set(targetWord.id, { word: targetWord, isCorrect: true });
                  }
                  spawnBurst(hit.xNorm, hit.yNorm, '#34D399');
                  asteroidsRef.current = [];
                  triggerBanner(`Zásah! ${hit.word.englishWord}`, true, 2400);
                  advanceAfterSpeechAndDelay(hit.word.englishWord, 950);
                  break;
                } else {
                  comboRef.current = 0;
                  heartsRef.current = Math.max(0, heartsRef.current - 1);
                  setCombo(0);
                  setHearts(heartsRef.current);
                  outcomesRef.current.set(targetWord.id, { word: targetWord, isCorrect: false });
                  spawnBurst(hit.xNorm, hit.yNorm, '#F87171');
                  asteroidsRef.current.splice(hitIndex, 1);
                  triggerBanner(`Ups! To bylo „${hit.word.englishWord}“`, false, 1100);
                  if (heartsRef.current <= 0) {
                    roundResolvedRef.current = true;
                    advanceAfterDelay(1100);
                    break;
                  }
                }
              }
            }

            // Bottom escape check
            const targetAsteroid = asteroidsRef.current.find((a) => a.word.id === targetWord.id);
            if (!roundResolvedRef.current && targetAsteroid && targetAsteroid.yNorm > 0.86) {
              roundResolvedRef.current = true;
              comboRef.current = 0;
              heartsRef.current = Math.max(0, heartsRef.current - 1);
              setCombo(0);
              setHearts(heartsRef.current);
              outcomesRef.current.set(targetWord.id, { word: targetWord, isCorrect: false });
              asteroidsRef.current = [];
              triggerBanner(`Uletěl! Bylo to „${targetWord.englishWord}“`, false, 1100);
              advanceAfterDelay(1100);
            }
          }

          // Draw Space Background
          const grad = ctx.createLinearGradient(0, 0, 0, ch);
          grad.addColorStop(0, '#090D24');
          grad.addColorStop(1, '#1A2352');
          ctx.fillStyle = grad;
          ctx.fillRect(0, 0, cw, ch);

          for (const s of bgStars) {
            ctx.fillStyle = `rgba(255,255,255,${s.alpha})`;
            ctx.beginPath();
            ctx.arc(s.x * cw, s.y * ch, s.r, 0, Math.PI * 2);
            ctx.fill();
          }

          // Draw Lasers
          for (const l of lasersRef.current) {
            const lx = l.xNorm * cw;
            const ly = l.yNorm * ch;
            ctx.fillStyle = '#38BDF8';
            ctx.shadowColor = '#38BDF8';
            ctx.shadowBlur = 10;
            ctx.fillRect(lx - 3, ly - 16, 6, 22);
            ctx.shadowBlur = 0;
          }

          // Draw Asteroids
          for (const a of asteroidsRef.current) {
            const ax = a.xNorm * cw;
            const ay = a.yNorm * ch;
            const [cDark, cLight] = ASTEROID_COLORS[a.colorIndex];

            ctx.save();
            ctx.translate(ax, ay);
            ctx.rotate(Math.sin(a.wobblePhase) * 0.12);
            ctx.fillStyle = cDark;
            ctx.strokeStyle = cLight;
            ctx.lineWidth = 3;
            ctx.beginPath();
            const points = 9;
            for (let i = 0; i < points; i++) {
              const ang = (Math.PI * 2 * i) / points;
              const rad = a.radiusPx * (i % 2 === 0 ? 1 : 0.84);
              const px = Math.cos(ang) * rad;
              const py = Math.sin(ang) * rad;
              if (i === 0) ctx.moveTo(px, py);
              else ctx.lineTo(px, py);
            }
            ctx.closePath();
            ctx.fill();
            ctx.stroke();
            ctx.restore();

            // Word pill badge over/under asteroid
            ctx.font = '800 14px Nunito, sans-serif';
            const label = a.word.englishWord;
            const tw = ctx.measureText(label).width;
            const pillW = tw + 18;
            const pillH = 26;
            const px = Math.max(6, Math.min(cw - pillW - 6, ax - pillW / 2));
            const py = ay + a.radiusPx - 6;
            ctx.fillStyle = 'rgba(255,255,255,0.96)';
            ctx.beginPath();
            ctx.roundRect(px, py, pillW, pillH, 10);
            ctx.fill();
            ctx.fillStyle = '#1E293B';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(label, px + pillW / 2, py + pillH / 2 + 1);
          }

          // Draw Burst Particles
          for (const p of particlesRef.current) {
            ctx.globalAlpha = Math.max(0, p.life);
            ctx.fillStyle = p.color;
            ctx.beginPath();
            ctx.arc(p.xNorm * cw, p.yNorm * ch, 4.5, 0, Math.PI * 2);
            ctx.fill();
          }
          ctx.globalAlpha = 1;

          // Draw Spaceship Rocket
          const sx = shipXRef.current * cw;
          const sy = ch * 0.87;
          ctx.save();
          ctx.translate(sx, sy);
          ctx.rotate(shipTiltRef.current);

          // Flame
          const flameLen = 14 + Math.sin(now * 0.03) * 5;
          ctx.fillStyle = '#F59E0B';
          ctx.beginPath();
          ctx.moveTo(-7, 16);
          ctx.lineTo(7, 16);
          ctx.lineTo(0, 16 + flameLen);
          ctx.closePath();
          ctx.fill();

          // Wings
          ctx.fillStyle = '#FF6B4A';
          ctx.beginPath();
          ctx.moveTo(-12, 4);
          ctx.lineTo(-24, 18);
          ctx.lineTo(-10, 16);
          ctx.closePath();
          ctx.fill();
          ctx.beginPath();
          ctx.moveTo(12, 4);
          ctx.lineTo(24, 18);
          ctx.lineTo(10, 16);
          ctx.closePath();
          ctx.fill();

          // Fuselage
          ctx.fillStyle = '#F8FAFC';
          ctx.beginPath();
          ctx.moveTo(0, -24);
          ctx.quadraticCurveTo(15, -4, 12, 16);
          ctx.lineTo(-12, 16);
          ctx.quadraticCurveTo(-15, -4, 0, -24);
          ctx.fill();

          // Window
          ctx.fillStyle = '#38BDF8';
          ctx.beginPath();
          ctx.arc(0, -3, 6, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore();

          ctx.restore();
        }
      }

      rafId = window.requestAnimationFrame(step);
    };

    rafId = window.requestAnimationFrame(step);
    return () => {
      cancelled = true;
      window.cancelAnimationFrame(rafId);
      clearRoundTimers();
      if (bannerTimer !== null) window.clearTimeout(bannerTimer);
    };
  }, [bgStars, completeGame, roundQueue]);

  const handleCanvasPointer = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const xNorm = Math.max(0.1, Math.min(0.9, (e.clientX - rect.left) / Math.max(1, rect.width)));
    shipXRef.current = xNorm;
    if (e.type === 'pointerdown') {
      fireLaser();
    }
  };

  if (!currentTarget) return null;

  return (
    <div className="mx-auto flex max-w-2xl flex-col space-y-3 px-3 py-3 sm:px-4">
      <div className="flex items-center justify-between gap-2">
        <button
          type="button"
          onClick={onBack}
          aria-label="Zpět"
          className="flex h-11 w-11 items-center justify-center rounded-full hover:bg-slate-200/70"
        >
          <ArrowLeft className="h-6 w-6" />
        </button>

        <div className="flex items-center gap-1">
          {[0, 1, 2].map((i) => (
            <Heart
              key={i}
              className={`h-5 w-5 ${
                i < hearts ? 'fill-red-500 text-red-500' : 'text-slate-300'
              }`}
            />
          ))}
        </div>

        <div className="flex items-center gap-2">
          {combo > 1 && (
            <span className="rounded-full bg-brand-coralLight px-2.5 py-1 text-xs font-extrabold text-brand-coralDark">
              {combo}× Combo!
            </span>
          )}
          <div className="flex items-center gap-1 rounded-full bg-brand-amberLight px-3 py-1 text-sm font-extrabold text-brand-amberDark">
            <Trophy className="h-4 w-4 text-brand-amber" />
            <span>{score}</span>
          </div>
        </div>
      </div>

      <button
        type="button"
        onClick={() => speakCzech(currentTarget.czechTranslation)}
        className="flex items-center justify-between rounded-2xl bg-brand-indigo px-4 py-2.5 text-white shadow-sm active:scale-[0.99]"
      >
        <div className="text-left">
          <p className="text-[11px] font-bold uppercase tracking-wide text-white/80">
            Kolo {roundIndex + 1} / {roundQueue.length} • Sestřel překlad pro:
          </p>
          <p className="font-display text-2xl font-extrabold leading-tight">
            {currentTarget.czechTranslation}
          </p>
        </div>
        <div className="flex h-11 w-11 items-center justify-center rounded-full bg-white/20">
          <Volume2 className="h-6 w-6" />
        </div>
      </button>

      <div className="relative overflow-hidden rounded-3xl border-2 border-brand-indigo/30 shadow-md">
        <canvas
          ref={canvasRef}
          onPointerDown={handleCanvasPointer}
          onPointerMove={(e) => {
            if (e.buttons > 0 || e.pointerType === 'touch') handleCanvasPointer(e);
          }}
          className="block h-[380px] w-full touch-none sm:h-[420px]"
        />

        {banner && (
          <div
            className={`pointer-events-none absolute left-1/2 top-4 -translate-x-1/2 rounded-full px-4 py-1.5 text-sm font-extrabold shadow-lg ${
              banner.ok ? 'bg-brand-mint text-white' : 'bg-red-500 text-white'
            }`}
          >
            {banner.text}
          </div>
        )}
      </div>

      <div className="flex items-center gap-2.5">
        <button
          type="button"
          onPointerDown={() => (moveDirRef.current = -1)}
          onPointerUp={() => (moveDirRef.current = 0)}
          onPointerLeave={() => (moveDirRef.current = 0)}
          onPointerCancel={() => (moveDirRef.current = 0)}
          aria-label="Doleva"
          className="flex h-14 w-[72px] items-center justify-center rounded-2xl border border-brand-outline bg-white text-brand-ink shadow-sm active:bg-slate-100 select-none"
        >
          <ChevronLeft className="h-7 w-7" />
        </button>

        <button
          type="button"
          onClick={fireLaser}
          className="flex h-14 flex-1 items-center justify-center gap-2 rounded-2xl bg-brand-coral font-display text-xl font-extrabold text-white shadow-md active:scale-95 select-none"
        >
          <Crosshair className="h-6 w-6" />
          <span>Vystřelit!</span>
        </button>

        <button
          type="button"
          onPointerDown={() => (moveDirRef.current = 1)}
          onPointerUp={() => (moveDirRef.current = 0)}
          onPointerLeave={() => (moveDirRef.current = 0)}
          onPointerCancel={() => (moveDirRef.current = 0)}
          aria-label="Doprava"
          className="flex h-14 w-[72px] items-center justify-center rounded-2xl border border-brand-outline bg-white text-brand-ink shadow-sm active:bg-slate-100 select-none"
        >
          <ChevronRight className="h-7 w-7" />
        </button>
      </div>
    </div>
  );
};
