package io.novafoundation.nova.common.view.input.seekbar

class SeekbarValues<T>(val values: List<SeekbarValue<T>>) {

    fun valueAt(index: Int): T? {
        return values.getOrNull(index)?.value
    }

    val max: Int
        get() = values.size - 1
}

class SeekbarValue<T>(val value: T, val label: String, val labelColorRes: Int)
