package io.novafoundation.nova.feature_staking_impl.data.bittensor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryPreferencesTest {

    private val preferences = InMemoryPreferences()

    private val firstKey = "first"

    private val secondKey = "second"

    @Test
    fun `keys flow emits all requested keys initially then only the changed selected key`() = runTest {
        val changes = mutableListOf<List<String>>()
        val observer = backgroundScope.launch { preferences.keysFlow(firstKey, secondKey).collect { changes += it } }
        runCurrent()
        assertEquals(listOf(listOf(firstKey, secondKey)), changes)

        preferences.putString(field = "unrelated", value = "ignored")
        runCurrent()
        assertEquals(listOf(listOf(firstKey, secondKey)), changes)

        preferences.putString(secondKey, value = "changed")
        runCurrent()
        preferences.removeField(firstKey)
        runCurrent()
        assertEquals(listOf(listOf(firstKey, secondKey), listOf(secondKey), listOf(firstKey)), changes)
        observer.cancelAndJoin()
    }

    @Test
    fun `key flow emits one initial key and every later write and removal`() = runTest {
        val changes = mutableListOf<String>()
        val observer = backgroundScope.launch { preferences.keyFlow(firstKey).collect { changes += it } }
        runCurrent()
        assertEquals(listOf(firstKey), changes)

        preferences.putString(firstKey, value = "value")
        runCurrent()
        preferences.removeField(firstKey)
        runCurrent()
        assertEquals(listOf(firstKey, firstKey, firstKey), changes)
        observer.cancelAndJoin()
    }

    @Test
    fun `no requested keys emits an empty initial list and ignores later writes`() = runTest {
        val changes = mutableListOf<List<String>>()
        val observer = backgroundScope.launch { preferences.keysFlow().collect { changes += it } }
        runCurrent()
        assertEquals(listOf(emptyList<String>()), changes)

        preferences.putString(firstKey, value = "ignored")
        runCurrent()
        assertEquals(listOf(emptyList<String>()), changes)
        observer.cancelAndJoin()
    }
}
