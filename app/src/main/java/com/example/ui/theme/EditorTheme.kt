package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class EditorTheme(
    val name: String,
    val background: Color,
    val editorBackground: Color,
    val sidebarBackground: Color,
    val activeTabBackground: Color,
    val inactiveTabBackground: Color,
    val cursorColor: Color,
    val textColor: Color,
    val keywordColor: Color,
    val stringColor: Color,
    val numberColor: Color,
    val commentColor: Color,
    val functionColor: Color,
    val tagColor: Color,
    val isDark: Boolean
) {
    companion object {
        val DarkModern = EditorTheme(
            name = "Dark Modern",
            background = Color(0xFF1E1E1E),
            editorBackground = Color(0xFF181818),
            sidebarBackground = Color(0xFF252526),
            activeTabBackground = Color(0xFF1E1E1E),
            inactiveTabBackground = Color(0xFF2D2D2D),
            cursorColor = Color(0xFF007ACC),
            textColor = Color(0xFFD4D4D4),
            keywordColor = Color(0xFF569CD6),
            stringColor = Color(0xFFCE9178),
            numberColor = Color(0xFFB5CEA8),
            commentColor = Color(0xFF6A9955),
            functionColor = Color(0xFFDCDCAA),
            tagColor = Color(0xFFE06C75),
            isDark = true
        )

        val Monokai = EditorTheme(
            name = "Monokai Jet",
            background = Color(0xFF272822),
            editorBackground = Color(0xFF1E1F1C),
            sidebarBackground = Color(0xFF191919),
            activeTabBackground = Color(0xFF272822),
            inactiveTabBackground = Color(0xFF34352F),
            cursorColor = Color(0xFFF92672),
            textColor = Color(0xFFF8F8F2),
            keywordColor = Color(0xFFF92672),
            stringColor = Color(0xFFE6DB74),
            numberColor = Color(0xFFAE81FF),
            commentColor = Color(0xFF75715E),
            functionColor = Color(0xFFA6E22E),
            tagColor = Color(0xFFFD971F),
            isDark = true
        )

        val Dracula = EditorTheme(
            name = "Dracula Vampire",
            background = Color(0xFF282A36),
            editorBackground = Color(0xFF1E1F29),
            sidebarBackground = Color(0xFF21222C),
            activeTabBackground = Color(0xFF282A36),
            inactiveTabBackground = Color(0xFF191A21),
            cursorColor = Color(0xFFFF79C6),
            textColor = Color(0xFFF8F8F2),
            keywordColor = Color(0xFFFF79C6),
            stringColor = Color(0xFFF1FA8C),
            numberColor = Color(0xFFBD93F9),
            commentColor = Color(0xFF6272A4),
            functionColor = Color(0xFF50FA7B),
            tagColor = Color(0xFF8BE9FD),
            isDark = true
        )

        val SolarizedLight = EditorTheme(
            name = "Solarized Gold",
            background = Color(0xFFFDF6E3),
            editorBackground = Color(0xFFEEE8D5),
            sidebarBackground = Color(0xFFE4DDCD),
            activeTabBackground = Color(0xFFFDF6E3),
            inactiveTabBackground = Color(0xFFD9D2C0),
            cursorColor = Color(0xFF268BD2),
            textColor = Color(0xFF586E75),
            keywordColor = Color(0xFF859900),
            stringColor = Color(0xFF2AA198),
            numberColor = Color(0xFFD33682),
            commentColor = Color(0xFF93A1A1),
            functionColor = Color(0xFF268BD2),
            tagColor = Color(0xFFCB4B16),
            isDark = false
        )

        val GitHubLight = EditorTheme(
            name = "GitHub Light",
            background = Color(0xFFFFFFFF),
            editorBackground = Color(0xFFF6F8FA),
            sidebarBackground = Color(0xFFEAECEF),
            activeTabBackground = Color(0xFFFFFFFF),
            inactiveTabBackground = Color(0xFFFAF9F6),
            cursorColor = Color(0xFF0969DA),
            textColor = Color(0xFF24292F),
            keywordColor = Color(0xFFCF222E),
            stringColor = Color(0xFF0A3069),
            numberColor = Color(0xFF0550AE),
            commentColor = Color(0xFF6E7781),
            functionColor = Color(0xFF8250DF),
            tagColor = Color(0xFF116329),
            isDark = false
        )

        val allThemes = listOf(DarkModern, Monokai, Dracula, SolarizedLight, GitHubLight)
    }
}
