package io.novafoundation.nova.common.utils

import android.database.Cursor

fun <R> Cursor.collectAll(collectItem: (Cursor) -> R): List<R> {
    if (!moveToFirst()) return emptyList()

    val items = mutableListOf<R>()

    do {
        val item = collectItem(this)
        items.add(item)
    } while (moveToNext())

    return items
}
