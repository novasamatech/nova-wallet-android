package io.novafoundation.nova.test_shared

import io.novafoundation.nova.common.utils.CollectionDiffer

fun <T> removesElement(elementCheck: (T) -> Boolean) = argThat<CollectionDiffer.Diff<T>> {
    it.newOrUpdated.isEmpty() && elementCheck(it.removed.single())
}

fun <T> insertsElement(elementCheck: (T) -> Boolean) = argThat<CollectionDiffer.Diff<T>> {
    it.removed.isEmpty() && elementCheck(it.newOrUpdated.single())
}

fun <T> emptyDiff() = argThat<CollectionDiffer.Diff<T>> {
    it.newOrUpdated.isEmpty() && it.removed.isEmpty()
}
