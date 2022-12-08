package io.novafoundation.nova.common.utils

import io.novafoundation.nova.common.utils.CollectionDiffer.Diff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

@JvmInline
private value class IntIdentifiable(val value: Int): Identifiable {
    override val identifier: String
        get() = value.toString()
}

private data class Struct(val id: Int, val data: Int) : Identifiable {
    override val identifier: String
        get() = id.toString()
}

class FlowExtKtTest {

    @Test
    fun testDiffed() {
        runBlocking {
            performIntTest(
                first = emptyList(),
                second = listOf(1, 2, 3),
                expectedDiff = Diff(
                    removed = emptyList(),
                    added = listOf(1, 2, 3),
                    updated = emptyList(),
                    all = listOf(1, 2, 3)
                ),
            )

            performIntTest(
                first = listOf(1, 2, 3),
                second = listOf(1, 2, 3),
                expectedDiff = Diff(
                    removed = emptyList(),
                    added = emptyList(),
                    updated = emptyList(),
                    all = listOf(1, 2, 3)
                )
            )

            performIntTest(
                first = listOf(1, 2, 3),
                second = emptyList(),
                expectedDiff = Diff(
                    removed = listOf(1, 2, 3),
                    added = emptyList(),
                    updated = emptyList(),
                    all = emptyList()
                )
            )

            performIntTest(
                first = listOf(1, 2),
                second = listOf(2, 3),
                expectedDiff = Diff(
                    removed = listOf(1),
                    added = listOf(3),
                    updated = emptyList(),
                    all = listOf(2, 3)
                )
            )

            val newStruct = Struct(id = 1, data = 1)
            performTest(
                first = listOf(Struct(id = 1, data = 0)),
                second = listOf(newStruct),
                expectedDiff = Diff(
                    removed = emptyList(),
                    added =  emptyList(),
                    updated = listOf(newStruct),
                    all = listOf(newStruct)
                )
            )
        }
    }

    private suspend fun performIntTest(
        first: List<Int>,
        second: List<Int>,
        expectedDiff: Diff<Int>,
    ) {
        performTest(first, second, expectedDiff, ::IntIdentifiable)
    }

    private suspend fun <T: Identifiable> performTest(
        first: List<T>,
        second: List<T>,
        expectedDiff: Diff<T>,
    ) {
        performTest(first, second, expectedDiff) { it }
    }

    private suspend fun <T> performTest(
        first: List<T>,
        second: List<T>,
        expectedDiff: Diff<T>,
        toIdentifiable: (T) -> Identifiable
    ) {
        val firstIdentifiable = first.map(toIdentifiable)
        val secondIdentifiable = second.map(toIdentifiable)

        val diffed = flowOf(firstIdentifiable, secondIdentifiable)
            .diffed()
            .withIndex().first { (index, _) -> index == 1 } // take second element which will actually represent diff
            .value

        assertEquals(expectedDiff.map(toIdentifiable), diffed)
    }
}
