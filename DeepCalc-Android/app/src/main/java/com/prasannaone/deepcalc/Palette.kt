package com.prasannaone.deepcalc

import androidx.compose.ui.graphics.Color

internal data class Palette(
    val surface: Color,
    val raised: Color,
    val soft: Color,
    val variant: Color,
    val input: Color,
    val text: Color,
    val muted: Color,
    val outline: Color,
    val primary: Color,
    val primaryStrong: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color,
    val secondaryText: Color,
    val keyAccent: Color,
    val keyAccentText: Color,
    val error: Color,
    val noteAccent: Color
)

internal fun palette(theme: String, accent: String, systemDark: Boolean): Palette {
    val dark = theme == "dark" || theme == "amoled" || (theme == "system" && systemDark)
    val base = when {
        theme == "amoled" -> Palette(
            Color.Black,
            Color(0xFF080808),
            Color(0xFF101010),
            Color(0xFF1B1B1B),
            Color(0xFF101010),
            Color(0xFFF4F0F8),
            Color(0xFFB9B2C2),
            Color(0xFF68616F),
            Color(0xFFD0B5FF),
            Color(0xFFE0CEFF),
            Color(0xFF211632),
            Color(0xFF211A2B),
            Color(0xFFEEE3FF),
            Color(0xFF3B2A56),
            Color(0xFFF5EDFF),
            Color(0xFFFFB4AB),
            Color(0xFFA992D2)
        )
        dark -> Palette(
            Color(0xFF14121A),
            Color(0xFF1C1924),
            Color(0xFF211D2A),
            Color(0xFF302B3A),
            Color(0xFF211E29),
            Color(0xFFF0EBF7),
            Color(0xFFC1B8CE),
            Color(0xFF81788E),
            Color(0xFFC4A7FF),
            Color(0xFFD4C0FF),
            Color(0xFF302447),
            Color(0xFF332A45),
            Color(0xFFE9DDFF),
            Color(0xFF4A3868),
            Color(0xFFF1E8FF),
            Color(0xFFFFB4AB),
            Color(0xFFB19BD8)
        )
        else -> Palette(
            Color(0xFFFBFAFF),
            Color.White,
            Color(0xFFF3F0FA),
            Color(0xFFE9E5F1),
            Color(0xFFF2EFF8),
            Color(0xFF201B2B),
            Color(0xFF716B7D),
            Color(0xFFAAA2B7),
            Color(0xFF7251B5),
            Color(0xFF60409F),
            Color(0xFFEEE5FF),
            Color(0xFFE9E1F7),
            Color(0xFF392B55),
            Color(0xFFDED0FF),
            Color(0xFF38245F),
            Color(0xFFB3261E),
            Color(0xFFA18ACB)
        )
    }

    val accents = when (accent) {
        "blue" -> if (dark) {
            listOf(Color(0xFFA9C7FF), Color(0xFFC3D7FF), Color(0xFF20334F), Color(0xFF293F60), Color(0xFFE8F0FF), Color(0xFF91ADD8))
        } else {
            listOf(Color(0xFF426DA9), Color(0xFF315B98), Color(0xFFE0EBFF), Color(0xFFD5E4FF), Color(0xFF1E3B66), Color(0xFF829FC8))
        }
        "emerald" -> if (dark) {
            listOf(Color(0xFF91D5B4), Color(0xFFB0E6CB), Color(0xFF1D4032), Color(0xFF244B3A), Color(0xFFE0FFED), Color(0xFF8DBDA4))
        } else {
            listOf(Color(0xFF28775A), Color(0xFF1C6449), Color(0xFFD9F2E6), Color(0xFFD2F0E1), Color(0xFF174B38), Color(0xFF7AAA94))
        }
        "rose" -> if (dark) {
            listOf(Color(0xFFF0A9C3), Color(0xFFFFC2D5), Color(0xFF4B2938), Color(0xFF583244), Color(0xFFFFE8EF), Color(0xFFCF9AAF))
        } else {
            listOf(Color(0xFFA64E70), Color(0xFF913D60), Color(0xFFFFE1EB), Color(0xFFFFDFEB), Color(0xFF67263F), Color(0xFFC58BA3))
        }
        "amber" -> if (dark) {
            listOf(Color(0xFFE9C078), Color(0xFFF4D49B), Color(0xFF49391F), Color(0xFF59431F), Color(0xFFFFF0CF), Color(0xFFC4A878))
        } else {
            listOf(Color(0xFF916112), Color(0xFF79500A), Color(0xFFFFEDC9), Color(0xFFFFEDC9), Color(0xFF65420C), Color(0xFFB89A68))
        }
        else -> if (dark) {
            listOf(Color(0xFFC4A7FF), Color(0xFFD4C0FF), Color(0xFF302447), Color(0xFF4A3868), Color(0xFFF1E8FF), Color(0xFFB19BD8))
        } else {
            listOf(Color(0xFF7251B5), Color(0xFF60409F), Color(0xFFEEE5FF), Color(0xFFDED0FF), Color(0xFF38245F), Color(0xFFA18ACB))
        }
    }

    return base.copy(
        primary = accents[0],
        primaryStrong = accents[1],
        primaryContainer = accents[2],
        secondaryContainer = if (accent == "purple") base.secondaryContainer else accents[2],
        secondaryText = if (accent == "purple") base.secondaryText else accents[4],
        keyAccent = accents[3],
        keyAccentText = accents[4],
        noteAccent = accents[5]
    )
}
