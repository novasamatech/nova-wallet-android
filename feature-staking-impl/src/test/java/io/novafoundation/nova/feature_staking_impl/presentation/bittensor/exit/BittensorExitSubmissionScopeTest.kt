package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Reproduces the deferred warning callback's lifetime without signing or sending a transaction. */
@OptIn(ExperimentalCoroutinesApi::class)
class BittensorExitSubmissionScopeTest {

    @Test
    fun `captured validation scope silently skips submission after warning`() = runTest {
        var submissions = 0
        lateinit var proceed: () -> Unit
        val validation = launch {
            proceed = { launchUnit { submissions++ } }
        }
        validation.join()

        proceed()
        runCurrent()

        assertFalse(validation.isActive)
        assertEquals(0, submissions)
    }

    @Test
    fun `screen scope starts one submission after validation has completed`() = runTest {
        val screenJob = Job()
        val screenScope = CoroutineScope(coroutineContext + screenJob)
        var submissions = 0
        lateinit var proceed: () -> Unit
        val validation = screenScope.launch {
            proceed = { screenScope.launchUnit { submissions++ } }
        }
        validation.join()

        proceed()
        runCurrent()

        assertFalse(validation.isActive)
        assertEquals(1, submissions)
        screenScope.cancel()
    }

    @Test
    fun `closed screen does not launch submission from a late warning callback`() = runTest {
        val screenJob = Job()
        val screenScope = CoroutineScope(coroutineContext + screenJob)
        var submissions = 0
        lateinit var proceed: () -> Unit
        val validation = screenScope.launch {
            proceed = { screenScope.launchUnit { submissions++ } }
        }
        validation.join()
        screenScope.cancel()

        proceed()
        runCurrent()

        assertEquals(0, submissions)
    }
}
