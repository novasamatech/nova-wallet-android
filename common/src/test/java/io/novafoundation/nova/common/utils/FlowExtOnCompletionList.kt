package io.novafoundation.nova.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FlowExtOnCompletionList {

    private val defaultScope = CoroutineScope(Dispatchers.Default)

    @Test
    fun onCompletionListAllFlowsCompleted() = runBlocking {
        val flows = listOf(
            flowOf { "object" },
            flowOf { "object" }
        )

        var onCompletionWasCalled = false

        flows.onCompletion { onCompletionWasCalled = true }
            .merge()
            .launchIn(defaultScope)

        delay(50)
        assert(onCompletionWasCalled)
    }

    @Test
    fun onCompletionListOneFlowsCompleted() = runBlocking {
        val flows = listOf(
            flowOf { "object" },
            endlessFlow()
        )

        var onCompletionWasCalled = false

        flows.onCompletion { onCompletionWasCalled = true }
            .merge()
            .launchIn(defaultScope)

        delay(50)
        assert(!onCompletionWasCalled)
    }

    private fun endlessFlow(): Flow<String> {
        return flow {
            while (true) {
                emit("object")
                delay(10)
            }
        }
    }
}
