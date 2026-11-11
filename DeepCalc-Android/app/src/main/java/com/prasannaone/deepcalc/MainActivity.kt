package com.prasannaone.deepcalc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DeepCalcApp() }
    }
}

private data class SavedSheet(
    val id: String,
    var title: String,
    val createdAt: Long,
    val lines: List<String>,
    val notes: List<String>
)

private data class AppSnapshot(
    val lines: List<String>,
    val notes: List<String>,
    val focus: Int,
    val savedSheets: List<SavedSheet>,
    val theme: String,
    val accent: String
)

private object LocalStore {
    private const val PREFS = "deepcalc-native-v1"

    fun load(context: Context): AppSnapshot {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedLines = runCatching { JSONArray(p.getString("active_lines", "[\"\"]") ?: "[\"\"]").toStrings() }.getOrDefault(listOf(""))
            .ifEmpty { listOf("") }
        val storedNotes = runCatching { JSONArray(p.getString("active_notes", "[]") ?: "[]").toStrings() }.getOrDefault(emptyList())
        val lines = storedLines.toMutableList().apply { if (last().trim().isNotEmpty()) add("") }
        val notes = lines.indices.map { storedNotes.getOrElse(it) { "" } }
        val storedFocus = p.getInt("focus", 0)
        val focus = if (storedFocus >= storedLines.lastIndex) lines.lastIndex else storedFocus.coerceIn(0, lines.lastIndex)
        val history = runCatching {
            val array = JSONArray(p.getString("history", "[]") ?: "[]")
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val l = o.optJSONArray("lines")?.toStrings() ?: return@mapNotNull null
                SavedSheet(o.optString("id"), o.optString("title", "Untitled sheet"), o.optLong("createdAt"), l,
                    o.optJSONArray("notes")?.toStrings() ?: emptyList())
            }
        }.getOrDefault(emptyList())
        return AppSnapshot(lines, notes, focus, history,
            p.getString("theme", "system") ?: "system", p.getString("accent", "purple") ?: "purple")
    }

    fun save(context: Context, lines: List<String>, notes: List<String>, focus: Int,
             history: List<SavedSheet>, theme: String, accent: String) {
        val activeLines = JSONArray().apply { lines.forEach { put(it) } }.toString()
        val activeNotes = JSONArray().apply { lines.indices.forEach { put(notes.getOrElse(it) { "" }) } }.toString()
        val records = JSONArray().apply {
            history.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id); put("title", item.title); put("createdAt", item.createdAt)
                    put("lines", JSONArray(item.lines)); put("notes", JSONArray(item.notes))
                })
            }
        }.toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("active_lines", activeLines).putString("active_notes", activeNotes)
            .putInt("focus", focus).putString("history", records)
            .putString("theme", theme).putString("accent", accent).remove("custom_keyboard").apply()
    }

    private fun JSONArray.toStrings(): List<String> = (0 until length()).map { optString(it, "") }
}

private data class Palette(
    val surface: Color, val raised: Color, val soft: Color, val variant: Color,
    val input: Color, val text: Color, val muted: Color, val outline: Color,
    val primary: Color, val primaryStrong: Color, val primaryContainer: Color,
    val secondaryContainer: Color, val secondaryText: Color, val keyAccent: Color,
    val keyAccentText: Color, val error: Color, val noteAccent: Color
)

private fun palette(theme: String, accent: String, systemDark: Boolean): Palette {
    val dark = theme == "dark" || theme == "amoled" || (theme == "system" && systemDark)
    val black = theme == "amoled"
    val base = when {
        black -> Palette(Color.Black, Color(0xFF080808), Color(0xFF101010), Color(0xFF1B1B1B), Color(0xFF101010), Color(0xFFF4F0F8), Color(0xFFB9B2C2), Color(0xFF68616F), Color(0xFFD0B5FF), Color(0xFFE0CEFF), Color(0xFF211632), Color(0xFF211A2B), Color(0xFFEEE3FF), Color(0xFF3B2A56), Color(0xFFF5EDFF), Color(0xFFFFB4AB), Color(0xFFA992D2))
        dark -> Palette(Color(0xFF14121A), Color(0xFF1C1924), Color(0xFF211D2A), Color(0xFF302B3A), Color(0xFF211E29), Color(0xFFF0EBF7), Color(0xFFC1B8CE), Color(0xFF81788E), Color(0xFFC4A7FF), Color(0xFFD4C0FF), Color(0xFF302447), Color(0xFF332A45), Color(0xFFE9DDFF), Color(0xFF4A3868), Color(0xFFF1E8FF), Color(0xFFFFB4AB), Color(0xFFB19BD8))
        else -> Palette(Color(0xFFFBFAFF), Color.White, Color(0xFFF3F0FA), Color(0xFFE9E5F1), Color(0xFFF2EFF8), Color(0xFF201B2B), Color(0xFF716B7D), Color(0xFFAAA2B7), Color(0xFF7251B5), Color(0xFF60409F), Color(0xFFEEE5FF), Color(0xFFE9E1F7), Color(0xFF392B55), Color(0xFFDED0FF), Color(0xFF38245F), Color(0xFFB3261E), Color(0xFFA18ACB))
    }
    val colors = when (accent) {
        "blue" -> if (dark) listOf(Color(0xFFA9C7FF), Color(0xFFC3D7FF), Color(0xFF20334F), Color(0xFF293F60), Color(0xFFE8F0FF), Color(0xFF91ADD8)) else listOf(Color(0xFF426DA9), Color(0xFF315B98), Color(0xFFE0EBFF), Color(0xFFD5E4FF), Color(0xFF1E3B66), Color(0xFF829FC8))
        "emerald" -> if (dark) listOf(Color(0xFF91D5B4), Color(0xFFB0E6CB), Color(0xFF1D4032), Color(0xFF244B3A), Color(0xFFE0FFED), Color(0xFF8DBDA4)) else listOf(Color(0xFF28775A), Color(0xFF1C6449), Color(0xFFD9F2E6), Color(0xFFD2F0E1), Color(0xFF174B38), Color(0xFF7AAA94))
        "rose" -> if (dark) listOf(Color(0xFFF0A9C3), Color(0xFFFFC2D5), Color(0xFF4B2938), Color(0xFF583244), Color(0xFFFFE8EF), Color(0xFFCF9AAF)) else listOf(Color(0xFFA64E70), Color(0xFF913D60), Color(0xFFFFE1EB), Color(0xFFFFDFEB), Color(0xFF67263F), Color(0xFFC58BA3))
        "amber" -> if (dark) listOf(Color(0xFFE9C078), Color(0xFFF4D49B), Color(0xFF49391F), Color(0xFF59431F), Color(0xFFFFF0CF), Color(0xFFC4A878)) else listOf(Color(0xFF916112), Color(0xFF79500A), Color(0xFFFFEDC9), Color(0xFFFFEDC9), Color(0xFF65420C), Color(0xFFB89A68))
        else -> if (dark) listOf(Color(0xFFC4A7FF), Color(0xFFD4C0FF), Color(0xFF302447), Color(0xFF4A3868), Color(0xFFF1E8FF), Color(0xFFB19BD8)) else listOf(Color(0xFF7251B5), Color(0xFF60409F), Color(0xFFEEE5FF), Color(0xFFDED0FF), Color(0xFF38245F), Color(0xFFA18ACB))
    }
    return base.copy(primary = colors[0], primaryStrong = colors[1], primaryContainer = colors[2],
        secondaryContainer = if (accent == "purple") base.secondaryContainer else colors[2],
        secondaryText = if (accent == "purple") base.secondaryText else colors[4],
        keyAccent = colors[3], keyAccentText = colors[4], noteAccent = colors[5])
}

@Composable
private fun DeepCalcApp() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val initial = remember { LocalStore.load(context) }
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
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
    var savedSheets by remember { mutableStateOf(initial.savedSheets) }
    var focusedLine by remember { mutableIntStateOf(initial.focus) }
    var customKeyboard by remember { mutableStateOf(true) }
    var noteFocused by remember { mutableStateOf(false) }
    var focusedNoteIndex by remember { mutableStateOf<Int?>(null) }
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
        focusEpoch++
    }
    fun clearLine() {
        val safe = focusedLine.coerceIn(0, lineFields.lastIndex)
        lineFields = lineFields.toMutableList().also { it[safe] = TextFieldValue("") }
        persist(); focusEpoch++
    }
    fun removeLine(index: Int) {
        if (lineFields.size <= 1) {
            lineFields = listOf(TextFieldValue("")); notes = listOf(""); focusedLine = 0
        } else {
            val removedNumber = index + 1
            lineFields = lineFields.filterIndexed { i, _ -> i != index }
                .map { it.copy(text = CalculatorEngine.shiftReferencesForRemove(it.text, removedNumber)) }
            notes = notes.filterIndexed { i, _ -> i != index }
            expandedNotes = expandedNotes.filter { it != index }.map { if (it > index) it - 1 else it }.toSet()
            focusedLine = (index - 1).coerceAtLeast(0)
        }
        persist(); focusEpoch++
    }
    fun insertNewLine(index: Int) {
        val safe = index.coerceIn(0, lineFields.size)
        val number = safe + 1
        lineFields = lineFields.map { it.copy(text = CalculatorEngine.shiftReferencesForInsert(it.text, number)) }
            .toMutableList().also { it.add(safe, TextFieldValue("")) }
        notes = notes.toMutableList().also { it.add(safe, "") }
        expandedNotes = expandedNotes.map { if (it >= safe) it + 1 else it }.toSet()
        focusedLine = safe; customKeyboard = true; noteFocused = false
        keyboard?.hide(); persist(); focusEpoch++
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
        persist(); focusEpoch++
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
        if (lineFields.last().text.trim().isNotEmpty()) { lineFields = lineFields + TextFieldValue(""); notes = notes + "" }
        expandedNotes = emptySet(); focusedLine = lineFields.lastIndex; customKeyboard = true; noteFocused = false; focusedNoteIndex = null
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
    LaunchedEffect(customKeyboard, noteFocused, activeDialog) {
        when {
            activeDialog == "save" -> keyboard?.show()
            activeDialog != null -> keyboard?.hide()
            noteFocused -> keyboard?.show()
            customKeyboard -> keyboard?.hide()
            else -> keyboard?.show()
        }
    }
    LaunchedEffect(activeDialog) {
        if (activeDialog == "save") {
            delay(100)
            runCatching { sheetNameRequester.requestFocus() }
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
        Column(Modifier.fillMaxSize().background(colors.surface).statusBarsPadding().navigationBarsPadding().imePadding()) {
            // Header mirrors the 58–62dp DeepCalc top bar.
            Row(Modifier.widthIn(max = 960.dp).fillMaxWidth().align(Alignment.CenterHorizontally).height(topBarHeight)
                .padding(start = if (compactWidth) 15.dp else 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("DeepCalc", color = colors.primary, fontSize = if (compactWidth) 21.sp else 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    // The header switch controls calculation fields only. Notes
                    // always use the Android keyboard.
                    customKeyboard = !customKeyboard
                    persist()
                }, enabled = !noteFocused) {
                    Icon(if (customKeyboard) Icons.Filled.Keyboard else Icons.Filled.Smartphone,
                        contentDescription = if (customKeyboard) "Switch to system keyboard" else "Switch to calculator keyboard",
                        tint = colors.muted)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = colors.muted) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, containerColor = colors.raised) {
                        MenuHeader("SHEET", colors)
                        DropdownMenuItem(text = { Text("Copy Sheet") }, onClick = { menuExpanded = false; copyText(sheetCopy(false), "Sheet copied") })
                        DropdownMenuItem(text = { Text("Copy Complete") }, onClick = { menuExpanded = false; copyText(sheetCopy(true), "Complete sheet copied") })
                        DropdownMenuItem(text = { Text("Save Sheet") }, onClick = { menuExpanded = false; sheetName = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date()); renameId = null; activeDialog = "save" })
                        DropdownMenuItem(text = { Text("Clear Sheet") }, onClick = { menuExpanded = false; lineFields = listOf(TextFieldValue("")); notes = listOf(""); expandedNotes = emptySet(); focusedLine = 0; noteFocused = false; focusedNoteIndex = null; customKeyboard = true; persist(); focusEpoch++ })
                        HorizontalDivider(color = colors.variant)
                        MenuHeader("APP", colors)
                        DropdownMenuItem(text = { Text("History") }, onClick = { menuExpanded = false; activeDialog = "history" })
                        DropdownMenuItem(text = { Text("Guide") }, onClick = { menuExpanded = false; activeDialog = "guide" })
                        DropdownMenuItem(text = { Text("Settings") }, onClick = { menuExpanded = false; activeDialog = "settings" })
                        HorizontalDivider(color = colors.variant)
                        MenuHeader("MORE", colors)
                        DropdownMenuItem(text = { Text("Share") }, onClick = { menuExpanded = false; shareApp() })
                        DropdownMenuItem(text = { Text("About") }, onClick = { menuExpanded = false; activeDialog = "about" })
                    }
                }
            }

            // Sheet: line numbers, editable expressions, expandable notes and live results.
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                lineFields.forEachIndexed { index, fieldValue ->
                    if (index in expandedNotes) {
                        val noteBringIntoViewRequester = remember(index) { BringIntoViewRequester() }
                        Row(Modifier.widthIn(max = 960.dp).fillMaxWidth()
                            .bringIntoViewRequester(noteBringIntoViewRequester)
                            .padding(start = if (compactWidth) 28.dp else 40.dp, end = if (compactWidth) 7.dp else 12.dp, bottom = 3.dp),
                            verticalAlignment = Alignment.Top) {
                            Text("⌃", modifier = Modifier.width(22.dp).padding(top = 1.dp), color = colors.primary, fontSize = 12.sp)
                            Column(Modifier.weight(1f).padding(start = 9.dp).borderStart(colors.noteAccent)) {
                                val noteRequester = remember(index) { FocusRequester() }
                                LaunchedEffect(focusedNoteIndex, expandedNotes) {
                                    if (focusedNoteIndex == index && index in expandedNotes) {
                                        runCatching { noteRequester.requestFocus() }
                                        keyboard?.show()
                                    }
                                }
                                BasicTextField(value = notes.getOrElse(index) { "" }, onValueChange = { value ->
                                    notes = notes.toMutableList().also { while (it.size <= index) it.add(""); it[index] = value }; persist()
                                }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 21.dp).heightIn(max = 100.dp).focusRequester(noteRequester)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            noteFocused = true
                                            focusedNoteIndex = index
                                            keyboard?.show()
                                            coroutineScope.launch {
                                                delay(260)
                                                noteBringIntoViewRequester.bringIntoView()
                                            }
                                        } else if (focusedNoteIndex == index) {
                                            noteFocused = false
                                            focusedNoteIndex = null
                                        }
                                    },
                                    textStyle = TextStyle(color = colors.text, fontSize = 12.sp, lineHeight = 19.sp),
                                    cursorBrush = SolidColor(colors.primary), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                                    decorationBox = { inner -> Box { if (notes.getOrElse(index) { "" }.isEmpty()) Text("For notes…", color = colors.muted.copy(alpha = .55f), fontSize = 12.sp); inner() } })
                            }
                        }
                    }
                    val requester = remember(index) { FocusRequester() }
                    val lineBringIntoViewRequester = remember(index) { BringIntoViewRequester() }
                    LaunchedEffect(focusEpoch, customKeyboard, noteFocused, focusedLine, activeDialog) {
                        if (activeDialog == null && !noteFocused && focusedLine == index) {
                            if (focusEpoch > 0) runCatching { requester.requestFocus() }
                            delay(180)
                            lineBringIntoViewRequester.bringIntoView()
                        }
                    }
                    val result = results.getOrNull(index) ?: CalculatorEngine.LineResult(CalculatorEngine.LineResult.Kind.EMPTY, "")
                    val isComment = fieldValue.text.trimStart().startsWith("#")
                    Row(Modifier.widthIn(max = 960.dp).fillMaxWidth()
                        .bringIntoViewRequester(lineBringIntoViewRequester)
                        .padding(horizontal = if (compactWidth) 6.dp else 12.dp, vertical = 4.dp),
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
                            }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 23.dp).focusRequester(requester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        focusedLine = index
                                        noteFocused = false
                                        focusedNoteIndex = null
                                        customKeyboard = true
                                        keyboard?.hide()
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
                                noteFocused = !closingNotes
                                focusedNoteIndex = if (noteFocused) index else null
                                if (closingNotes) {
                                    focusedLine = index
                                    customKeyboard = true
                                    focusEpoch++
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

            if (customKeyboard && !noteFocused) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CalculatorKeyboard(colors = colors, onTap = { key ->
                    when (key) {
                        "enter" -> insertNewLine(focusedLine + 1)
                        "backspace" -> deleteAtCursor()
                        "power" -> insertAtCursor("^")
                        "hash" -> { insertAtCursor("#"); customKeyboard = false; keyboard?.show(); persist() }
                        "dollar" -> { insertAtCursor("$"); customKeyboard = false; keyboard?.show(); persist() }
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

        if (toastVisible) {
            Surface(Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp).padding(bottom = if (customKeyboard && !noteFocused) {
                when { configuration.screenHeightDp <= 700 -> 265.dp; compactWidth -> 285.dp; else -> 300.dp }
            } else 24.dp),
                color = Color(0xFF292532), contentColor = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 7.dp) {
                Text(toast, Modifier.padding(horizontal = 16.dp, vertical = 11.dp), fontSize = 13.sp, lineHeight = 18.sp)
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

@Composable
private fun MenuHeader(label: String, colors: Palette) {
    Text(label, Modifier.padding(start = 15.dp, top = 8.dp, bottom = 3.dp), color = colors.muted.copy(alpha = .72f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun HistoryAction(label: String, primary: Boolean, colors: Palette, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = if (primary) colors.primaryContainer else colors.soft) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = if (primary) colors.primary else colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GuideSection(title: String, body: String, colors: Palette) {
    Text(title, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 13.dp, bottom = 4.dp))
    Text(body, color = colors.text, fontSize = 13.sp, lineHeight = 20.sp)
}

private fun Modifier.borderStart(color: Color): Modifier = this.then(Modifier.drawBehind {
    drawLine(color.copy(alpha = .60f), androidx.compose.ui.geometry.Offset(0f, 0f),
        androidx.compose.ui.geometry.Offset(0f, size.height), strokeWidth = 2.dp.toPx())
})

@Composable
private fun CalculatorKeyboard(colors: Palette, onTap: (String) -> Unit, onLong: (String) -> Unit) {
    val configuration = LocalConfiguration.current
    val gap = when {
        configuration.screenWidthDp >= 700 -> 7.dp
        configuration.screenHeightDp <= 700 -> 4.dp
        else -> 6.dp
    }
    val cellHeight = when {
        configuration.screenHeightDp <= 560 -> (configuration.screenHeightDp * 0.067f).dp.coerceIn(34.dp, 49.dp)
        configuration.screenHeightDp <= 700 -> (configuration.screenHeightDp * 0.067f).dp.coerceIn(37.dp, 49.dp)
        configuration.screenWidthDp <= 420 -> (configuration.screenHeightDp * 0.074f).dp.coerceIn(42.dp, 54.dp)
        configuration.screenWidthDp >= 700 -> 58.dp
        else -> (configuration.screenHeightDp * 0.071f).dp.coerceIn(43.dp, 58.dp)
    }
    BoxWithConstraints(Modifier.widthIn(max = 960.dp).fillMaxWidth().background(colors.variant)
        .padding(horizontal = if (configuration.screenWidthDp >= 700) 14.dp else 6.dp,
            vertical = if (configuration.screenHeightDp <= 700) 5.dp else 9.dp)) {
        val keys = listOf(
            listOf("^" to "power", "#" to "hash", "$" to "dollar", "=" to "=", "▣" to "paste"),
            listOf("7" to "7", "8" to "8", "9" to "9", "÷" to "÷", "%" to "%"),
            listOf("4" to "4", "5" to "5", "6" to "6", "×" to "×", "()" to "paren")
        )
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            keys.forEach { row ->
                Row(Modifier.fillMaxWidth().height(cellHeight), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { (label, key) -> KeyboardKey(label, key, Modifier.weight(1f).fillMaxHeight(), colors, onTap, onLong) }
                }
            }
            Box(Modifier.fillMaxWidth().height(cellHeight * 2 + gap)) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    Row(Modifier.fillMaxWidth().height(cellHeight), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        listOf("1" to "1", "2" to "2", "3" to "3", "−" to "−").forEach { (label, key) -> KeyboardKey(label, key, Modifier.weight(1f).fillMaxHeight(), colors, onTap, onLong) }
                        Spacer(Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().height(cellHeight), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        listOf("0" to "0", "." to ".", "⌫" to "backspace", "+" to "+").forEach { (label, key) -> KeyboardKey(label, key, Modifier.weight(1f).fillMaxHeight(), colors, onTap, onLong) }
                        Spacer(Modifier.weight(1f))
                    }
                }
                Box(Modifier.align(Alignment.TopEnd).width((this@BoxWithConstraints.maxWidth - gap * 4) / 5).height(cellHeight * 2 + gap)) {
                    KeyboardKey("↵", "enter", Modifier.fillMaxSize(), colors, onTap, onLong, enter = true)
                }
            }
        }
    }
}

@Composable
private fun KeyboardKey(label: String, key: String, modifier: Modifier, colors: Palette,
                        onTap: (String) -> Unit, onLong: (String) -> Unit, enter: Boolean = false) {
    val special = key in setOf("power", "hash", "dollar", "=", "paste", "÷", "%", "×", "paren", "−", "+")
    val danger = key == "backspace" || enter
    val bg = when { danger -> colors.keyAccent; special -> colors.secondaryContainer; else -> colors.raised }
    val fg = when { danger -> colors.keyAccentText; special -> colors.secondaryText; else -> colors.text }
    var longPressVisual by remember(key) { mutableStateOf(false) }
    Box(modifier.clip(RoundedCornerShape(if (enter) 13.dp else 11.dp)).background(bg)
        .semantics { role = Role.Button; onClick { onTap(key); true } }
        .pointerInput(key) {
            var longTriggered = false
            detectTapGestures(
                onTap = { onTap(key) },
                onLongPress = { longTriggered = true; longPressVisual = true },
                onPress = {
                    longTriggered = false
                    val released = tryAwaitRelease()
                    if (released && longTriggered) {
                        if (key in setOf("power", "backspace", "dollar")) onLong(key) else onTap(key)
                    }
                    longPressVisual = false
                }
            )
        }, contentAlignment = Alignment.Center) {
        Text(if (key == "power" && longPressVisual) "√" else label, color = fg,
            fontSize = if (enter) 25.sp else 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
    }
}
