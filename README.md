DeepCalc

A simple notepad-style calculator for the web.

DeepCalc lets you work through calculations line by line. Results update as you type, so you can handle quick arithmetic and longer calculation sheets without constantly reaching for an equals button.

Features

- Multiline calculations with live results.
- Basic arithmetic, parentheses, powers and square roots.
- Contextual percentage calculations.
- Named variables such as "$price = 100".
- References to previous results using "$1", "$2", etc.
- Comments and blank lines to organise calculations.
- Automatic recalculation of dependent expressions.
- Automatic local saving in your browser.
- Built-in calculator keyboard with system keyboard support.
- Variable quick picker.
- Copy individual results or the entire sheet.
- Light, dark and system themes.

Example

$price = 49.99
$quantity = 3

$price * $quantity
100 + 10%
$1 * 2

Run locally

DeepCalc is currently a standalone HTML project.

1. Download or clone this repository.
2. Open "index.html" in a modern browser.
3. Start calculating!

Your calculation sheet is saved locally in your browser. It does not automatically sync across devices.

Project structure

DeepCalc/
├── index.html
├── README.md
└── LICENSE

The project is maintained as a single HTML file for now, making it easy to inspect, edit and host.

Credits

Web adaptation and ongoing development: "PrasannaOne" (https://github.com/PrasannaOne)

Adapted from "SoulCalc" (https://github.com/tigrino/soulcalc), originally created by Albert “Tigr” Zenkoff.

The original MIT licence and copyright notice are retained in the "LICENSE" file.

---

DeepCalc is an evolving project. Features and documentation may change as development continues.
