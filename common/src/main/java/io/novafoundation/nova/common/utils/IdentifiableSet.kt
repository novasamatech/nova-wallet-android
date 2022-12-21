package io.novafoundation.nova.common.utils

class SetItem<T : Identifiable>(val value: T) {

    override fun equals(other: Any?): Boolean {
        if (other !is SetItem<*>) return false
        if (value.javaClass != other.value.javaClass) return false

        return value.identifier == other.value.identifier
    }

    override fun hashCode(): Int {
        return value.identifier.hashCode()
    }
}

fun <T : Identifiable> T.asSetItem(): SetItem<T> {
    return SetItem(this)
}

fun <T : Identifiable> Collection<T>.asSetItems(): Set<SetItem<T>> {
    val setItemList = this.map { SetItem(it) }
    return setOf(*setItemList.toTypedArray())
}
