# DeepCalc

**Think it. Note it. Calculate it.**

DeepCalc is a notepad-style calculator for expressions, live results, variables and independent notes.

## Features

- Multiline calculations, live results, variables and previous-line references.
- Arithmetic, percentages, parentheses, powers, square roots and comments.
- Built-in calculator keyboard, with an optional device keyboard for expressions.
- Independent notes that use the device keyboard.
- Copy Sheet and Copy Complete.
- Explicitly saved sheets with History, rename, reopen and delete.
- System, Light, Dark and true-black AMOLED themes with five coordinated palettes.
- Installable PWA with offline app-shell support and automatic update checks.
- Responsive layout for phones, tablets and desktop screens.

## Root files

All files intentionally stay in the repository root. No asset folders are required.

| File | What it contains |
| --- | --- |
| `index.html` | Page structure, metadata, dialogs and app entry point. |
| `styles.css` | Layout, responsive rules, themes, palette tokens and visual states. |
| `app.js` | State, storage, calculator parser/evaluator, editing, notes, History and sharing. |
| `manifest.webmanifest` | Install identity, display settings and icon declarations. |
| `service-worker.js` | Offline app shell, cache versioning and update lifecycle. |
| `icon-192.png`, `icon-512.png`, `icon-maskable-512.png` | PWA launcher artwork. |
| `apple-touch-icon.png`, `favicon-32.png` | Apple home-screen and browser tab artwork. |
| `og-image.png` | Social preview artwork (1200 × 630). |
| `LICENSE` | Original MIT licence and copyright notice. Keep it intact. |

## Code map

- **`index.html`**: edit labels, dialogs and metadata. Keep file paths root-relative.
- **`styles.css`**: edit visual design. The V8 release section at the bottom contains the coordinated palette tokens and long-result responsive rules.
- **`app.js`**: major sections are labelled. The lexer/parser/evaluator is isolated from the interface. Avoid changing it unless calculator semantics are being deliberately changed.
- **`service-worker.js`**: core files must remain in `CORE_FILES`. Optional images are cached best-effort. Increment `CACHE_NAME` for every release and update the `?v=` values in `index.html` and the service worker together.

## Local testing

A PWA requires HTTPS or localhost. From this directory:

```bash
python -m http.server 8000
```

Open `http://localhost:8000/` in a modern browser. Test calculations, line references, notes, keyboard modes, copy/share, History, theme changes, offline startup and an update from the previous deployed version before publishing.

## Data and privacy

The active sheet, preferences and saved History are stored in this browser's local storage. They survive normal reloads and app updates, but they do not sync between devices. Clearing site data or using a different browser/device can remove or leave behind those records. Export or copy important work before clearing browser data.

## Credits

Web adaptation and ongoing development: [PrasannaOne](https://github.com/PrasannaOne).

Based on [SoulCalc](https://github.com/tigrino/soulcalc), originally created by Albert “Tigr” Zenkoff. The original MIT licence and copyright notice are retained.

Project repository: [PrasannaOne/DeepCalc](https://github.com/PrasannaOne/DeepCalc).
