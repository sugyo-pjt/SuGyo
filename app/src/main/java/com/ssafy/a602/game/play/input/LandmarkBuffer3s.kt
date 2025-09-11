package com.ssafy.a602.game.play.input

class LandmarkBuffer3s {
    private val buf = ArrayDeque<FramePack>()
    private val lock = Any()

    fun add(p: FramePack) = synchronized(lock) {
        buf.addLast(p)
        val cutoff = p.tsMs - 3000
        while (buf.isNotEmpty() && buf.first().tsMs < cutoff) buf.removeFirst()
    }
    fun sliceAround(centerMs: Long): List<FramePack> = synchronized(lock) {
        val s = centerMs - 1000; val e = centerMs + 1000
        buf.filter { it.tsMs in s..e }
    }
    fun latest(): Long = synchronized(lock) { buf.lastOrNull()?.tsMs ?: 0L }
}
