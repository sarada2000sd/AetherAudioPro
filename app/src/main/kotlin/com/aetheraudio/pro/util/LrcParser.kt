package com.aetheraudio.pro.util

import java.io.File

data class LrcLine(val timeMs: Long, val text: String)

/**
 * Parses standard LRC timestamp tags: [mm:ss.xx]Lyric text (spec: local synchronized .lrc support).
 */
object LrcParser {
    private val tagRegex = Regex("""\[(\d{2}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    fun parse(path: String?): List<LrcLine> {
        if (path == null) return emptyList()
        val file = File(path)
        if (!file.exists()) return emptyList()
        val lines = mutableListOf<LrcLine>()
        file.forEachLine { raw ->
            val matches = tagRegex.findAll(raw)
            val text = raw.replace(tagRegex, "").trim()
            if (text.isEmpty()) return@forEachLine
            matches.forEach { m ->
                val minutes = m.groupValues[1].toLong()
                val seconds = m.groupValues[2].toLong()
                val fraction = m.groupValues[3].let { if (it.isEmpty()) 0L else it.padEnd(3, '0').take(3).toLong() }
                val timeMs = minutes * 60_000 + seconds * 1000 + fraction
                lines += LrcLine(timeMs, text)
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    fun activeIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) idx = i else break
        }
        return idx
    }
}
