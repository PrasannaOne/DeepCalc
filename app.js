(() => {
  "use strict";

  // ---------------------------------------------------------------------------
  // State and persistence (the web equivalent of SheetRepository/DataStore)
  // ---------------------------------------------------------------------------
  const SHEET_KEY = "soulcalc.web.sheet.v1";
  const THEME_KEY = "soulcalc.web.theme.v1";
  let inputs = [""];
  let notes = [""];
  let savedSheets = [];
  let sheetNameMode = "save";
  let renamingSheetId = null;
  let expandedNotes = new Set();
  let focusedLineIndex = 0;
  let useCustomKeyboard = true;
  let currentVariables = {};
  let toastTimer = null;
  let activeField = null;
  let isNoteFocused = false;
  const HISTORY_KEY = "deepcalc.web.saved-sheets.v1";

  const $ = id => document.getElementById(id);
  const linesElement = $("lines");

  function saveSheet() {
    try {
      localStorage.setItem("deepcalc.web.notes.v1", JSON.stringify(notes));
      localStorage.setItem(SHEET_KEY, JSON.stringify({
        lines: inputs,
        focusedLineIndex
      }));
    } catch (_) { /* Browser storage can be disabled; calculations still work. */ }
  }

  function loadSheet() {
    try { const savedNotes = JSON.parse(localStorage.getItem("deepcalc.web.notes.v1") || "[]"); notes = Array.isArray(savedNotes) ? savedNotes.map(String) : []; } catch (_) { notes = []; }
    try {
      const saved = JSON.parse(localStorage.getItem(SHEET_KEY) || "null");
      if (saved && Array.isArray(saved.lines)) {
        const savedLines = saved.lines.map(line => String(line));
        inputs = savedLines.length ? savedLines : [""];
        // Match MainViewModel.ensureTrailingBlankLine() and restoredFocusIndex().
        if (inputs.length && inputs[inputs.length - 1].trim() !== "") inputs.push("");
        const savedIndex = Number(saved.focusedLineIndex) || 0;
        focusedLineIndex = savedIndex >= savedLines.length - 1
          ? Math.max(inputs.length - 1, 0)
          : Math.max(savedIndex, 0);
        focusedLineIndex = Math.min(focusedLineIndex, inputs.length - 1);
        notes = inputs.map((_, i) => String(notes[i] || ""));
      }
    } catch (_) {
      inputs = [""];
      focusedLineIndex = 0;
    }
  }

  function loadSavedSheets() {
    try {
      const value = JSON.parse(localStorage.getItem(HISTORY_KEY) || "[]");
      savedSheets = Array.isArray(value) ? value.filter(item => item && item.id && Array.isArray(item.lines)).map(item => ({
        id: String(item.id), title: String(item.title || "Untitled sheet"),
        createdAt: String(item.createdAt || new Date().toISOString()),
        lines: item.lines.map(line => String(line)),
        notes: Array.isArray(item.notes) ? item.notes.map(note => String(note || "")) : []
      })) : [];
    } catch (_) { savedSheets = []; }
  }

  function persistSavedSheets() {
    try { localStorage.setItem(HISTORY_KEY, JSON.stringify(savedSheets)); }
    catch (_) { showToast("Storage is full. Delete an old saved sheet."); }
  }

  function suggestedSheetTitle() {
    return new Date().toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
  }

  // ---------------------------------------------------------------------------
  // Lexer, recursive-descent parser and evaluator ported from the repo's domain
  // layer. Keep expression behaviour aligned with Lexer.kt / Parser.kt /
  // Evaluator.kt / SheetEngine.kt rather than using JavaScript eval().
  // ---------------------------------------------------------------------------
  // Numeric literal precision helpers. These intentionally avoid JavaScript eval().
  function significantDigits(literal) {
    const digits = literal.replace(/\D/g, "");
    const first = [...digits].findIndex(ch => ch !== "0");
    if (first < 0) return 0;
    let last = -1;
    for (let i = digits.length - 1; i >= 0; i--) {
      if (digits[i] !== "0") { last = i; break; }
    }
    return last - first + 1;
  }

  function tokenize(source) {
    const tokens = [];
    let position = 0;

    while (position < source.length) {
      const char = source[position];
      if (/\s/.test(char)) { position++; continue; }
      const start = position;

      if (/[0-9]/.test(char) || (char === "." && /[0-9]/.test(source[position + 1] || ""))) {
        let value = "";
        let hasDecimal = false;
        while (position < source.length) {
          const current = source[position];
          if (/[0-9]/.test(current)) {
            value += current;
            position++;
          } else if (current === "." && !hasDecimal) {
            hasDecimal = true;
            value += current;
            position++;
          } else break;
        }
        tokens.push({ type: "NUMBER", value, position: start });
        continue;
      }

      if (char === "$") {
        position++;
        const next = source[position];
        if (next == null) throw new CalcError("Invalid token: $", start);
        if (/[0-9]/.test(next)) {
          let value = "";
          while (/[0-9]/.test(source[position] || "")) value += source[position++];
          tokens.push({ type: "LINE_REF", value, position: start });
        } else if (/[A-Za-z]/.test(next)) {
          let value = "";
          while (/[A-Za-z0-9_]/.test(source[position] || "")) value += source[position++];
          tokens.push({ type: "VARIABLE", value, position: start });
        } else {
          throw new CalcError("Invalid token: $", start);
        }
        continue;
      }

      const singles = {
        "+":"PLUS", "-":"MINUS", "−":"MINUS", "*":"MULTIPLY", "×":"MULTIPLY",
        "/":"DIVIDE", "÷":"DIVIDE", "%":"PERCENT", "^":"POWER", "=":"EQUALS",
        "(":"LPAREN", ")":"RPAREN"
      };
      if (singles[char]) {
        tokens.push({ type: singles[char], value: char, position: start });
        position++;
        continue;
      }

      if (/[A-Za-z]/.test(char)) {
        let value = "";
        while (/[A-Za-z0-9_]/.test(source[position] || "")) value += source[position++];
        if (value.toLowerCase() === "sqrt") {
          tokens.push({ type: "SQRT", value, position: start });
        } else {
          throw new CalcError("Invalid token: " + value, start);
        }
        continue;
      }

      throw new CalcError("Invalid token: " + char, start);
    }

    tokens.push({ type: "EOF", value: "", position: source.length });
    return tokens;
  }

  class CalcError extends Error {
    constructor(message, position = 0) {
      super(message);
      this.position = position;
    }
  }

  function parseExpression(source) {
    let tokens;
    try {
      tokens = tokenize(source);
    } catch (error) {
      return { kind: "error", message: error.message };
    }

    if (tokens.length === 1) return { kind: "empty" };
    let pos = 0;
    const current = () => tokens[pos];
    const take = type => {
      if (current().type !== type) {
        throw new CalcError("Unexpected token: " + current().value, current().position);
      }
      return tokens[pos++];
    };

    function expression() {
      if (current().type === "VARIABLE" && tokens[pos + 1]?.type === "EQUALS") {
        const name = take("VARIABLE").value;
        take("EQUALS");
        return { type: "assignment", name, expression: computation() };
      }
      return computation();
    }

    function computation() {
      let left = term();
      while (current().type === "PLUS" || current().type === "MINUS") {
        const operator = current().type;
        pos++;
        const right = term();
        const adjustedRight = right.type === "percent" && right.base == null
          ? { type: "percent", operand: right.operand, base: left }
          : right;
        left = {
          type: "binary",
          operator: operator === "PLUS" ? "ADD" : "SUBTRACT",
          left,
          right: adjustedRight
        };
      }
      return left;
    }

    function term() {
      let left = factor();
      while (current().type === "MULTIPLY" || current().type === "DIVIDE") {
        const operator = current().type === "MULTIPLY" ? "MULTIPLY" : "DIVIDE";
        pos++;
        left = { type: "binary", operator, left, right: factor() };
      }
      return left;
    }

    function factor() {
      const base = power();
      if (current().type === "PERCENT") {
        pos++;
        return { type: "percent", operand: base, base: null };
      }
      return base;
    }

    function power() {
      const base = unary();
      if (current().type === "POWER") {
        pos++;
        return { type: "binary", operator: "POWER", left: base, right: power() };
      }
      return base;
    }

    function unary() {
      if (current().type === "MINUS") {
        pos++;
        return { type: "unaryMinus", operand: unary() };
      }
      return primary();
    }

    function primary() {
      const token = current();
      switch (token.type) {
        case "NUMBER":
          pos++;
          if (significantDigits(token.value) > 15) {
            throw new CalcError("Too many significant digits: " + token.value, token.position);
          }
          return { type: "number", value: Number(token.value) };
        case "VARIABLE":
          pos++;
          return { type: "variable", name: token.value };
        case "LINE_REF": {
          pos++;
          const lineNumber = Number(token.value);
          if (!Number.isInteger(lineNumber) || lineNumber < 0 || lineNumber > 2147483647) {
            throw new CalcError("Invalid line reference: $" + token.value, token.position);
          }
          return { type: "lineRef", lineNumber };
        }
        case "LPAREN": {
          pos++;
          const node = computation();
          if (current().type !== "RPAREN") {
            throw new CalcError("Expected ')'", current().position);
          }
          pos++;
          return node;
        }
        case "SQRT": {
          pos++;
          if (current().type !== "LPAREN") {
            throw new CalcError("Expected '(' after sqrt", current().position);
          }
          pos++;
          const argument = computation();
          if (current().type !== "RPAREN") {
            throw new CalcError("Expected ')'", current().position);
          }
          pos++;
          return { type: "function", name: "sqrt", argument };
        }
        case "EOF":
          throw new CalcError("Unexpected end of input", token.position);
        default:
          throw new CalcError("Unexpected token: " + token.value, token.position);
      }
    }

    try {
      const node = expression();
      if (current().type !== "EOF") {
        throw new CalcError("Unexpected token: " + current().value, current().position);
      }
      return { kind: "success", node };
    } catch (error) {
      return { kind: "error", message: error.message || "Parse error" };
    }
  }

  function evaluateNode(node, scope) {
    switch (node.type) {
      case "number": return node.value;
      case "binary": {
        const left = evaluateNode(node.left, scope);
        const right = evaluateNode(node.right, scope);
        switch (node.operator) {
          case "ADD": return left + right;
          case "SUBTRACT": return left - right;
          case "MULTIPLY": return left * right;
          case "DIVIDE":
            if (right === 0) {
              if (left === 0) throw new CalcError("NaN");
              throw new CalcError(left < 0 ? "-∞" : "∞");
            }
            return left / right;
          case "POWER": return Math.pow(left, right);
          default: throw new CalcError("Evaluation error");
        }
      }
      case "unaryMinus": return -evaluateNode(node.operand, scope);
      case "percent": {
        const percentage = evaluateNode(node.operand, scope) / 100;
        return node.base != null ? evaluateNode(node.base, scope) * percentage : percentage;
      }
      case "variable":
        if (Object.prototype.hasOwnProperty.call(scope.variables, node.name)) {
          return scope.variables[node.name];
        }
        throw new CalcError("? $" + node.name);
      case "lineRef":
        if (Object.prototype.hasOwnProperty.call(scope.lineResults, node.lineNumber)) {
          return scope.lineResults[node.lineNumber];
        }
        throw new CalcError("? $" + node.lineNumber);
      case "assignment": {
        const value = evaluateNode(node.expression, scope);
        scope.variables[node.name] = value;
        return value;
      }
      case "function": {
        const argument = evaluateNode(node.argument, scope);
        if (node.name.toLowerCase() === "sqrt") {
          if (argument < 0) throw new CalcError("NaN");
          return Math.sqrt(argument);
        }
        throw new CalcError("Unknown function: " + node.name);
      }
      default: throw new CalcError("Evaluation error");
    }
  }

  function classifyLine(input) {
    const trimmed = input.trim();
    if (!trimmed) return "EMPTY";
    if (trimmed.startsWith("#")) return "COMMENT";
    return "EXPRESSION";
  }

  function evaluateSheet(sourceLines) {
    const scope = { variables: Object.create(null), lineResults: Object.create(null) };
    const results = [];
    sourceLines.forEach((input, index) => {
      const type = classifyLine(input);
      if (type !== "EXPRESSION") {
        results.push({ kind: "empty", text: "" });
        return;
      }
      const parsed = parseExpression(input);
      if (parsed.kind === "empty") {
        results.push({ kind: "empty", text: "" });
        return;
      }
      if (parsed.kind === "error") {
        results.push({ kind: "error", text: formatError(parsed.message) });
        return;
      }
      try {
        const value = evaluateNode(parsed.node, scope);
        const text = formatNumber(value);
        // SheetEngine stores every successful result for later $N references.
        scope.lineResults[index + 1] = value;
        results.push({ kind: "success", value, text });
      } catch (error) {
        results.push({ kind: "error", text: formatError(error.message || "Evaluation error") });
      }
    });
    currentVariables = { ...scope.variables };
    return results;
  }

  function formatNumber(value) {
    if (Number.isNaN(value)) return "NaN";
    if (!Number.isFinite(value)) return value > 0 ? "∞" : "-∞";
    if (Number.isInteger(value) && Math.abs(value) < 1e15) return String(value);
    let formatted = value.toPrecision(10);
    if (formatted.includes("e")) {
      const parts = formatted.split("e");
      parts[0] = parts[0].replace(/(\.\d*?)0+$/, "$1").replace(/\.$/, "");
      return parts[0] + "e" + parts[1].replace(/^([+-])0+(\d)/, "$1$2");
    }
    return formatted.replace(/(\.\d*?)0+$/, "$1").replace(/\.$/, "");
  }

  function formatError(message) {
    if (message === "∞" || message === "-∞" || message === "NaN") return message;
    if (message.toLowerCase().includes("too many significant digits")) return "? digits";
    if (message.toLowerCase().includes("undefined variable")) {
      return "? " + (message.match(/\$\w+/) || [""])[0];
    }
    if (message.toLowerCase().includes("invalid line reference")) {
      return "? " + (message.match(/\$\d+/) || [""])[0];
    }
    if (message.startsWith("?")) return message;
    return "?";
  }

  // Render complete result strings. CSS allows long values to wrap instead of truncating.
  function updateResultCells(results) {
    const rows = linesElement.querySelectorAll(".line-row");
    results.forEach((result, index) => {
      const cell = rows[index]?.querySelector(".result");
      if (!cell) return;
      cell.textContent = result.text;
      // Keep the full value available to assistive technology and long-press users.
      cell.title = result.kind === "success" ? result.text : "";
      cell.dataset.copy = result.kind === "success" ? result.text : "";
      cell.classList.toggle("error", result.kind === "error");
      cell.classList.toggle("empty", result.kind === "empty");
      cell.disabled = result.kind !== "success";
      cell.setAttribute("aria-label", result.kind === "success" ? "Copy result " + result.text : result.text);
    });
    rows.forEach((row, index) => {
      const field = row.querySelector(".expression");
      field.classList.toggle("comment", classifyLine(inputs[index]) === "COMMENT");
    });
  }

  function recalculate() {
    const results = evaluateSheet(inputs);
    updateResultCells(results);
    return results;
  }

  // ---------------------------------------------------------------------------
  // UI rendering and line editing (mirrors LineRow + SheetEngine)
  // ---------------------------------------------------------------------------
  function createLineRow(input, index, result) {
    const row = document.createElement("div");
    row.className = "line-row";

    const number = document.createElement("div");
    number.className = "line-number";
    number.textContent = String(index + 1);

    const inputBox = document.createElement("div");
    inputBox.className = "input-box";

    const field = document.createElement("textarea");
    field.className = "expression";
    field.rows = 1;
    field.value = input;
    field.autocomplete = "off";
    field.autocapitalize = "off";
    field.spellcheck = false;
    field.setAttribute("aria-label", "Line " + (index + 1));
    field.setAttribute("enterkeyhint", "next");
    field.setAttribute("inputmode", useCustomKeyboard ? "none" : "text");
    field.dataset.lineIndex = String(index);
    field.addEventListener("focus", () => {
      isNoteFocused = false;
      syncKeyboardMode();
      focusedLineIndex = index;
      activeField = field;
      saveSheet();
    });
    field.addEventListener("input", () => {
      inputs[index] = field.value;
      resizeField(field);
      recalculate();
      saveSheet();
    });
    field.addEventListener("keydown", event => {
      if (event.key === "Enter") {
        event.preventDefault();
        insertNewLine(index + 1);
      } else if (event.key === "Backspace" && field.value.length === 0 && inputs.length > 1) {
        event.preventDefault();
        deleteLine(index);
      }
    });
    field.addEventListener("pointerdown", () => {
      focusedLineIndex = index;
      activeField = field;
    });
    inputBox.append(field);
    const copyDot = document.createElement("button");
    copyDot.type = "button";
    copyDot.className = "line-copy-dot";
    copyDot.setAttribute("aria-label", "Copy expression");
    copyDot.title = "Copy expression";
    copyDot.addEventListener("click", () => {
      if (field.value.trim()) copyText(field.value, "Copied: " + field.value.trim());
    });
    inputBox.append(copyDot);

    const noteToggle = document.createElement("button");
    noteToggle.type = "button";
    noteToggle.className = "note-toggle";
    noteToggle.textContent = expandedNotes.has(index) ? "⌄" : "⌃";
    noteToggle.setAttribute("aria-label", expandedNotes.has(index) ? "Hide note" : "Add or edit note");
    noteToggle.setAttribute("aria-expanded", expandedNotes.has(index) ? "true" : "false");
    noteToggle.addEventListener("click", () => toggleNote(index));

    const resultBox = document.createElement("div");
    resultBox.className = "result-box";
    const resultButton = document.createElement("button");
    resultButton.type = "button";
    resultButton.className = "result";
    resultButton.textContent = result.text;
    resultButton.title = result.kind === "success" ? result.text : "";
    resultButton.dataset.copy = result.kind === "success" ? result.text : "";
    resultButton.disabled = result.kind !== "success";
    resultButton.addEventListener("click", () => {
      if (resultButton.dataset.copy) copyText(resultButton.dataset.copy, "Copied: " + resultButton.dataset.copy);
    });
    resultBox.append(resultButton);

    row.append(number, inputBox, noteToggle, resultBox);
    resizeField(field);
    return row;
  }

  let activeNoteField = null;
  function toggleNote(index) {
    if (expandedNotes.has(index)) {
      expandedNotes.delete(index);
      isNoteFocused = false;
      if (activeNoteField && document.contains(activeNoteField)) activeNoteField.blur();
      activeNoteField = null;
      syncKeyboardMode();
      renderRows();
      return;
    }
    expandedNotes.add(index);
    renderRows();
    requestAnimationFrame(() => {
      const editors = [...linesElement.querySelectorAll(".note-editor")];
      const editor = editors.find(item => Number(item.dataset.lineIndex) === index);
      if (editor) {
        activeNoteField = editor;
        isNoteFocused = true;
        syncKeyboardMode();
        editor.focus();
        editor.setSelectionRange(editor.value.length, editor.value.length);
      }
    });
  }

  function resizeField(field) {
    field.style.height = "auto";
    field.style.height = Math.max(24, field.scrollHeight) + "px";
  }

  function renderRows(options = {}) {
    const results = evaluateSheet(inputs);
    linesElement.innerHTML = "";
    inputs.forEach((input, index) => {
      if (expandedNotes.has(index)) {
        const noteRow = document.createElement("div");
        noteRow.className = "note-row";
        const noteContent = document.createElement("div");
        noteContent.className = "note-content";
        const editor = document.createElement("textarea");
        editor.className = "note-editor";
        editor.rows = 1;
        editor.placeholder = "For notes…";
        editor.value = notes[index] || "";
        editor.dataset.lineIndex = String(index);
        editor.setAttribute("inputmode", "text");
        editor.setAttribute("enterkeyhint", "done");
        editor.setAttribute("aria-label", "Note for calculation " + (index + 1));
        editor.addEventListener("input", () => {
          notes[index] = editor.value;
          editor.style.height = "20px";
          editor.style.height = Math.min(72, Math.max(20, editor.scrollHeight)) + "px";
          saveSheet();
        });
        editor.addEventListener("focus", () => { activeNoteField = editor; isNoteFocused = true; syncKeyboardMode(); });
        editor.addEventListener("blur", () => { isNoteFocused = false; syncKeyboardMode(); });
        noteContent.append(editor); noteRow.append(noteContent); linesElement.append(noteRow);
        requestAnimationFrame(() => { editor.style.height = "20px"; editor.style.height = Math.min(72, Math.max(20, editor.scrollHeight)) + "px"; });
      }
      linesElement.append(createLineRow(input, index, results[index]));
    });
    if (options.focus) {
      requestAnimationFrame(() => {
        const field = linesElement.querySelectorAll(".expression")[focusedLineIndex];
        if (!field) return;
        activeField = field;
        field.setAttribute("inputmode", useCustomKeyboard ? "none" : "text");
        field.focus({ preventScroll: true });
        const position = options.cursorPosition == null ? field.value.length : options.cursorPosition;
        field.setSelectionRange(position, position);
        field.scrollIntoView({ block: "nearest" });
        if (!useCustomKeyboard) field.focus();
      });
    }
    saveSheet();
  }

  function getActiveField() {
    const fields = linesElement.querySelectorAll(".expression");
    let field = activeField && document.contains(activeField) ? activeField : fields[focusedLineIndex];
    if (!field && fields.length) field = fields[0];
    if (field) {
      activeField = field;
      focusedLineIndex = Number(field.dataset.lineIndex);
    }
    return field || null;
  }

  function insertTextAtCursor(text) {
    const field = getActiveField();
    if (!field) return;
    const start = field.selectionStart == null ? field.value.length : field.selectionStart;
    const end = field.selectionEnd == null ? start : field.selectionEnd;
    const updated = field.value.slice(0, start) + text + field.value.slice(end);
    inputs[focusedLineIndex] = updated;
    field.value = updated;
    resizeField(field);
    recalculate();
    field.focus({ preventScroll: true });
    field.setSelectionRange(start + text.length, start + text.length);
    activeField = field;
    saveSheet();
  }

  async function pasteFromClipboard() {
    try {
      if (!navigator.clipboard || !window.isSecureContext) throw new Error("Clipboard unavailable");
      const value = await navigator.clipboard.readText();
      if (value) insertTextAtCursor(value);
      else showToast("Clipboard is empty");
    } catch (_) { showToast("Unable to paste"); }
  }

  function deleteAtCursor() {
    const field = getActiveField();
    if (!field) return;
    const text = field.value;
    const start = field.selectionStart == null ? text.length : field.selectionStart;
    const end = field.selectionEnd == null ? start : field.selectionEnd;
    let updated = text;
    let cursor = start;

    if (start !== end) {
      updated = text.slice(0, start) + text.slice(end);
    } else if (start > 0) {
      const before = text.slice(0, start);
      const count = before.endsWith("sqrt(") ? 5 : 1;
      updated = text.slice(0, start - count) + text.slice(start);
      cursor = start - count;
    } else if (text.length === 0) {
      deleteLine(focusedLineIndex);
      return;
    } else {
      return;
    }

    inputs[focusedLineIndex] = updated;
    field.value = updated;
    resizeField(field);
    recalculate();
    field.focus({ preventScroll: true });
    field.setSelectionRange(cursor, cursor);
    saveSheet();
  }

  function clearCurrentLine() {
    const field = getActiveField();
    if (!field) return;
    inputs[focusedLineIndex] = "";
    field.value = "";
    resizeField(field);
    recalculate();
    field.focus({ preventScroll: true });
    field.setSelectionRange(0, 0);
    saveSheet();
  }

  // The reference-shifting rules are copied from SheetEngine.kt:
  // insertion shifts $N references at/after the inserted line; removal shifts
  // references strictly after the removed line. References to the removed line
  // remain unchanged and therefore become unresolved/self-references as in app.
  function shiftReferencesForInsert(line, insertedLineNumber) {
    return line.replace(/\$(\d+)/g, (match, digits) => {
      const number = Number(digits);
      return Number.isSafeInteger(number) && number >= insertedLineNumber
        ? "$" + (number + 1)
        : match;
    });
  }

  function shiftReferencesForRemove(line, removedLineNumber) {
    return line.replace(/\$(\d+)/g, (match, digits) => {
      const number = Number(digits);
      return Number.isSafeInteger(number) && number > removedLineNumber
        ? "$" + (number - 1)
        : match;
    });
  }

  function insertNewLine(index) {
    const safeIndex = Math.max(0, Math.min(index, inputs.length));
    const insertedLineNumber = safeIndex + 1;
    inputs = inputs.map(line => shiftReferencesForInsert(line, insertedLineNumber));
    inputs.splice(safeIndex, 0, "");
    notes.splice(safeIndex, 0, "");
    expandedNotes = new Set([...expandedNotes].map(i => i >= safeIndex ? i + 1 : i));
    focusedLineIndex = safeIndex;
    useCustomKeyboard = true;
    syncKeyboardMode();
    renderRows({ focus: true, cursorPosition: 0 });
  }

  function deleteLine(index) {
    if (index < 0 || index >= inputs.length) return;
    if (inputs.length <= 1) {
      inputs[0] = "";
      notes[0] = "";
      focusedLineIndex = 0;
      renderRows({ focus: true, cursorPosition: 0 });
      return;
    }
    const removedLineNumber = index + 1;
    inputs = inputs
      .filter((_, lineIndex) => lineIndex !== index)
      .map(line => shiftReferencesForRemove(line, removedLineNumber));
    notes.splice(index, 1);
    expandedNotes = new Set([...expandedNotes].filter(i => i !== index).map(i => i > index ? i - 1 : i));
    focusedLineIndex = Math.max(index - 1, 0);
    renderRows({ focus: true });
  }

  // ---------------------------------------------------------------------------
  // Keyboard behaviour matches CalculatorKeyboard.kt (400 ms long press).
  // ---------------------------------------------------------------------------
  function setKeyboardMode(custom) {
    useCustomKeyboard = Boolean(custom);
    syncKeyboardMode();
    const field = getActiveField();
    if (field) {
      field.setAttribute("inputmode", useCustomKeyboard ? "none" : "text");
      if (useCustomKeyboard) {
        field.blur();
        requestAnimationFrame(() => field.focus({ preventScroll: true }));
      } else {
        requestAnimationFrame(() => field.focus());
      }
    }
    saveSheet();
  }

  function syncKeyboardMode() {
    $("keyboardPanel").style.display = useCustomKeyboard && !isNoteFocused ? "" : "none";
    $("keyboardIcon").innerHTML = useCustomKeyboard
      ? '<path d="M20 5H4c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zM8 8h2v2H8V8zm-3 0h2v2H5V8zm0 3h2v2H5v-2zm3 0h2v2H8v-2zm3-3h2v2h-2V8zm0 3h2v2h-2v-2zm3-3h2v2h-2V8zm0 3h2v2h-2v-2zm3-3h2v2h-2V8zm0 3h2v2h-2v-2zm-11 5v-1h10v1H6z"/>'
      : '<path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM8 7h2v2H8V7zm-3 0h2v2H5V7zm0 3h2v2H5v-2zm3 0h2v2H8v-2zm3-3h2v2h-2V7zm0 3h2v2h-2v-2zm3-3h2v2h-2V7zm3 0h2v2h-2V7zm-6 9H5v-2h6v2zm7 0h-5v-2h5v2zm-1-4h-2v-2h2v2zm-3 0h-2v-2h2v2z"/>';

    $("keyboardToggle").setAttribute(
      "aria-label",
      useCustomKeyboard ? "Switch to system keyboard" : "Switch to calculator keyboard"
    );
  }

  function handleKeyTap(key, button) {
    switch (key) {
      case "insert":
        if (button.dataset.value === "()") {
          const field = getActiveField();
          const at = field && field.selectionStart != null ? field.selectionStart : (field ? field.value.length : 0);
          insertTextAtCursor("()");
          const current = getActiveField();
          current?.setSelectionRange(at + 1, at + 1);
        } else insertTextAtCursor(button.dataset.value);
        break;
      case "paste": pasteFromClipboard(); break;
      case "power": insertTextAtCursor("^"); break;
      case "hash":
        insertTextAtCursor("#");
        setKeyboardMode(false);
        break;
      case "dollar":
        insertTextAtCursor("$");
        setKeyboardMode(false);
        break;
      case "backspace": deleteAtCursor(); break;
      case "enter": insertNewLine(focusedLineIndex + 1); break;
    }
  }

  function handleLongPress(key, button) {
    switch (key) {
      case "power": insertTextAtCursor("sqrt("); break;
      case "backspace": clearCurrentLine(); break;
      case "dollar": showVariablePicker(); break;
    }
  }

  document.querySelectorAll(".key").forEach(button => {
    const key = button.dataset.key;
    let timer = null;
    let longPressed = false;
    const hasLongPress = ["power", "backspace", "dollar"].includes(key);

    button.addEventListener("pointerdown", event => {
      event.preventDefault();
      longPressed = false;
      button.setPointerCapture?.(event.pointerId);
      if (hasLongPress) {
        timer = window.setTimeout(() => {
          longPressed = true;
          button.classList.add("alt");
          if (key === "power") button.textContent = "√";
        }, 400);
      }
    });
    const release = event => {
      if (timer) { clearTimeout(timer); timer = null; }
      if (longPressed) {
        button.classList.remove("alt");
        if (key === "power") button.textContent = "^";
        handleLongPress(key, button);
      } else {
        handleKeyTap(key, button);
      }
      event.preventDefault();
    };
    button.addEventListener("pointerup", release);
    button.addEventListener("pointercancel", () => {
      if (timer) clearTimeout(timer);
      timer = null;
      longPressed = false;
      button.classList.remove("alt");
      if (key === "power") button.textContent = "^";
    });
  });

  // ---------------------------------------------------------------------------
  // Menu, dialogs, clipboard and theme preferences (DataStore equivalent).
  // ---------------------------------------------------------------------------
  function showToast(message) {
    const toast = $("toast");
    toast.textContent = message;
    toast.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.remove("show"), 1800);
  }

  async function copyText(text, message) {
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
      } else {
        const temporary = document.createElement("textarea");
        temporary.value = text;
        temporary.style.position = "fixed";
        temporary.style.opacity = "0";
        document.body.append(temporary);
        temporary.select();
        document.execCommand("copy");
        temporary.remove();
      }
      if (message) showToast(message);
    } catch (_) {
      showToast("Unable to copy");
    }
  }

  function formatAllLines() {
    const results = evaluateSheet(inputs);
    return inputs.map((line, index) => {
      if (classifyLine(line) === "EMPTY") return null;
      if (classifyLine(line) === "COMMENT") return line;
      const result = results[index].text;
      return result ? line + " = " + result : line;
    }).filter(line => line !== null).join("\n");
  }

  function formatAllWithNotes() {
    const results = evaluateSheet(inputs);
    return inputs.map((line, index) => {
      const parts = [];
      if (classifyLine(line) !== "EMPTY") parts.push(classifyLine(line) === "COMMENT" ? line : (results[index].text ? line + " = " + results[index].text : line));
      if ((notes[index] || "").trim()) parts.push("  Note: " + notes[index].trim());
      return parts.join("\n");
    }).filter(Boolean).join("\n");
  }

  // Share copy stays short; Open Graph metadata supplies the rich link preview.
  async function shareDeepCalc() {
    // Keep the share message complementary to the link preview. The preview carries
    // the product name, tagline and artwork, so the message only explains the app.
    const payload = {
      title: "DeepCalc",
      text: "A notepad calculator for thoughts, notes and calculations.",
      url: "https://prasannaone.github.io/DeepCalc/"
    };
    try {
      if (navigator.share) await navigator.share(payload);
      else await copyText(payload.text + "\n" + payload.url, "Share text copied");
    } catch (error) { if (error && error.name !== "AbortError") showToast("Sharing unavailable"); }
  }

  // Explicit History records are separate from the automatically saved active sheet.
  function saveCurrentSheet(title) {
    const cleanedTitle = String(title || "").trim() || suggestedSheetTitle();
    const record = {
      id: "sheet-" + Date.now() + "-" + Math.random().toString(36).slice(2, 8),
      title: cleanedTitle,
      createdAt: new Date().toISOString(),
      lines: inputs.slice(),
      notes: inputs.map((_, index) => String(notes[index] || ""))
    };
    savedSheets.unshift(record);
    if (savedSheets.length > 100) savedSheets = savedSheets.slice(0, 100);
    persistSavedSheets();
    showToast("Sheet saved to History");
  }

  function openSaveSheetDialog() {
    sheetNameMode = "save";
    renamingSheetId = null;
    $("sheetNameTitle").textContent = "Save Sheet";
    $("sheetNameSave").textContent = "Save";
    $("sheetNameInput").value = suggestedSheetTitle();
    openDialog("sheetNameDialog");
    requestAnimationFrame(() => { $("sheetNameInput").focus(); $("sheetNameInput").select(); });
  }

  function openRenameSheetDialog(id) {
    const item = savedSheets.find(sheet => sheet.id === id);
    if (!item) return;
    sheetNameMode = "rename";
    renamingSheetId = id;
    $("sheetNameTitle").textContent = "Rename Sheet";
    $("sheetNameSave").textContent = "Save";
    $("sheetNameInput").value = item.title;
    openDialog("sheetNameDialog");
    requestAnimationFrame(() => { $("sheetNameInput").focus(); $("sheetNameInput").select(); });
  }

  function renderHistory() {
    const list = $("historyList");
    list.innerHTML = "";
    if (!savedSheets.length) {
      const empty = document.createElement("div");
      empty.className = "history-empty";
      empty.textContent = "No saved sheets yet. Save a sheet to keep it here.";
      list.append(empty);
      return;
    }
    savedSheets.forEach(item => {
      const card = document.createElement("article");
      card.className = "history-item";
      const info = document.createElement("div");
      info.className = "history-info";
      const title = document.createElement("div");
      title.className = "history-item-title";
      title.textContent = item.title;
      const meta = document.createElement("div");
      meta.className = "history-item-meta";
      const date = new Date(item.createdAt);
      const noteCount = (item.notes || []).filter(note => String(note || "").trim()).length;
      meta.textContent = (Number.isNaN(date.getTime()) ? "Saved sheet" : date.toLocaleString()) + " · " + item.lines.length + " lines · " + noteCount + " notes";
      const preview = document.createElement("div");
      preview.className = "history-item-preview";
      preview.textContent = item.lines.find(line => line.trim()) || "Empty sheet";
      info.append(title, meta, preview);
      const actions = document.createElement("div");
      actions.className = "history-actions";
      const makeAction = (label, cls, handler) => {
        const button = document.createElement("button");
        button.className = "history-action" + (cls ? " " + cls : "");
        button.type = "button";
        button.textContent = label;
        button.addEventListener("click", handler);
        return button;
      };
      actions.append(
        makeAction("Open", "primary", () => openSavedSheet(item.id)),
        makeAction("Rename", "", () => openRenameSheetDialog(item.id)),
        makeAction("Delete", "delete", () => {
          if (!window.confirm("Delete “" + item.title + "” from History?")) return;
          savedSheets = savedSheets.filter(sheet => sheet.id !== item.id);
          persistSavedSheets();
          renderHistory();
        })
      );
      card.append(info, actions);
      list.append(card);
    });
  }

  function openSavedSheet(id) {
    const item = savedSheets.find(sheet => sheet.id === id);
    if (!item) return;
    inputs = item.lines.length ? item.lines.slice() : [""];
    notes = inputs.map((_, index) => String(item.notes[index] || ""));
    if (inputs[inputs.length - 1].trim() !== "") { inputs.push(""); notes.push(""); }
    expandedNotes.clear();
    focusedLineIndex = Math.max(0, inputs.length - 1);
    activeField = null;
    activeNoteField = null;
    isNoteFocused = false;
    useCustomKeyboard = true;
    closeDialogs();
    syncKeyboardMode();
    renderRows({ focus: true, cursorPosition: 0 });
    showToast("Opened “" + item.title + "”");
  }

  function openDialog(id) {
    $("menu").classList.remove("open");
    document.querySelectorAll(".dialog-scrim").forEach(dialog => dialog.classList.remove("open"));
    $(id).classList.add("open");
  }

  function closeDialogs() {
    document.querySelectorAll(".dialog-scrim").forEach(dialog => dialog.classList.remove("open"));
  }

  function showVariablePicker() {
    const names = Object.keys(currentVariables).sort();
    $("variableEmpty").hidden = names.length > 0;
    $("variablePickerContent").hidden = names.length === 0;
    const list = $("variableList");
    list.innerHTML = "";
    names.forEach(name => {
      const chip = document.createElement("button");
      chip.className = "variable-chip";
      chip.type = "button";
      chip.textContent = "$" + name;
      chip.addEventListener("click", () => {
        closeDialogs();
        insertTextAtCursor("$" + name);
      });
      list.append(chip);
    });
    openDialog("variableDialog");
  }

  function getStoredTheme() {
    try { return localStorage.getItem(THEME_KEY) || "system"; }
    catch (_) { return "system"; }
  }

  let themeMode = getStoredTheme();
  const ACCENT_KEY = "deepcalc.web.accent.v1";
  let accentMode = "purple";
  try { accentMode = localStorage.getItem(ACCENT_KEY) || "purple"; } catch (_) {}
  function applyAccent(mode) {
    accentMode = mode;
    try { localStorage.setItem(ACCENT_KEY, mode); } catch (_) {}
    document.documentElement.dataset.accent = mode;
    document.querySelectorAll('input[name="accent"]').forEach(radio => { radio.checked = radio.value === mode; });
  }

  function applyTheme(mode) {
    themeMode = mode;
    try { localStorage.setItem(THEME_KEY, mode); } catch (_) {}
    const dark = mode === "dark" || mode === "amoled" ||
      (mode === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches);
    document.documentElement.dataset.theme = mode === "amoled" ? "amoled" : (dark ? "dark" : "light");
    const themeMeta = document.getElementById("metaThemeColor");
    if (themeMeta) themeMeta.content = mode === "amoled" ? "#000000" : (dark ? "#111318" : "#faf9ff");
    document.querySelectorAll('input[name="theme"]').forEach(radio => {
      radio.checked = radio.value === mode;
    });
  }

  window.matchMedia("(prefers-color-scheme: dark)").addEventListener?.("change", () => {
    if (themeMode === "system") applyTheme("system");
  });

  $("keyboardToggle").addEventListener("click", () => setKeyboardMode(!useCustomKeyboard));
  $("menuToggle").addEventListener("click", event => {
    event.stopPropagation();
    $("menu").classList.toggle("open");
  });

  $("menu").addEventListener("click", event => {
    const button = event.target.closest("[data-menu]");
    if (!button) return;
    switch (button.dataset.menu) {
      case "copy": {
        const text = formatAllLines();
        if (text.length) copyText(text, "Sheet copied");
        $("menu").classList.remove("open");
        break;
      }
      case "copy-complete": {
        const text = formatAllWithNotes();
        if (text.length) copyText(text, "Complete sheet copied");
        $("menu").classList.remove("open");
        break;
      }
      case "save":
        $("menu").classList.remove("open");
        openSaveSheetDialog();
        break;
      case "history":
        $("menu").classList.remove("open");
        renderHistory();
        openDialog("historyDialog");
        break;
      case "share":
        $("menu").classList.remove("open");
        shareDeepCalc();
        break;
      case "clear":
        inputs = [""];
        notes = [""];
        expandedNotes.clear();
        focusedLineIndex = 0;
        activeField = null;
        activeNoteField = null;
        isNoteFocused = false;
        useCustomKeyboard = true;
        syncKeyboardMode();
        renderRows({ focus: true, cursorPosition: 0 });
        $("menu").classList.remove("open");
        break;
      case "guide": openDialog("guideDialog"); break;
      case "settings": openDialog("settingsDialog"); break;
      case "about": openDialog("aboutDialog"); break;
    }
  });

  $("sheetNameSave").addEventListener("click", () => {
    const title = $("sheetNameInput").value.trim();
    if (!title) { $("sheetNameInput").focus(); return; }
    if (sheetNameMode === "rename" && renamingSheetId) {
      const item = savedSheets.find(sheet => sheet.id === renamingSheetId);
      if (item) item.title = title;
      persistSavedSheets();
      closeDialogs();
      renderHistory();
      openDialog("historyDialog");
      showToast("Sheet renamed");
    } else {
      saveCurrentSheet(title);
      closeDialogs();
    }
  });
  $("sheetNameInput").addEventListener("keydown", event => {
    if (event.key === "Enter") { event.preventDefault(); $("sheetNameSave").click(); }
  });
  document.querySelectorAll("[data-close]").forEach(button => {
    button.addEventListener("click", closeDialogs);
  });
  document.querySelectorAll(".dialog-scrim").forEach(scrim => {
    scrim.addEventListener("click", event => {
      if (event.target === scrim) closeDialogs();
    });
  });
  document.querySelectorAll('input[name="theme"]').forEach(radio => {
    radio.addEventListener("change", () => applyTheme(radio.value));
  });
  document.querySelectorAll('input[name="accent"]').forEach(radio => {
    radio.addEventListener("change", () => applyAccent(radio.value));
  });

  document.addEventListener("click", event => {
    if (!event.target.closest("#menu") && !event.target.closest("#menuToggle")) {
      $("menu").classList.remove("open");
    }
  });
  document.addEventListener("keydown", event => {
    if (event.key === "Escape") {
      closeDialogs();
      $("menu").classList.remove("open");
    }
  });

  // ---------------------------------------------------------------------------
  // Start with the restored sheet and system theme.
  // ---------------------------------------------------------------------------
  loadSheet();
  loadSavedSheets();
  applyTheme(themeMode);
  applyAccent(accentMode);
  renderRows();
  syncKeyboardMode();
})();


// ---------------------------------------------------------------------------
// Progressive Web App lifecycle
// - Check for new deployments on launch, when the app returns to the foreground,
//   and periodically while it remains open.
// - New service workers activate immediately; the controller change reloads once.
// - The service worker keeps the app usable offline with a network-first strategy.
// ---------------------------------------------------------------------------
(() => {
  if (!("serviceWorker" in navigator)) return;
  if (!window.isSecureContext) return;

  let reloadingForUpdate = false;
  navigator.serviceWorker.addEventListener("controllerchange", () => {
    if (reloadingForUpdate) return;
    reloadingForUpdate = true;
    location.reload();
  });

  async function registerAndCheckForUpdates() {
    try {
      const registration = await navigator.serviceWorker.register("./service-worker.js", {
        scope: "./",
        updateViaCache: "none"
      });
      registration.update().catch(() => {});
      document.addEventListener("visibilitychange", () => {
        if (document.visibilityState === "visible") registration.update().catch(() => {});
      });
      window.addEventListener("pageshow", () => {
        if (document.visibilityState === "visible") registration.update().catch(() => {});
      });
      window.setInterval(() => registration.update().catch(() => {}), 15 * 60 * 1000);
    } catch (error) {
      console.warn("DeepCalc could not check for PWA updates:", error);
    }
  }

  if (document.readyState === "complete") registerAndCheckForUpdates();
  else window.addEventListener("load", registerAndCheckForUpdates, { once: true });
})();
