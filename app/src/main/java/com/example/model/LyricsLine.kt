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
        for (line in lrcContent.lines()) {
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
        val timestampedLyrics = lines.sortedBy { it.timestampMs }
        if (timestampedLyrics.isNotEmpty()) return timestampedLyrics

        // MP3 USLT lyric tags are usually plain text rather than LRC timestamped lines.
        // Spread plain lines across the duration as a readable karaoke-style fallback.
        return lrcContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("[") }
            .mapIndexed { index, text -> LyricsLine(index * 3000L, text) }
            .toList()
    }
}
