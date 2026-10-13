package com.erhanemir.btterminal

/** Keypad uzerindeki mini ekranlar icin son komutlar. Her yonde en fazla 2 satir tutulur. */
object CmdHistory {

    private const val KEEP = 2

    val rx = ArrayDeque<String>()
    val tx = ArrayDeque<String>()

    var onChange: (() -> Unit)? = null

    @Synchronized
    fun pushRx(line: String) {
        val t = line.trim()
        if (t.isEmpty()) return
        rx.addLast(t)
        while (rx.size > KEEP) rx.removeFirst()
        onChange?.invoke()
    }

    @Synchronized
    fun pushTx(cmd: String) {
        val t = cmd.trim()
        if (t.isEmpty()) return
        tx.addLast(t)
        while (tx.size > KEEP) tx.removeFirst()
        onChange?.invoke()
    }

    @Synchronized
    fun clear() {
        rx.clear()
        tx.clear()
        onChange?.invoke()
    }
}
