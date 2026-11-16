# Slovíčka — Webová aplikace (React + Vite + Tailwind CSS)

Moderní výuková webová aplikace pro děti (iOS Safari, Android Chrome i desktop), plně uložená v prohlížeči (`localStorage`), bez nutnosti databáze či serverového backendu.

## Funkce
- **12 výchozích balíčků**: Unit 1–8, Čísla 1–10, Zvířata, Barvy, Ovoce a zelenina
- **7 typů cvičení**: Spojovačka, Doplňovačka (s předvyplněním 1–2 písmen), Výslovnost (Web Speech API + hodnocení shody), Střílečka (vesmírná HTML5 Canvas minihra), Pexeso, Anagram, Kvíz
- **Rodičovský režim**: profily dětí (`Dítě 1` + vlastní avataři), PIN zámek, správa balíčků a slovíček, databanka obrázků (Iconify / Openverse / Wikipedia) i nahrání vlastních obrázků, detailní přehled pokroku

## Lokální spuštění
```bash
cd web
npm install
npm run dev
```

## Nasazení na Vercel
1. Nahrajte repozitář na GitHub.
2. Na Vercelu vytvořte nový projekt a nastavte **Root Directory** na `web`.
3. Build Command: `npm run build`, Output Directory: `dist` (automaticky detekováno pro Vite).
4. Soubor `vercel.json` zajišťuje správné SPA směrování a mikrofon (Web Speech API) funguje automaticky přes HTTPS.
