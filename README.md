# DeepCalc

**Think it. Note it. Calculate it.**

DeepCalc is a notepad-style calculator for working through expressions, live results, variables and independent notes in one place.

## Features

- Multiline calculations with live results.
- Arithmetic, percentages, parentheses, powers and square roots.
- Named variables and references to previous calculation lines.
- Comments, automatic recalculation and local autosave.
- Built-in calculator keyboard, with optional device keyboard for expressions.
- Independent notes that use the device keyboard.
- Copy Sheet and Copy Complete.
- Explicitly saved sheets with History, rename, reopen and delete.
- System, Light, Dark and AMOLED themes, plus accent palettes.
- Installable PWA with offline app-shell support and automatic update checks.

## Project files

All files are kept in the repository root for easy editing and uploading from GitHub's mobile interface.

| File | Purpose |
| --- | --- |
| `index.html` | App structure, metadata, dialogs and entry point. |
| `styles.css` | All layout, themes, palettes and responsive styling. |
| `app.js` | Calculator engine, sheet editing, notes, History, clipboard and app interactions. |
| `manifest.webmanifest` | PWA name, icons, scope and install behaviour. |
| `service-worker.js` | Offline app shell, cache management and fresh deployment fetching. |
| `icon-192.png`, `icon-512.png`, `icon-maskable-512.png` | Standard and maskable PWA launcher icons. |
| `apple-touch-icon.png`, `favicon-32.png` | Apple home-screen and browser tab icons. |
| `og-image.png` | 1200 × 630 social-sharing preview. |
| `LICENSE` | Retained original MIT licence and attribution. |

## Editing guide

- Change the interface or visual styling in `styles.css`.
- Change calculator behaviour and interactions in `app.js`. The parser/evaluator section is deliberately isolated and documented; avoid changing it unless modifying calculation behaviour is intended.
- Change app metadata, dialogs or page structure in `index.html`.
- When publishing a new release, update the cache name in `service-worker.js` (for example, `deepcalc-shell-v8`) and update the `?v=7` query on `styles.css` and `app.js` in `index.html` and `service-worker.js`. This ensures installed copies fetch the new assets.
- Keep all paths and file names exactly as written. GitHub Pages paths are case-sensitive.

## Run locally

A PWA requires HTTPS or localhost. From this directory, run:

```bash
python -m http.server 8000
```

Then open `http://localhost:8000/` in a modern browser.

Calculation sheets and saved History are stored locally in the browser and do not automatically sync across devices.

## Credits

Web adaptation and ongoing development: [PrasannaOne](https://github.com/PrasannaOne)

Based on [SoulCalc](https://github.com/tigrino/soulcalc), originally created by Albert “Tigr” Zenkoff. The original MIT licence and copyright notice are retained.

Project repository: [PrasannaOne/DeepCalc](https://github.com/PrasannaOne/DeepCalc)
