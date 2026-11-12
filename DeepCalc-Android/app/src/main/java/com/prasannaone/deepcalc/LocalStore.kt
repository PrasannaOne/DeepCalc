package com.prasannaone.deepcalc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class SavedSheet(
    val id: String,
    var title: String,
    val createdAt: Long,
    val lines: List<String>,
    val notes: List<String>
)

internal data class AppSnapshot(
    val lines: List<String>,
    val notes: List<String>,
    val focus: Int,
    val savedSheets: List<SavedSheet>,
    val theme: String,
    val accent: String
)

/** Device-local persistence for the active sheet, preferences and History. */
internal object LocalStore {
    private const val PREFS = "deepcalc-native-v1"

    fun load(context: Context): AppSnapshot {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedLines = runCatching {
            JSONArray(preferences.getString("active_lines", "[\"\"]") ?: "[\"\"]")
                .toStrings()
        }.getOrDefault(listOf(""))
            .ifEmpty { listOf("") }

        val storedNotes = runCatching {
            JSONArray(preferences.getString("active_notes", "[]") ?: "[]").toStrings()
        }.getOrDefault(emptyList())

        // Always keep a trailing empty editor row so the sheet is ready for input.
        val lines = storedLines.toMutableList().apply {
            if (last().trim().isNotEmpty()) add("")
        }
        val notes = lines.indices.map { storedNotes.getOrElse(it) { "" } }
        val storedFocus = preferences.getInt("focus", 0)
        val focus = if (storedFocus >= storedLines.lastIndex) {
            lines.lastIndex
        } else {
            storedFocus.coerceIn(0, lines.lastIndex)
        }

        val history = runCatching {
            val array = JSONArray(preferences.getString("history", "[]") ?: "[]")
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val itemLines = item.optJSONArray("lines")?.toStrings()
                    ?: return@mapNotNull null
                SavedSheet(
                    id = item.optString("id"),
                    title = item.optString("title", "Untitled sheet"),
                    createdAt = item.optLong("createdAt"),
                    lines = itemLines,
                    notes = item.optJSONArray("notes")?.toStrings() ?: emptyList()
                )
            }
        }.getOrDefault(emptyList())

        return AppSnapshot(
            lines = lines,
            notes = notes,
            focus = focus,
            savedSheets = history,
            theme = preferences.getString("theme", "system") ?: "system",
            accent = preferences.getString("accent", "purple") ?: "purple"
        )
    }

    fun save(
        context: Context,
        lines: List<String>,
        notes: List<String>,
        focus: Int,
        history: List<SavedSheet>,
        theme: String,
        accent: String
    ) {
        val activeLines = JSONArray().apply {
            lines.forEach { put(it) }
        }.toString()
        val activeNotes = JSONArray().apply {
            lines.indices.forEach { index -> put(notes.getOrElse(index) { "" }) }
        }.toString()
        val records = JSONArray().apply {
            history.forEach { sheet ->
                put(JSONObject().apply {
                    put("id", sheet.id)
                    put("title", sheet.title)
                    put("createdAt", sheet.createdAt)
                    put("lines", JSONArray(sheet.lines))
                    put("notes", JSONArray(sheet.notes))
                })
            }
        }.toString()

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("active_lines", activeLines)
            .putString("active_notes", activeNotes)
            .putInt("focus", focus)
            .putString("history", records)
            .putString("theme", theme)
            .putString("accent", accent)
            .remove("custom_keyboard")
            .apply()
    }

    private fun JSONArray.toStrings(): List<String> =
        (0 until length()).map { optString(it, "") }
}
