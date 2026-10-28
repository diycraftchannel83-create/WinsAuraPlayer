package com.example.model

data class LyricsLine(
    val timestampMs: Long,
    val text: String
) {
    fun formattedTimestamp(): String {
        val totalHundredths = timestampMs.coerceAtLeast(0L) / 10
        val hundredths = totalHundredths % 100
        val totalSec = totalHundredths / 100
        return "[%02d:%02d.%02d]".format(totalSec / 60, totalSec % 60, hundredths)
    }

    fun formattedDisplayTime(): String {
        val totalHundredths = timestampMs.coerceAtLeast(0L) / 10
        val hundredths = totalHundredths % 100
        val totalSec = totalHundredths / 100
        return "%02d:%02d.%02d".format(totalSec / 60, totalSec % 60, hundredths)
    }
}

object LrcParser {
    private val LRC_REGEX = Regex("""\[(\d{2}):(\d{2})\.?(\d{2,3})?\](.*)""")

    fun parse(lrcContent: String): List<LyricsLine> = parsePlainOrLrc(lrcContent)

    fun parsePlainOrLrc(content: String): List<LyricsLine> {
        if (content.isBlank()) return emptyList()
        val result = mutableListOf<LyricsLine>()
        val timed = content.lines().any { LRC_REGEX.containsMatchIn(it.trim()) }
        for ((index, rawLine) in content.lines().withIndex()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.matches(Regex("""^\[[a-zA-Z]+:.*\]$"""))) continue
            val match = LRC_REGEX.find(trimmed)
            if (match == null) {
                result.add(LyricsLine(if (timed) 0L else index * 4000L, trimmed))
            } else {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val rawFraction = match.groupValues[3]
                val millis = when (rawFraction.length) {
                    2 -> (rawFraction.toLongOrNull() ?: 0L) * 10
                    3 -> rawFraction.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val lyric = match.groupValues[4].trim()
                if (lyric.isNotEmpty()) result.add(LyricsLine((min * 60 + sec) * 1000 + millis, lyric))
            }
        }
        return if (timed) result.sortedBy { it.timestampMs } else result
    }

    fun formatToLrc(lines: List<LyricsLine>, artist: String = "", title: String = ""): String {
        val output = StringBuilder()
        if (title.isNotBlank()) output.append("[ti:").append(title.trim()).append("]\n")
        if (artist.isNotBlank()) output.append("[ar:").append(artist.trim()).append("]\n")
        output.append("[by:WinsAuraPlayer / WinsLyricEditor]\n")
        lines.sortedBy { it.timestampMs }.forEach { line ->
            if (line.text.isNotBlank()) output.append(line.formattedTimestamp()).append(line.text.trim()).append('\n')
        }
        return output.toString().trimEnd()
    }
}
