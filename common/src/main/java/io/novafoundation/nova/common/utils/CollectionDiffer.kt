package io.novafoundation.nova.common.utils

interface Identifiable {

    val identifier: String
}

fun <T : Identifiable> List<T>.findById(other: Identifiable?): T? = find { it.identifier == other?.identifier }

object CollectionDiffer {

    data class Diff<T>(
        val added: List<T>,
        val updated: List<T>,
        val removed: List<T>,
        val all: List<T>
    ) {
        val newOrUpdated by lazy { updated + added }
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
}

fun <T, R> CollectionDiffer.Diff<T>.map(mapper: (T) -> R) = CollectionDiffer.Diff(
    added = added.map(mapper),
    updated = updated.map(mapper),
    removed = removed.map(mapper),
    all = all.map(mapper)
)
