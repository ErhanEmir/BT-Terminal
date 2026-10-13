package com.erhanemir.btterminal

import android.content.Context

object Prefs {
    private const val FILE = "btterm"
    private const val KEY_NEWLINE = "newline"
    private const val KEY_CUSTOM = "custom_buttons" // "etiket<TAB>deger" kumesi

    fun newlineEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_NEWLINE, true)

    fun setNewlineEnabled(ctx: Context, v: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_NEWLINE, v).apply()
    }

    fun withNewline(ctx: Context, text: String): String =
        if (newlineEnabled(ctx)) text + "\n" else text

    fun loadCustom(ctx: Context): MutableList<Pair<String, String>> {
        val set = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getStringSet(KEY_CUSTOM, emptySet())
            ?: emptySet()
        return set.mapNotNull {
            val i = it.indexOf('\t')
            if (i < 0) null else it.substring(0, i) to it.substring(i + 1)
        }.toMutableList()
    }

    fun saveCustom(ctx: Context, list: List<Pair<String, String>>) {
        val set = list.map { "${it.first}\t${it.second}" }.toSet()
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putStringSet(KEY_CUSTOM, set).apply()
    }
}
