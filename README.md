# DeepCalc

```{=html}
<p align="center">
```
`<a href="https://prasannaone.github.io/DeepCalc/">`{=html}
`<img src="https://img.shields.io/badge/OPEN%20DEEPCALC-LIVE%20WEB%20APP-7650c8?style=for-the-badge&logo=githubpages&logoColor=white" alt="Open DeepCalc">`{=html}
`</a>`{=html}
```{=html}
</p>
```
```{=html}
<h1 align="center">
```
`<a href="https://prasannaone.github.io/DeepCalc/">`{=html}↗ OPEN
DEEPCALC`</a>`{=html}
```{=html}
</h1>
```
```{=html}
<p align="center">
```
`<strong>`{=html}Write it. Work it out. See it unfold.`</strong>`{=html}
```{=html}
</p>
```
```{=html}
<p align="center">
```
`<a href="https://github.com/PrasannaOne/DeepCalc">`{=html}`<img src="https://img.shields.io/badge/source-GitHub-24292f?style=flat&logo=github" alt="GitHub source">`{=html}`</a>`{=html}
`<a href="LICENSE">`{=html}`<img src="https://img.shields.io/badge/license-MIT-21815e?style=flat" alt="MIT licence">`{=html}`</a>`{=html}
`<img src="https://img.shields.io/badge/platform-Web%20%7C%20PWA-7650c8?style=flat" alt="Web and PWA">`{=html}
`<img src="https://img.shields.io/badge/built%20with-HTML%20%7C%20CSS%20%7C%20JavaScript-f0db4f?style=flat&logo=javascript&logoColor=222" alt="HTML CSS JavaScript">`{=html}
```{=html}
</p>
```
```{=html}
<p align="center">
```
`<a href="https://prasannaone.github.io/DeepCalc/">`{=html}
`<img src="og-image.png" alt="DeepCalc: Think it. Note it. Calculate it." width="100%">`{=html}
`</a>`{=html}
```{=html}
</p>
```
DeepCalc is a notepad-style calculator for working through calculations
as a connected set of lines. Write expressions, define variables, refer
to earlier results, add notes and watch your work update as you go.

It is a browser-based adaptation of
[SoulCalc](https://github.com/tigrino/soulcalc), with a responsive
interface and installable Progressive Web App (PWA).

------------------------------------------------------------------------

## At a glance

-   **Live app:**
    [prasannaone.github.io/DeepCalc](https://prasannaone.github.io/DeepCalc/)
-   **Source code:**
    [github.com/PrasannaOne/DeepCalc](https://github.com/PrasannaOne/DeepCalc)
-   **Original project:** [SoulCalc by Albert "Tigr"
    Zenkoff](https://github.com/tigrino/soulcalc)
-   **Licence:** MIT. See [LICENSE](LICENSE).

## Features

### A calculation sheet, not just a calculator

-   Work through multiple expressions, one line at a time.
-   See results update as you edit.
-   Keep intermediate steps visible.
-   Insert, edit and remove calculation lines.
-   Use comments and blank lines to organise a sheet.

### Variables and line references

-   Define named variables and reuse them in later expressions.
-   Refer to earlier calculation results by line number.
-   Changes to earlier expressions are reflected when the sheet
    recalculates.

Example:

``` text
$price = 49.99
$quantity = 3
$price × $quantity
```

Line references use a dollar sign followed by the line number, such as
`$1` or `$2`.

### Notes beside your calculations

-   Expand a line's note area to add context, reminders or explanations.
-   Notes remain separate from calculation expressions.
-   Notes do not change calculation results or line references.
-   Note fields use the device's native keyboard.

### Flexible input

-   Use the built-in calculator keypad for quick entry.
-   Switch calculation fields between the custom keypad and the device
    keyboard.
-   Use cursor-aware insertion, deletion and paste actions.
-   Insert paired parentheses with the cursor positioned between them.
-   Use the variable picker for quick access to named variables.

### Copy, save and revisit

-   **Copy Sheet:** copy the calculation sheet and its results.
-   **Copy Complete:** include notes with the calculations.
-   Tap a result to copy its value.
-   Save named snapshots to History.
-   Rename, reopen or delete saved sheets.
-   Keep the automatically saved active sheet separate from explicitly
    saved History entries.

### Themes and palettes

DeepCalc includes four appearance modes:

-   **System:** follows the device appearance preference.
-   **Light:** a bright interface.
-   **Dark:** a dark interface.
-   **AMOLED:** a true-black app canvas.

Choose from five accent palettes: Purple, Sapphire, Jade, Ruby and Gold.

### Progressive Web App

DeepCalc can be installed from a supported browser and opened in an
app-style window. Its service worker caches the essential app files so
the app shell can be available offline after a successful initial load
and cache.

------------------------------------------------------------------------

## Calculation examples

DeepCalc evaluates expressions as you type.

  Operation               Example                   Result
  ----------------------- -------------------- -----------
  Addition                `12 + 8`                    `20`
  Subtraction             `12 - 8`                     `4`
  Multiplication          `12 × 8`                    `96`
  Division                `12 ÷ 4`                     `3`
  Alternative operators   `12 * 8`, `12 / 4`     `96`, `3`
  Parentheses             `(12 + 8) × 2`              `40`
  Power                   `2 ^ 8`                    `256`
  Square root             `sqrt(81)`                   `9`
  Percentage              `10%`                      `0.1`

### Percentage behaviour

Percentages use calculator-style context rules:

  Expression      Result
  ------------- --------
  `10%`            `0.1`
  `100 + 10%`      `110`
  `100 - 10%`       `90`
  `100 × 10%`       `10`
  `100 ÷ 10%`     `1000`

For addition and subtraction, a percentage on the right is calculated
against the left-hand value.

### Variables

Assign a value to a named variable using `$`:

``` text
$price = 49.99
$quantity = 3
$price × $quantity
```

The final expression evaluates to `149.97`. Variables can be reused by
later lines in the sheet.

### Line references

Use `$` followed by an earlier line number to refer to that line's
result:

``` text
100 + 50
$1 × 2
$1 + $2
```

References are one-based and refer to calculation lines.

### Comments

Start a comment with `#`:

``` text
# Monthly budget
$income = 5000
$rent = 1500
$utilities = 200
$income - $rent - $utilities
```

Comments and blank lines help organise a sheet.

------------------------------------------------------------------------

## Sheets and data

DeepCalc keeps the active sheet and saved History separate.

  -----------------------------------------------------------------------
  Data                                How it works
  ----------------------------------- -----------------------------------
  Active sheet                        Automatically saved while you work
                                      and restored when the app is opened
                                      again.

  Saved sheets                        Explicit snapshots created with
                                      **Save Sheet** and managed in
                                      **History**.
  -----------------------------------------------------------------------

Saved sheets include their calculation lines and notes. History retains
up to 100 saved sheets.

### Local storage and privacy

The active sheet, notes, saved History and preferences are stored
locally in the browser on the device.

-   DeepCalc does not provide account-based cloud synchronisation in
    this version.
-   Local data normally remains available across reloads and app
    updates.
-   Clearing browser or site data can erase saved work.
-   A different browser or browser profile has separate local storage.
-   Private or incognito browsing may discard data when the session
    ends.
-   Keep a separate copy of important work before clearing site data or
    changing devices.

------------------------------------------------------------------------

## Install and use offline

1.  Open [DeepCalc](https://prasannaone.github.io/DeepCalc/) in a
    supported browser.
2.  Choose the browser's **Install app** or **Add to Home Screen**
    option when offered.
3.  Open DeepCalc from your home screen or app launcher.

Install options vary by browser and operating system. The website can
also be used without installing it.

### Offline availability

The service worker caches the app shell and essential files. After those
files have been cached successfully, DeepCalc can reopen offline. The
first visit requires a network connection. Offline availability can also
be affected by browser storage policies.

When a new version is published, the service worker checks for updated
files and manages the app cache. Browser update timing can vary, so a
newly deployed release may not appear immediately in every open tab or
installed app.

------------------------------------------------------------------------

## Project structure

All application files are kept in the repository root.

  -----------------------------------------------------------------------
  File                                Purpose
  ----------------------------------- -----------------------------------
  `index.html`                        App structure, metadata, dialogs
                                      and entry point.

  `styles.css`                        Layout, responsive rules, themes,
                                      palettes and visual states.

  `app.js`                            App state, persistence, calculation
                                      engine, editing, notes, keyboard,
                                      clipboard, sharing and History.

  `manifest.webmanifest`              PWA name, launch settings and icon
                                      declarations.

  `service-worker.js`                 App-shell caching, offline fallback
                                      and update lifecycle.

  `icon-192.png`                      192 × 192 PWA icon.

  `icon-512.png`                      512 × 512 PWA icon.

  `icon-maskable-512.png`             Maskable PWA icon.

  `apple-touch-icon.png`              Apple home-screen icon.

  `favicon-32.png`                    Browser tab icon.

  `og-image.png`                      Social sharing preview artwork.

  `README.md`                         Project documentation.

  `LICENSE`                           MIT licence and original copyright
                                      notice.
  -----------------------------------------------------------------------

### Code map

**`index.html`** - Defines the interface and dialog structure. -
Contains page metadata and social-preview metadata. - Loads the
stylesheet and application script.

**`styles.css`** - Defines the calculator layout, calculation rows,
notes, menus, dialogs and keypad. - Contains responsive rules for
different screen sizes. - Defines appearance modes and accent palette
variables.

**`app.js`** - Manages application state and browser storage. -
Tokenizes, parses and evaluates expressions. - Recalculates lines and
resolves variables and line references. - Handles editing, cursor-aware
input, notes, keypad actions and clipboard operations. - Manages
sharing, saved sheets, History, themes and dialogs. - Registers and
checks the service worker.

**`service-worker.js`** - Caches the essential app shell. - Removes
older versioned app caches during activation. - Provides cached files
when the network is unavailable.

------------------------------------------------------------------------

## Run locally

Serve the repository with a local HTTP server:

``` bash
python -m http.server 8000
```

Then open:

``` text
http://localhost:8000/
```

Using a local server is preferable to opening `index.html` directly.
Service workers and PWA features require a secure context, normally
HTTPS or localhost.

------------------------------------------------------------------------

## Maintenance checklist

Before publishing changes:

1.  Keep filenames and paths consistent, including uppercase and
    lowercase characters.
2.  Confirm every manifest icon points to an existing image.
3.  Test expressions, operator precedence, percentages, variables and
    line references.
4.  Test line insertion, deletion, cursor placement, paste and keyboard
    switching.
5.  Test notes, copying, sharing, saving, renaming, reopening and
    deleting sheets.
6.  Reload the app and confirm the active sheet and History persist.
7.  Check text and keypad contrast in every theme and palette.
8.  Test narrow phone screens, landscape orientation and desktop
    layouts.
9.  Test first-load installation, offline startup and updates from the
    previous release.

### Protect the calculation engine

The tokenizer, parser, evaluator and sheet recalculation logic are
central to DeepCalc. Interface changes should not unintentionally alter
calculation behaviour.

When changing calculation code, test operator precedence, parentheses,
percentages, negative values, variable definitions, line references,
invalid expressions and division by zero.

### Service worker changes

When changing files cached by the service worker:

-   Update the cache version in `service-worker.js`.
-   Keep the core-file list aligned with the files that actually exist.
-   Check that all paths resolve from the repository root.
-   Test an update from an already installed or previously opened
    version.

------------------------------------------------------------------------

## Sharing

The Share action uses the device's native share sheet when the browser
supports it. The message is intentionally short so it complements the
link preview rather than repeating the app name, tagline and artwork.

**Share description:**

> A notepad calculator for thoughts, notes and calculations.

**Website:** <https://prasannaone.github.io/DeepCalc/>

The page provides social sharing metadata and preview artwork. The
appearance of a link preview depends on the receiving platform and its
preview cache.

------------------------------------------------------------------------

## Credits

DeepCalc is a web adaptation and ongoing project by
[PrasannaOne](https://github.com/PrasannaOne).

It is based on [SoulCalc](https://github.com/tigrino/soulcalc),
originally created by **Albert "Tigr" Zenkoff**. The original MIT
licence and copyright notice are retained in this repository.

------------------------------------------------------------------------

```{=html}
<p align="center">
```
`<strong>`{=html}`<a href="https://prasannaone.github.io/DeepCalc/">`{=html}OPEN
DEEPCALC ↗`</a>`{=html}`</strong>`{=html} `<br>`{=html}
`<sub>`{=html}Write it. Work it out. See it unfold.`</sub>`{=html}
```{=html}
</p>
```
