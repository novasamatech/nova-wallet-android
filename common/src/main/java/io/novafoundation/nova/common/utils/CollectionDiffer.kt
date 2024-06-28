package io.novafoundation.nova.common.utils

interface Identifiable {

    val identifier: String
}

fun <T : Identifiable> Iterable<T>.findById(other: Identifiable?): T? = find { it.identifier == other?.identifier }
fun <T : Identifiable> Iterable<T>.findById(id: String): T? = find { it.identifier == id }

fun <T : Identifiable> Iterable<T>.firstById(id: String): T = first { it.identifier == id }

fun CollectionDiffer.Diff<*>.hasDifference() = newOrUpdated.isNotEmpty() || removed.isNotEmpty()

fun CollectionDiffer.Diff<*>.hasUpdated() = updated.isNotEmpty()

object CollectionDiffer {

    data class Diff<T>(
        val added: List<T>,
        val updated: List<T>,
        val removed: List<T>,
        val all: List<T>
    ) {
        val newOrUpdated by lazy { updated + added }

        companion object {
            fun <T> empty() = CollectionDiffer.Diff<T>(
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList()
            )

            fun <T> added(list: List<T>) = CollectionDiffer.Diff<T>(
                added = list,
                emptyList(),
                emptyList(),
                emptyList()
            )
        }
    }

    fun <T : Identifiable> findDiff(
        newItems: List<T>,
        oldItems: List<T>,
        forceUseNewItems: Boolean
    ): Diff<T> {
        val newKeys: Set<String> = newItems.mapTo(mutableSetOf()) { it.identifier }
        val oldMapping = oldItems.associateBy { it.identifier }

        val added = newItems.mapNotNull { new ->
            val old = oldMapping[new.identifier]

            new.takeIf { old == null }
        }

        val updated = newItems.mapNotNull { new ->
            val old = oldMapping[new.identifier]

            // old exists and it is different from new (or we're forced to use new)
            new.takeIf { old != null && (old != new || forceUseNewItems) }
        }

        val removed = oldItems.filter { it.identifier !in newKeys }

        return Diff(added = added, updated = updated, removed = removed, all = newItems)
    }

    fun <K, V> findDiff(
        newItems: Map<K, V>,
        oldItems: Map<K, V>,
        forceUseNewItems: Boolean
    ): Diff<Map.Entry<K, V>> {
        val added = mutableListOf<Map.Entry<K, V>>()
        val updated = mutableListOf<Map.Entry<K, V>>()

        newItems.forEach { newEntry ->
            val (key, newValue) = newEntry
            val oldValue = oldItems[key]

            when {
                key !in oldItems -> added.add(newEntry)
                oldValue != newValue || forceUseNewItems -> updated.add(newEntry)
            }
        }

        val removed = oldItems.mapNotNull { entry -> entry.takeIf { entry.key !in newItems } }

        return Diff(added = added, updated = updated, removed = removed, all = newItems.entries.toList())
    }
}

fun <T, R> CollectionDiffer.Diff<T>.map(mapper: (T) -> R) = CollectionDiffer.Diff(
    added = added.map(mapper),
    updated = updated.map(mapper),
    removed = removed.map(mapper),
    all = all.map(mapper)
)
