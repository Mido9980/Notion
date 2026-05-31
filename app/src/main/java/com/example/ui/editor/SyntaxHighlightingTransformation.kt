package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.EditorTheme

class SyntaxHighlightingTransformation(
    private val extension: String,
    private val theme: EditorTheme
) : VisualTransformation {

    companion object {
        // HTML / XML Regexes (Precompiled)
        private val htmlCommentRegex = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
        private val htmlTagRegex = Regex("</?[a-zA-Z0-9:-]+([^>]*?)>")
        private val htmlTagNameRegex = Regex("</?[a-zA-Z0-9:-]+")
        private val htmlStringRegex = Regex("\"[^\"]*\"|'[^']*'")

        // CSS Regexes (Precompiled)
        private val cssCommentRegex = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        private val cssPropertyRegex = Regex("[a-zA-Z0-9-]+(?=\\s*:)")
        private val cssColorRegex = Regex("#[a-fA-F0-9]{3,8}")

        // JS / JSON Regexes (Precompiled)
        private val jsMlCommentRegex = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        private val jsSlCommentRegex = Regex("//.*")
        private val jsStringRegex = Regex("\"[^\"]*\"|'[^']*'|`[^`]*`")
        private val jsKeywords = listOf(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete",
            "do", "else", "export", "extends", "finally", "for", "function", "if", "import", "in",
            "instanceof", "new", "return", "super", "switch", "this", "throw", "try", "typeof",
            "var", "void", "while", "with", "yield", "let", "static"
        )
        private val jsKeywordRegex = Regex("\\b(${jsKeywords.joinToString("|")})\\b")
        private val jsNumberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        private val jsFunctionRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\Parenthesised)")
        private val jsFunctionRegexObj = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\()")

        // Python Regexes (Precompiled)
        private val pyCommentRegex = Regex("#.*")
        private val pyTripleQuoteRegex = Regex("\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''")
        private val pyStringRegex = Regex("\"[^\"]*\"|'[^']*'")
        private val pyKeywords = listOf(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class",
            "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
            "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
            "try", "while", "with", "yield"
        )
        private val pyKeywordRegex = Regex("\\b(${pyKeywords.joinToString("|")})\\b")
        private val pyNumberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        private val pyFunctionRegexObj = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\()")

        // Kotlin Regexes (Precompiled)
        private val ktMlCommentRegex = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        private val ktSlCommentRegex = Regex("//.*")
        private val ktStringRegex = Regex("\"[^\"]*\"")
        private val ktKeywords = listOf(
            "as", "as?", "break", "class", "continue", "do", "else", "false", "for", "fun", "if",
            "in", "is", "!is", "null", "object", "package", "return", "super", "this", "throw",
            "true", "try", "typealias", "val", "var", "when", "while", "import", "interface", "private",
            "public", "internal", "protected"
        )
        private val ktKeywordRegex = Regex("\\b(${ktKeywords.joinToString("|")})\\b")
        private val ktNumberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        private val ktFunctionRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\()")
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val builder = AnnotatedString.Builder(rawText)

        try {
            when (extension.lowercase()) {
                "html", "xml" -> highlightHtml(rawText, builder)
                "css" -> highlightCss(rawText, builder)
                "js", "json" -> highlightJavaScript(rawText, builder)
                "py" -> highlightPython(rawText, builder)
                "kt" -> highlightKotlin(rawText, builder)
                else -> { /* No highlighting for raw text */ }
            }
        } catch (e: Exception) {
            // Fallback safely to plain text
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun highlightHtml(text: String, builder: AnnotatedString.Builder) {
        val styled = BooleanArray(text.length)

        // 1. Comments <!-- ... -->
        htmlCommentRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(SpanStyle(color = theme.commentColor), start, end)
            for (i in start until end) {
                if (i in styled.indices) styled[i] = true
            }
        }

        // 2. Tag brackets and tag names: </?[a-zA-Z0-9:-]+([^>]*?)>
        htmlTagRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            
            var isComment = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isComment = true
                    break
                }
            }
            
            if (!isComment) {
                val tagRange = match.range
                htmlTagNameRegex.find(match.value)?.let { nameMatch ->
                    val nameStart = tagRange.first + nameMatch.range.first
                    val nameEnd = tagRange.first + nameMatch.range.last + 1
                    builder.addStyle(SpanStyle(color = theme.tagColor), nameStart, nameEnd)
                    for (i in nameStart until nameEnd) {
                        if (i in styled.indices) styled[i] = true
                    }
                }
            }
        }

        // 3. Strings inside HTML attributes
        htmlStringRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.stringColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }
    }

    private fun highlightCss(text: String, builder: AnnotatedString.Builder) {
        val styled = BooleanArray(text.length)

        // 1. Comments /* ... */
        cssCommentRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(SpanStyle(color = theme.commentColor), start, end)
            for (i in start until end) {
                if (i in styled.indices) styled[i] = true
            }
        }

        // 2. Properties and attributes inside selectors
        cssPropertyRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.keywordColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 3. Color values
        cssColorRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.stringColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }
    }

    private fun highlightJavaScript(text: String, builder: AnnotatedString.Builder) {
        val styled = BooleanArray(text.length)

        // 1. Multiline comments /* ... */
        jsMlCommentRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(SpanStyle(color = theme.commentColor), start, end)
            for (i in start until end) {
                if (i in styled.indices) styled[i] = true
            }
        }

        // 2. Singleline comments //
        jsSlCommentRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(SpanStyle(color = theme.commentColor), start, end)
            for (i in start until end) {
                if (i in styled.indices) styled[i] = true
            }
        }

        // 3. Strings: double quotes, single quotes, template literals
        jsStringRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.stringColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 4. Keywords
        jsKeywordRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.keywordColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 5. Numbers
        jsNumberRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.numberColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 6. Functions / Methods
        jsFunctionRegexObj.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.functionColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }
    }

    private fun highlightPython(text: String, builder: AnnotatedString.Builder) {
        val styled = BooleanArray(text.length)

        // 1. Comments #
        pyCommentRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(SpanStyle(color = theme.commentColor), start, end)
            for (i in start until end) {
                if (i in styled.indices) styled[i] = true
            }
        }

        // 2. Docstrings / Triple Quoted Strings
        pyTripleQuoteRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.commentColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 3. Regular Strings
        pyStringRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.stringColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 4. Keywords
        pyKeywordRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.keywordColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 5. Numbers
        pyNumberRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.numberColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 6. Functions/methods
        pyFunctionRegexObj.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.functionColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }
    }

    private fun highlightKotlin(text: String, builder: AnnotatedString.Builder) {
        val styled = BooleanArray(text.length)

        // 1. Multiline comments /* ... */
        ktMlCommentRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(SpanStyle(color = theme.commentColor), start, end)
            for (i in start until end) {
                if (i in styled.indices) styled[i] = true
            }
        }

        // 2. Singleline comments //
        ktSlCommentRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(SpanStyle(color = theme.commentColor), start, end)
            for (i in start until end) {
                if (i in styled.indices) styled[i] = true
            }
        }

        // 3. Strings
        ktStringRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.stringColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 4. Keywords
        ktKeywordRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.keywordColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 5. Numbers
        ktNumberRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.numberColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }

        // 6. Functions
        ktFunctionRegex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            var isAnyStyled = false
            for (i in start until end) {
                if (i in styled.indices && styled[i]) {
                    isAnyStyled = true
                    break
                }
            }
            if (!isAnyStyled) {
                builder.addStyle(SpanStyle(color = theme.functionColor), start, end)
                for (i in start until end) {
                    if (i in styled.indices) styled[i] = true
                }
            }
        }
    }
}
