package com.prasannaone.deepcalc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat

import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Compose owns the system-bar and IME insets; resize the viewport when
        // Android's keyboard opens so the scrollable sheet can reveal its editor.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent { DeepCalcApp() }
    }
}

@Composable
private fun DeepCalcApp() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val initial = remember { LocalStore.load(context) }
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    // Read the IME inset during composition. It is observable Compose state,
    // so the keyboard-routing effect can react when the system IME closes.
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val configuration = LocalConfiguration.current
    val shortScreen = configuration.screenHeightDp <= 700
    val compactWidth = configuration.screenWidthDp <= 420
    val topBarHeight = when {
        configuration.screenHeightDp <= 560 -> 48.dp
        shortScreen -> 54.dp
        compactWidth -> 58.dp
        else -> 62.dp
    }

    var lineFields by remember { mutableStateOf(initial.lines.map { TextFieldValue(it) }) }
    var notes by remember { mutableStateOf(initial.notes) }
    // Row IDs never depend on a row's current position. This prevents Compose from
    // reusing a deleted row's FocusRequester/scroll state for its next neighbour.
    var rowIds by remember { mutableStateOf(initial.lines.indices.map { it.toLong() + 1L }) }
    var nextRowId by remember { mutableLongStateOf(initial.lines.size.toLong() + 1L) }
    fun allocateRowId(): Long {
        val id = nextRowId
        nextRowId += 1L
        return id
    }
    var savedSheets by remember { mutableStateOf(initial.savedSheets) }
    var focusedLine by remember { mutableIntStateOf(initial.focus) }
    var activeEditor by remember { mutableStateOf<ActiveEditor>(ActiveEditor.Calculation(initial.focus)) }
    var calculationKeyboardMode by remember { mutableStateOf(CalculationKeyboardMode.DEEPCALC) }
    var themeMode by remember { mutableStateOf(initial.theme) }
    var accentMode by remember { mutableStateOf(initial.accent) }
    var expandedNotes by remember { mutableStateOf(setOf<Int>()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var sheetName by remember { mutableStateOf("") }
    var renameId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<SavedSheet?>(null) }
    var focusEpoch by remember { mutableIntStateOf(0) }
    val sheetNameRequester = remember { FocusRequester() }

    val keyboardRoute = remember(activeEditor, calculationKeyboardMode, activeDialog) {
        resolveKeyboardRoute(activeEditor, calculationKeyboardMode, activeDialog)
    }
    // The custom keypad waits for the Android IME to finish closing before it
    // enters the layout. This prevents even a transition frame with both shown.
    var calculatorKeyboardVisible by remember { mutableStateOf(true) }
    val showCalculatorKeyboard = shouldShowCalculatorKeyboard(
        keyboardRoute,
        imeVisible,
        calculatorKeyboardVisible
    )
    val noteFocused = activeEditor is ActiveEditor.Note

    fun activateCalculation(index: Int) {
        val safeIndex = index.coerceIn(0, lineFields.lastIndex)
        calculationKeyboardMode = calculationModeAfterFocusChange(activeEditor, calculationKeyboardMode)
        activeEditor = ActiveEditor.Calculation(safeIndex)
        focusedLine = safeIndex
    }

    val currentLines = lineFields.map { it.text }
    val (results, variables) = remember(currentLines) { CalculatorEngine.evaluateSheet(currentLines) }
    val colors = palette(themeMode, accentMode, systemDark)
    val isDarkAppearance = themeMode == "dark" || themeMode == "amoled" || (themeMode == "system" && systemDark)
    SideEffect {
        activity.window.navigationBarColor = colors.surface.toArgb()
        activity.window.statusBarColor = colors.surface.toArgb()
        activity.window.decorView.setBackgroundColor(colors.surface.toArgb())
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightNavigationBars = !isDarkAppearance
            isAppearanceLightStatusBars = !isDarkAppearance
        }
        if (Build.VERSION.SDK_INT >= 29) activity.window.isNavigationBarContrastEnforced = false
    }
    val colorScheme = if (themeMode == "dark" || themeMode == "amoled" || (themeMode == "system" && systemDark))
        darkColorScheme(primary = colors.primary, onPrimary = Color.Black, secondary = colors.primaryContainer,
            background = colors.surface, surface = colors.raised, surfaceVariant = colors.variant,
            onSurface = colors.text, onSurfaceVariant = colors.muted, error = colors.error,
            outline = colors.outline)
    else lightColorScheme(primary = colors.primary, onPrimary = Color.White, secondary = colors.primaryContainer,
        background = colors.surface, surface = colors.raised, surfaceVariant = colors.variant,
        onSurface = colors.text, onSurfaceVariant = colors.muted, error = colors.error,
        outline = colors.outline)

    fun persist() = LocalStore.save(context, currentLines, notes, focusedLine, savedSheets, themeMode, accentMode)
    fun showToast(message: String) { toast = message; toastVisible = true }
    fun closeDialog() { activeDialog = null }
    fun copyText(value: String, message: String) {
        if (value.isNotEmpty()) { clipboard.setText(AnnotatedString(value)); showToast(message) }
    }
    fun insertAtCursor(value: String, moveInsidePair: Boolean = false) {
        val safe = focusedLine.coerceIn(0, lineFields.lastIndex)
        val field = lineFields[safe]
        val start = field.selection.start.coerceIn(0, field.text.length)
        val end = field.selection.end.coerceIn(start, field.text.length)
        val nextText = field.text.substring(0, start) + value + field.text.substring(end)
        val cursor = if (moveInsidePair) start + 1 else start + value.length
        lineFields = lineFields.toMutableList().also { it[safe] = TextFieldValue(nextText, androidx.compose.ui.text.TextRange(cursor)) }
        persist()
    }
    fun clearLine() {
        val safe = focusedLine.coerceIn(0, lineFields.lastIndex)
        lineFields = lineFields.toMutableList().also { it[safe] = TextFieldValue("") }
        persist()
    }
    fun removeLine(index: Int) {
        if (lineFields.size <= 1) {
            lineFields = listOf(TextFieldValue(""))
            notes = listOf("")
            rowIds = removeStableRowId(rowIds, 0, allocateRowId())
            expandedNotes = emptySet()
            focusedLine = 0
        } else {
            val safeIndex = index.coerceIn(0, lineFields.lastIndex)
            val removedNumber = safeIndex + 1
            lineFields = lineFields.filterIndexed { i, _ -> i != safeIndex }
                .map { it.copy(text = CalculatorEngine.shiftReferencesForRemove(it.text, removedNumber)) }
            notes = notes.filterIndexed { i, _ -> i != safeIndex }
            rowIds = removeStableRowId(rowIds, safeIndex, allocateRowId())
            expandedNotes = expandedNotes.filter { it != safeIndex }.map { if (it > safeIndex) it - 1 else it }.toSet()
            focusedLine = (safeIndex - 1).coerceAtLeast(0).coerceAtMost(lineFields.lastIndex)
        }
        activateCalculation(focusedLine)
        persist(); focusEpoch++
    }
    fun insertNewLine(index: Int) {
        val safe = index.coerceIn(0, lineFields.size)
        val number = safe + 1
        lineFields = lineFields.map { it.copy(text = CalculatorEngine.shiftReferencesForInsert(it.text, number)) }
            .toMutableList().also { it.add(safe, TextFieldValue("")) }
        notes = notes.toMutableList().also { it.add(safe, "") }
        rowIds = insertStableRowId(rowIds, safe, allocateRowId())
        expandedNotes = expandedNotes.map { if (it >= safe) it + 1 else it }.toSet()
        activateCalculation(safe)
        persist(); focusEpoch++
    }
    fun deleteAtCursor() {
        val safe = focusedLine.coerceIn(0, lineFields.lastIndex)
        val field = lineFields[safe]
        val start = field.selection.start.coerceIn(0, field.text.length)
        val end = field.selection.end.coerceIn(start, field.text.length)
        var next = field.text
        var cursor = start
        when {
            start != end -> next = field.text.removeRange(start, end)
            start > 0 -> {
                val count = if (field.text.substring(0, start).endsWith("sqrt(")) 5 else 1
                next = field.text.removeRange(start - count, start); cursor = start - count
            }
            field.text.isEmpty() -> { removeLine(safe); return }
            else -> return
        }
        lineFields = lineFields.toMutableList().also { it[safe] = TextFieldValue(next, androidx.compose.ui.text.TextRange(cursor)) }
        persist()
    }
    fun sheetCopy(withNotes: Boolean): String {
        val out = mutableListOf<String>()
        currentLines.forEachIndexed { index, line ->
            val kind = line.trim()
            if (kind.isNotEmpty()) {
                val result = results.getOrNull(index)?.text.orEmpty()
                out += if (kind.startsWith("#") || result.isEmpty()) line else "$line = $result"
            }
            if (withNotes && notes.getOrElse(index) { "" }.trim().isNotEmpty()) out += "  Note: ${notes[index].trim()}"
        }
        return out.joinToString("\n")
    }
    fun saveCurrentSheet(title: String) {
        val clean = title.trim().ifEmpty { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date()) }
        val record = SavedSheet("sheet-${System.currentTimeMillis()}-${(100000..999999).random()}", clean,
            System.currentTimeMillis(), currentLines.toList(), currentLines.indices.map { notes.getOrElse(it) { "" } })
        savedSheets = (listOf(record) + savedSheets).take(100)
        persist(); showToast("Sheet saved to History")
    }
    fun openSavedSheet(item: SavedSheet) {
        lineFields = (item.lines.ifEmpty { listOf("") }).map { TextFieldValue(it) }
        notes = lineFields.indices.map { item.notes.getOrElse(it) { "" } }
        if (lineFields.last().text.trim().isNotEmpty()) {
            lineFields = lineFields + TextFieldValue("")
            notes = notes + ""
        }
        rowIds = List(lineFields.size) { allocateRowId() }
        expandedNotes = emptySet()
        calculationKeyboardMode = CalculationKeyboardMode.DEEPCALC
        activateCalculation(lineFields.lastIndex)
        persist(); closeDialog(); focusEpoch++; showToast("Opened “${item.title}”")
    }
    fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DeepCalc")
            putExtra(Intent.EXTRA_TEXT, "Write it. Work it out. See it unfold.\nhttps://github.com/PrasannaOne/DeepCalc")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share DeepCalc"))
    }

    LaunchedEffect(toastVisible, toast) {
        if (toastVisible) { delay(1800); toastVisible = false }
    }
    // A single routing decision owns both keyboard surfaces. The custom keypad
    // is rendered only for a calculation target in DEEPCALC mode; the Android
    // IME is requested for notes, Android-mode calculations and the save dialog.
    LaunchedEffect(keyboardRoute) {
        calculatorKeyboardVisible = false
        if (keyboardRoute.showCalculatorKeyboard) {
            // If we're returning from Notes or Android-keyboard mode, wait in
            // the inset observer below until the IME has completely closed.
            if (!imeVisible) calculatorKeyboardVisible = true
            else keyboard?.hide()
        } else if (keyboardRoute.showSystemKeyboard && activeDialog != "save") {
            // Wait one frame for the newly active field to receive focus before
            // requesting the system IME. A manual Back dismissal is not retried.
            withFrameNanos { }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }
    LaunchedEffect(keyboardRoute.showCalculatorKeyboard, imeVisible) {
        if (keyboardRoute.showCalculatorKeyboard) {
            if (imeVisible) {
                calculatorKeyboardVisible = false
                keyboard?.hide()
            } else {
                calculatorKeyboardVisible = true
            }
        }
    }
    LaunchedEffect(activeDialog) {
        if (activeDialog == "save") {
            withFrameNanos { }
            runCatching { sheetNameRequester.requestFocus() }
            withFrameNanos { }
            keyboard?.show()
        }
    }
    LaunchedEffect(currentLines, notes, focusedLine, savedSheets, themeMode, accentMode) { persist() }

    val platformViewConfiguration = LocalViewConfiguration.current
    val deepCalcViewConfiguration = remember(platformViewConfiguration) {
        object : ViewConfiguration by platformViewConfiguration {
            override val longPressTimeoutMillis: Long = 400L
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides deepCalcViewConfiguration) {
      MaterialTheme(colorScheme = colorScheme) {
      Box(Modifier.fillMaxSize().background(colors.surface)) {
        Column(
            Modifier.fillMaxSize()
                .background(colors.surface)
                .safeDrawingPadding()
                .then(if (keyboardRoute.showSystemKeyboard && imeVisible) Modifier.imePadding() else Modifier)
        ) {
            // Header mirrors the 58–62dp DeepCalc top bar.
            Row(Modifier.widthIn(max = 960.dp).fillMaxWidth().align(Alignment.CenterHorizontally).height(topBarHeight)
                .padding(start = if (compactWidth) 15.dp else 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("DeepCalc", color = colors.primary, fontSize = if (compactWidth) 21.sp else 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    calculationKeyboardMode = if (calculationKeyboardMode == CalculationKeyboardMode.DEEPCALC)
                        CalculationKeyboardMode.ANDROID else CalculationKeyboardMode.DEEPCALC
                }, enabled = activeEditor is ActiveEditor.Calculation && activeDialog == null) {
                    Icon(if (calculationKeyboardMode == CalculationKeyboardMode.DEEPCALC) Icons.Filled.Keyboard else Icons.Filled.Smartphone,
                        contentDescription = if (calculationKeyboardMode == CalculationKeyboardMode.DEEPCALC) "Switch to Android keyboard" else "Switch to DeepCalc keypad",
                        tint = colors.muted)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = colors.muted) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, containerColor = colors.raised) {
                        Column(Modifier.widthIn(min = 184.dp).padding(vertical = 4.dp)) {
                            MenuHeader("SHEET", colors)
                            CompactMenuItem("Copy Sheet", colors) { menuExpanded = false; copyText(sheetCopy(false), "Sheet copied") }
                            CompactMenuItem("Copy Complete", colors) { menuExpanded = false; copyText(sheetCopy(true), "Complete sheet copied") }
                            CompactMenuItem("Save Sheet", colors) {
                                menuExpanded = false
                                sheetName = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date())
                                renameId = null
                                activeDialog = "save"
                            }
                            CompactMenuItem("Clear Sheet", colors) {
                                menuExpanded = false
                                lineFields = listOf(TextFieldValue(""))
                                notes = listOf("")
                                rowIds = listOf(allocateRowId())
                                expandedNotes = emptySet()
                                calculationKeyboardMode = CalculationKeyboardMode.DEEPCALC
                                activateCalculation(0)
                                persist()
                                focusEpoch++
                            }
                            HorizontalDivider(color = colors.variant)
                            MenuHeader("APP", colors)
                            CompactMenuItem("History", colors) { menuExpanded = false; activeDialog = "history" }
                            CompactMenuItem("Guide", colors) { menuExpanded = false; activeDialog = "guide" }
                            CompactMenuItem("Settings", colors) { menuExpanded = false; activeDialog = "settings" }
                            HorizontalDivider(color = colors.variant)
                            MenuHeader("MORE", colors)
                            CompactMenuItem("Share", colors) { menuExpanded = false; shareApp() }
                            CompactMenuItem("About", colors) { menuExpanded = false; activeDialog = "about" }
                        }
                    }
                }
            }

            // Sheet: line numbers, editable expressions, expandable notes and live results.
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                lineFields.forEachIndexed { index, fieldValue ->
                    val rowId = rowIds[index]
                    key(rowId) {
                        if (index in expandedNotes) {
                            val noteBringIntoViewRequester = remember(rowId) { BringIntoViewRequester() }
                            Row(Modifier.widthIn(max = 960.dp).fillMaxWidth()
                                .bringIntoViewRequester(noteBringIntoViewRequester)
                                .padding(start = if (compactWidth) 28.dp else 40.dp, end = if (compactWidth) 7.dp else 12.dp, bottom = 3.dp)
                                .padding(bottom = if (activeEditor == ActiveEditor.Note(index)) 16.dp else 0.dp),
                                verticalAlignment = Alignment.Top) {
                                Text("⌃", modifier = Modifier.width(22.dp).padding(top = 1.dp), color = colors.primary, fontSize = 12.sp)
                                Column(Modifier.weight(1f).padding(start = 9.dp).borderStart(colors.noteAccent)) {
                                    val noteRequester = remember(rowId) { FocusRequester() }
                                    LaunchedEffect(rowId, activeEditor, expandedNotes, focusEpoch, activeDialog) {
                                        if (activeEditor == ActiveEditor.Note(index) && index in expandedNotes && activeDialog == null) {
                                            runCatching { noteRequester.requestFocus() }
                                        }
                                    }
                                    LaunchedEffect(rowId, activeEditor, imeVisible, notes.getOrElse(index) { "" }) {
                                        if (activeEditor == ActiveEditor.Note(index) && index in expandedNotes) {
                                            withFrameNanos { }
                                            noteBringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                    BasicTextField(value = notes.getOrElse(index) { "" }, onValueChange = { value ->
                                        notes = notes.toMutableList().also { while (it.size <= index) it.add(""); it[index] = value }; persist()
                                    }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 21.dp).heightIn(max = 100.dp).focusRequester(noteRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                activeEditor = ActiveEditor.Note(index)
                                            }
                                        },
                                        textStyle = TextStyle(color = colors.text, fontSize = 12.sp, lineHeight = 19.sp),
                                        cursorBrush = SolidColor(colors.primary), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                                        decorationBox = { inner -> Box { if (notes.getOrElse(index) { "" }.isEmpty()) Text("For notes…", color = colors.muted.copy(alpha = .55f), fontSize = 12.sp); inner() } })
                                }
                            }
                        }
                        val requester = remember(rowId) { FocusRequester() }
                        val lineBringIntoViewRequester = remember(rowId) { BringIntoViewRequester() }
                        LaunchedEffect(rowId, focusEpoch, activeEditor, activeDialog) {
                            if (activeDialog == null && activeEditor == ActiveEditor.Calculation(index)) {
                                runCatching { requester.requestFocus() }
                            }
                        }
                        LaunchedEffect(rowId, activeEditor, imeVisible, fieldValue.text) {
                            if (activeEditor == ActiveEditor.Calculation(index)) {
                                withFrameNanos { }
                                lineBringIntoViewRequester.bringIntoView()
                            }
                        }
                        val result = results.getOrNull(index) ?: CalculatorEngine.LineResult(CalculatorEngine.LineResult.Kind.EMPTY, "")
                        val isComment = fieldValue.text.trimStart().startsWith("#")
                        Row(Modifier.widthIn(max = 960.dp).fillMaxWidth()
                            .bringIntoViewRequester(lineBringIntoViewRequester)
                            .padding(horizontal = if (compactWidth) 6.dp else 12.dp, vertical = 4.dp)
                            .padding(bottom = if (activeEditor == ActiveEditor.Calculation(index)) 16.dp else 0.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.width(if (compactWidth) 24.dp else 28.dp).height(36.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable(role = Role.Button) {
                                        copyText(fieldValue.text, if (fieldValue.text.isBlank()) "Line is empty" else "Copied: ${fieldValue.text.trim()}")
                                    },
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text((index + 1).toString(), modifier = Modifier.padding(end = if (compactWidth) 5.dp else 7.dp),
                                    color = colors.muted.copy(alpha = .72f), textAlign = TextAlign.End, fontSize = 11.sp)
                            }
                            Row(Modifier.weight(7f).defaultMinSize(minHeight = 36.dp).clip(RoundedCornerShape(11.dp))
                                .background(colors.input).border(1.dp, if (focusedLine == index && !noteFocused) colors.primary.copy(alpha = .52f) else Color.Transparent, RoundedCornerShape(11.dp))
                                .padding(start = 11.dp, end = 3.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(value = fieldValue, onValueChange = { next ->
                                    lineFields = lineFields.toMutableList().also { it[index] = next }; focusedLine = index; persist()
                                },
                                    // The value-based BasicTextField cannot opt out of IME-on-focus
                                    // through KeyboardOptions. Read-only keeps focus/cursor selection
                                    // but prevents Android's keyboard from flashing in keypad mode.
                                    readOnly = !keyboardRoute.showSystemKeyboard,
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 23.dp).focusRequester(requester)
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            activateCalculation(index)
                                        }
                                    },
                                    textStyle = TextStyle(color = if (isComment) colors.muted else colors.text, fontSize = if (compactWidth) 14.sp else 15.sp,
                                        lineHeight = 23.sp, fontFamily = if (isComment) FontFamily.SansSerif else FontFamily.Monospace),
                                    cursorBrush = SolidColor(colors.primary), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None,
                                        autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { insertNewLine(index + 1) }),
                                    decorationBox = { inner -> Box(Modifier.fillMaxWidth().heightIn(min = 23.dp), contentAlignment = Alignment.CenterStart) { inner() } })
                                IconButton(onClick = {
                                    val closingNotes = index in expandedNotes
                                    expandedNotes = if (closingNotes) expandedNotes - index else expandedNotes + index
                                    if (closingNotes) {
                                        activateCalculation(index)
                                        focusEpoch++
                                    } else {
                                        activeEditor = ActiveEditor.Note(index)
                                    }
                                    persist()
                                }, modifier = Modifier.size(32.dp)) {
                                    Text(if (index in expandedNotes) "⌄" else "⌃", color = if (index in expandedNotes) colors.primary else colors.muted.copy(alpha = .65f), fontSize = 12.sp)
                                }
                            }
                            Box(Modifier.weight(3f).padding(start = 3.dp).defaultMinSize(minHeight = 36.dp)
                                .clip(RoundedCornerShape(11.dp)).background(colors.primaryContainer.copy(alpha = .48f)).padding(horizontal = 7.dp, vertical = 6.dp),
                                contentAlignment = Alignment.TopEnd) {
                                Text(result.text, modifier = Modifier.clickable(enabled = result.kind == CalculatorEngine.LineResult.Kind.SUCCESS) { copyText(result.text, "Copied: ${result.text}") },
                                    color = when (result.kind) { CalculatorEngine.LineResult.Kind.ERROR -> colors.error; CalculatorEngine.LineResult.Kind.EMPTY -> colors.muted; else -> colors.primaryStrong },
                                    textAlign = TextAlign.End, fontFamily = FontFamily.Monospace, fontSize = if (compactWidth) 13.sp else 14.sp, lineHeight = 22.sp,
                                    fontWeight = if (result.kind == CalculatorEngine.LineResult.Kind.SUCCESS) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
                // Extra scroll range keeps the final calculation comfortably
                // above the bottom edge when the keyboard resizes the window.
                Spacer(Modifier.height(32.dp))
            }

            if (toastVisible) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 24.dp, vertical = 6.dp),
                    color = Color(0xFF292532), contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp), shadowElevation = 7.dp
                ) {
                    Text(toast, Modifier.padding(horizontal = 16.dp, vertical = 9.dp), fontSize = 12.sp, lineHeight = 16.sp)
                }
            }

            if (showCalculatorKeyboard) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CalculatorKeyboard(colors = colors, onTap = { key ->
                    when (key) {
                        "enter" -> insertNewLine(focusedLine + 1)
                        "backspace" -> deleteAtCursor()
                        "power" -> insertAtCursor("^")
                        "hash" -> { insertAtCursor("#"); calculationKeyboardMode = CalculationKeyboardMode.ANDROID; persist() }
                        "dollar" -> { insertAtCursor("$"); calculationKeyboardMode = CalculationKeyboardMode.ANDROID; persist() }
                        "paste" -> {
                            val clip = clipboard.getText()?.text
                            if (!clip.isNullOrEmpty()) insertAtCursor(clip) else showToast("Clipboard is empty")
                        }
                        "paren" -> insertAtCursor("()", true)
                        else -> insertAtCursor(key)
                    }
                }, onLong = { key ->
                    when (key) {
                        "power" -> insertAtCursor("sqrt(")
                        "backspace" -> clearLine()
                        "dollar" -> activeDialog = "variables"
                    }
                })
                }
            }
        }

        when (activeDialog) {
            "save" -> AlertDialog(onDismissRequest = { closeDialog() }, containerColor = colors.raised,
                title = { Text(if (renameId == null) "Save Sheet" else "Rename Sheet", color = colors.text) },
                text = { Column {
                    Text("Sheet name", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(7.dp))
                    OutlinedTextField(value = sheetName, onValueChange = { sheetName = it.take(80) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(sheetNameRequester),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                            focusedBorderColor = colors.primary, unfocusedBorderColor = colors.outline.copy(alpha = .35f)))
                } }, confirmButton = { TextButton(onClick = {
                    val cleanTitle = sheetName.trim()
                    val editingId = renameId
                    if (cleanTitle.isNotEmpty()) {
                        if (editingId == null) saveCurrentSheet(cleanTitle) else {
                            savedSheets = savedSheets.map { if (it.id == editingId) it.copy(title = cleanTitle) else it }
                            persist(); showToast("Sheet renamed")
                        }
                        renameId = null
                        activeDialog = if (editingId == null) null else "history"
                    }
                }) { Text("Save", color = colors.primary) } },
                dismissButton = { TextButton(onClick = { closeDialog() }) { Text("Cancel", color = colors.primary) } })
            "history" -> Dialog(onDismissRequest = { closeDialog() }) {
                Surface(shape = RoundedCornerShape(24.dp), color = colors.raised, border = BorderStroke(1.dp, colors.outline.copy(alpha = .17f)), modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth()) {
                    Column(Modifier.padding(22.dp).heightIn(max = 620.dp)) {
                        Text("History", color = colors.text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        if (savedSheets.isEmpty()) {
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No saved sheets yet. Save a sheet to keep it here.", color = colors.muted, fontSize = 13.sp, textAlign = TextAlign.Center) }
                        } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            savedSheets.forEach { item ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                    Text(item.title, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    val noteCount = item.notes.count { it.isNotBlank() }
                                    Text("${DateFormat.getDateTimeInstance().format(Date(item.createdAt))} · ${item.lines.size} lines · $noteCount notes",
                                        color = colors.muted.copy(alpha = .78f), fontSize = 11.sp)
                                    Text(item.lines.firstOrNull { it.isNotBlank() } ?: "Empty sheet", color = colors.muted.copy(alpha = .75f),
                                        fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        HistoryAction("Open", true, colors) { openSavedSheet(item) }
                                        HistoryAction("Rename", false, colors) { sheetName = item.title; renameId = item.id; activeDialog = "save" }
                                        HistoryAction("Delete", false, colors) { pendingDelete = item }
                                    }
                                }
                                HorizontalDivider(color = colors.variant)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { closeDialog() }) { Text("Done", color = colors.primary) } }
                    }
                }
            }
            "variables" -> AlertDialog(onDismissRequest = { closeDialog() }, containerColor = colors.raised,
                title = { Text("Insert Variable", color = colors.text) },
                text = { if (variables.isEmpty()) Text("No variables defined yet.\nDefine a variable with: \$name = value", color = colors.text, fontSize = 13.sp)
                    else Column { Text("Tap to insert:", color = colors.text, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); variables.keys.sorted().forEach { name ->
                        Text("\$$name", Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).clickable { closeDialog(); insertAtCursor("\$$name") }.padding(10.dp), color = colors.primary, fontSize = 13.sp)
                    } } }, confirmButton = { TextButton(onClick = { closeDialog() }) { Text("Cancel", color = colors.primary) } })
            "settings" -> AlertDialog(onDismissRequest = { closeDialog() }, containerColor = colors.raised,
                title = { Text("Settings", color = colors.text) },
                text = { Column {
                    Text("Theme", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(colors.input).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("system" to "System", "light" to "Light", "dark" to "Dark", "amoled" to "AMOLED").forEach { (value, label) ->
                            Box(Modifier.weight(1f).defaultMinSize(minHeight = 35.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (themeMode == value) colors.raised else Color.Transparent)
                                .border(1.dp, if (themeMode == value) colors.primary.copy(alpha = .35f) else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { themeMode = value; persist() }.padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                                Text(label, color = if (themeMode == value) colors.primary else colors.muted,
                                    fontSize = if (compactWidth) 10.sp else 12.sp, fontWeight = if (themeMode == value) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp)); Text("Palettes", color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    val paletteNames = listOf("purple" to "Purple", "blue" to "Sapphire", "emerald" to "Jade", "rose" to "Ruby", "amber" to "Gold")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        paletteNames.forEach { (value, label) ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                                .background(if (accentMode == value) colors.primary.copy(alpha = .08f) else Color.Transparent)
                                .clickable { accentMode = value; persist() }.padding(vertical = 5.dp, horizontal = 1.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(26.dp).border(2.dp, if (accentMode == value) colors.primary else colors.outline.copy(alpha = .14f), CircleShape)
                                    .padding(2.dp).clip(CircleShape).background(palette(themeMode, value, systemDark).primary), contentAlignment = Alignment.Center) {
                                    if (accentMode == value) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(label, color = colors.muted, fontSize = if (compactWidth) 9.sp else 10.sp, maxLines = 1)
                            }
                        }
                    }
                } }, confirmButton = { TextButton(onClick = { closeDialog() }) { Text("Done", color = colors.primary) } })
            "guide" -> AlertDialog(onDismissRequest = { closeDialog() }, containerColor = colors.raised,
                title = { Text("Guide", color = colors.text) },
                text = { Column(Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState())) {
                    GuideSection("Basics", "Type mathematical expressions and see results instantly. Use +, −, ×, ÷ for basic operations, ^ for power, and % for percentage.", colors)
                    GuideSection("Variables", "Define variables with \$name = value\nExample: \$tax = 0.08\nUse them in expressions: 100 × \$tax", colors)
                    GuideSection("Line References", "Reference previous results with $1, $2, etc.\nExample: If line 1 equals 50, then $1 × 2 = 100", colors)
                    GuideSection("Keyboard", "Calculation fields open the DeepCalc keypad by default. Use the header keyboard button to switch the focused calculation to your Android keyboard. Notes always use the Android keyboard; returning to a calculation switches back to the DeepCalc keypad.\n\n^ — Power (hold for √ square root)\n$ — Variables (hold for picker)\n# — Start a comment\n⌫ — Backspace (hold to clear line)\n⏎ — New line", colors)
                    GuideSection("Notes", "Use the small arrow beside a calculation to open its note. Notes are separate from calculations and do not affect results or line references. Notes use your device keyboard.", colors)
                    GuideSection("Copy and Save", "Copy Sheet copies calculations. Copy Complete includes calculations and their notes. Save Sheet stores a separate snapshot in History.", colors)
                    GuideSection("History", "Open History to reopen saved sheets, rename them, or delete them. Your active sheet is autosaved separately.", colors)
                } }, confirmButton = { TextButton(onClick = { closeDialog() }) { Text("OK", color = colors.primary) } })
            "about" -> AlertDialog(onDismissRequest = { closeDialog() }, containerColor = colors.raised,
                title = { Text("About DeepCalc", color = colors.text) },
                text = { Column {
                    Text("A focused space for working things out, line by line. Write expressions, follow live results, reuse variables, reference earlier answers and keep notes beside your calculations. Your sheets stay on your device.",
                        color = colors.text, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(13.dp))
                    Text("Made by Prasanna ♥️", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text("MADE WITH", color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Native Android · Kotlin · Jetpack Compose · Material 3", color = colors.text, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(11.dp))
                    Text("ORIGINAL CREDIT", color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("DeepCalc builds on the original SoulCalc project by Albert “Tigr” Zenkoff. The original MIT licence and required attribution are preserved.",
                        color = colors.text, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Open source · MIT License", color = colors.muted, fontSize = 11.sp)
                } },
                confirmButton = { TextButton(onClick = { closeDialog() }) { Text("Done", color = colors.primary) } })
            else -> Unit
        }

        pendingDelete?.let { item ->
            AlertDialog(onDismissRequest = { pendingDelete = null }, containerColor = colors.raised,
                title = { Text("Delete saved sheet?", color = colors.text) },
                text = { Text("Delete “${item.title}” from History?", color = colors.text) },
                confirmButton = { TextButton(onClick = { savedSheets = savedSheets.filterNot { it.id == item.id }; pendingDelete = null; persist(); showToast("Sheet deleted") }) { Text("Delete", color = colors.error) } },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = colors.primary) } })
        }
      }
      }
    }
}

