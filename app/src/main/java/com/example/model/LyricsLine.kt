package com.example.model

data class LyricsLine(
    val timestampMs: Long,
    val text: String
)

object LrcParser {
    private val LRC_REGEX = Regex("""\[(\d{2}):(\d{2})\.?(\d{2,3})?\](.*)""")

    fun parse(lrcContent: String): List<LyricsLine> {
        if (lrcContent.isBlank()) return emptyList()
        val lines = mutableListOf<LyricsLine>()
        val sourceLines = lrcContent.lines().filter { it.isNotBlank() }
        val hasTimestamps = sourceLines.any { LRC_REGEX.containsMatchIn(it.trim()) }
        if (!hasTimestamps) {
            // Plain embedded ID3 lyrics have no timing; keep every line visible in the lyrics view.
            return sourceLines.mapIndexed { index, line -> LyricsLine(index * 4000L, line.trim()) }
        }
        for (line in sourceLines) {
            val trimmed = line.trim()
            val match = LRC_REGEX.find(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val millisRaw = match.groupValues[3]
                val millis = when {
                    millisRaw.length == 2 -> (millisRaw.toLongOrNull() ?: 0L) * 10
                    millisRaw.length == 3 -> millisRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val timeMs = (min * 60 + sec) * 1000 + millis
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    lines.add(LyricsLine(timeMs, text))
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}
