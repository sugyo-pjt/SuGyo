package com.ssafy.a602.game.play.input

import java.util.concurrent.atomic.AtomicReference

class PingPongBuffer<T>(initialCapacity: Int = 16) {
    private var a = ArrayList<T>(initialCapacity)
    private var b = ArrayList<T>(initialCapacity)
    private val active = AtomicReference<MutableList<T>>(a)
    private var standby: MutableList<T> = b

    fun add(item: T) {
        active.get().add(item)
    }

    /** 0.3초마다 호출: 활성↔대기 스왑, 반환 리스트는 전송 후 clear() 필수 */
    fun swapAndGet(): MutableList<T> {
        val toSend = active.getAndSet(standby)
        standby = toSend
        return toSend
    }

    fun clearAll() {
        active.get().clear()
        standby.clear()
    }
}
