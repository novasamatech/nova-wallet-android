package io.novafoundation.nova.test_shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class CoroutineTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    fun runCoroutineTest(test: suspend CoroutineScope.() -> Unit) = testScope.runBlockingTest {
        test(testScope)
    }
}
