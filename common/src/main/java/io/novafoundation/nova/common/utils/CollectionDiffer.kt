package io.novafoundation.nova.common.utils

interface Identifiable {

    val identifier: String
}

object CollectionDiffer {

    class Diff<T>(
        val newOrUpdated: List<T>,
        val removed: List<T>
    )

    fun <T : Identifiable> findDiff(
        newItems: List<T>,
        oldItems: List<T>,
        forceUseNewItems: Boolean
    ): Diff<T> {

        val newKeys: Set<String> = newItems.mapTo(mutableSetOf()) { it.identifier }
        val oldMapping = oldItems.associateBy { it.identifier }

        val newOrUpdated = newItems.mapNotNull { new ->
            val old = oldMapping[new.identifier]

            when {
                old == null -> new // new
                old != new -> new // updated
                forceUseNewItems -> new // forced to use new item
                else -> null // same
            }
        }

        val removed = oldItems.filter { it.identifier !in newKeys }

        return Diff(newOrUpdated, removed)
    }
}
