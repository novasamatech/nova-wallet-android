package io.novafoundation.nova.common.data.network

import io.novafoundation.nova.common.data.network.NovaUtilsMirrorCoolDown.Admission
import io.novafoundation.nova.common.data.network.NovaUtilsMirrorCoolDown.Outcome
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaUtilsMirrorCoolDownTest {

    private enum class Kind { OPEN, PROBE, SKIPPED }

    @Volatile
    private var nowMillis = 0L

    private val coolDown = NovaUtilsMirrorCoolDown(initialMillis = PAUSE_MILLIS, maxMillis = MAX_PAUSE_MILLIS, clock = { nowMillis })

    private fun admit(cacheOnly: Boolean = false): Admission = coolDown.admit(cacheOnly)

    private fun kindOf(admission: Admission): Kind {
        return when (admission) {
            is Admission.Open -> Kind.OPEN
            is Admission.Probe -> Kind.PROBE
            Admission.Skipped -> Kind.SKIPPED
        }
    }

    private fun nextKind(cacheOnly: Boolean = false): Kind = kindOf(admit(cacheOnly))

    /** Pauses an open mirror and lets the pause pass. */
    private fun pauseAndWait() {
        coolDown.report(admit(), Outcome.FAILED)
        nowMillis += PAUSE_MILLIS
    }

    @Test
    fun `open mirror admits every request and stays open whatever is reported but a failure`() {
        val reported = listOf(Outcome.ANSWERED, Outcome.NO_VERDICT).map { coolDown.report(admit(), it) }

        assertEquals(listOf<Long?>(null, null), reported)
        assertEquals(listOf(Kind.OPEN, Kind.OPEN), listOf(nextKind(), nextKind()))
    }

    @Test
    fun `failure skips the mirror until the pause has passed, then exactly one request probes`() {
        val startedPause = coolDown.report(admit(), Outcome.FAILED)

        val rightAfter = nextKind()
        nowMillis += PAUSE_MILLIS - 1
        val beforeTheEnd = nextKind()
        nowMillis += 1
        val afterTheEnd = listOf(nextKind(), nextKind(), nextKind())

        assertEquals(PAUSE_MILLIS, startedPause)
        assertEquals(listOf(Kind.SKIPPED, Kind.SKIPPED), listOf(rightAfter, beforeTheEnd))
        assertEquals(listOf(Kind.PROBE, Kind.SKIPPED, Kind.SKIPPED), afterTheEnd)
    }

    @Test
    fun `probe outcome decides what the next requests do`() {
        class Row(val outcome: Outcome, val startedPause: Long?, val rightAfter: Kind, val afterTheLongerPause: Kind)

        val rows = listOf(
            Row(Outcome.ANSWERED, startedPause = null, rightAfter = Kind.OPEN, afterTheLongerPause = Kind.OPEN),
            Row(Outcome.FAILED, startedPause = PAUSE_MILLIS * 2, rightAfter = Kind.SKIPPED, afterTheLongerPause = Kind.PROBE),
            Row(Outcome.NO_VERDICT, startedPause = null, rightAfter = Kind.PROBE, afterTheLongerPause = Kind.SKIPPED)
        )

        rows.forEach { row ->
            nowMillis = 0
            val coolDown = NovaUtilsMirrorCoolDown(PAUSE_MILLIS, MAX_PAUSE_MILLIS) { nowMillis }
            coolDown.report(coolDown.admit(cacheOnly = false), Outcome.FAILED)
            nowMillis += PAUSE_MILLIS

            val startedPause = coolDown.report(coolDown.admit(cacheOnly = false), row.outcome)
            val rightAfter = kindOf(coolDown.admit(cacheOnly = false))
            nowMillis += PAUSE_MILLIS * 2
            val afterTheLongerPause = kindOf(coolDown.admit(cacheOnly = false))

            assertEquals("${row.outcome} started pause", row.startedPause, startedPause)
            assertEquals("${row.outcome} right after", row.rightAfter, rightAfter)
            assertEquals("${row.outcome} after the longer pause", row.afterTheLongerPause, afterTheLongerPause)
        }
    }

    @Test
    fun `pause doubles on each failed probe up to the maximum and starts over after an answer`() {
        val expectedPauses = listOf(PAUSE_MILLIS, PAUSE_MILLIS * 2, MAX_PAUSE_MILLIS, MAX_PAUSE_MILLIS)

        val pauses = expectedPauses.map { expectedPause ->
            val startedPause = coolDown.report(admit(), Outcome.FAILED)
            nowMillis += expectedPause
            startedPause
        }
        coolDown.report(admit(), Outcome.ANSWERED)
        val pauseAfterAnswer = coolDown.report(admit(), Outcome.FAILED)

        assertEquals(expectedPauses, pauses)
        assertEquals(PAUSE_MILLIS, pauseAfterAnswer)
    }

    @Test
    fun `failures of requests admitted together start one pause`() {
        val first = admit()
        val second = admit()

        val startedPauses = listOf(first, second).map { coolDown.report(it, Outcome.FAILED) }
        nowMillis += PAUSE_MILLIS

        assertEquals(listOf(PAUSE_MILLIS, null), startedPauses)
        assertEquals("the second failure must not have made the pause longer", Kind.PROBE, nextKind())
    }

    @Test
    fun `answer of a request admitted before the pause ends it, its failure or silence does not`() {
        val outcomes = mapOf(Outcome.ANSWERED to Kind.OPEN, Outcome.FAILED to Kind.SKIPPED, Outcome.NO_VERDICT to Kind.SKIPPED)

        outcomes.forEach { (outcome, expected) ->
            val coolDown = NovaUtilsMirrorCoolDown(PAUSE_MILLIS, MAX_PAUSE_MILLIS) { nowMillis }
            val admittedBeforeThePause = coolDown.admit(cacheOnly = false)
            coolDown.report(coolDown.admit(cacheOnly = false), Outcome.FAILED)

            val startedPause = coolDown.report(admittedBeforeThePause, outcome)

            assertNull(startedPause)
            assertEquals(outcome.name, expected, kindOf(coolDown.admit(cacheOnly = false)))
        }
    }

    @Test
    fun `answer while a probe is in flight ends the pause and the probe's late report changes nothing`() {
        listOf(Outcome.FAILED, Outcome.NO_VERDICT).forEach { lateProbeOutcome ->
            val coolDown = NovaUtilsMirrorCoolDown(PAUSE_MILLIS, MAX_PAUSE_MILLIS) { nowMillis }
            val admittedBeforeThePause = coolDown.admit(cacheOnly = false)
            coolDown.report(coolDown.admit(cacheOnly = false), Outcome.FAILED)
            nowMillis += PAUSE_MILLIS
            val probe = coolDown.admit(cacheOnly = false)

            coolDown.report(admittedBeforeThePause, Outcome.ANSWERED)
            val startedPause = coolDown.report(probe, lateProbeOutcome)

            assertNull(startedPause)
            assertEquals(lateProbeOutcome.name, Kind.OPEN, kindOf(coolDown.admit(cacheOnly = false)))
        }
    }

    @Test
    fun `failure admitted under an earlier open state does not pause the mirror that has answered since`() {
        val admittedLongAgo = admit()
        pauseAndWait()
        coolDown.report(admit(), Outcome.ANSWERED)

        val startedPause = coolDown.report(admittedLongAgo, Outcome.FAILED)

        assertNull(startedPause)
        assertEquals(Kind.OPEN, nextKind())
    }

    @Test
    fun `late failure of an earlier probe is not taken for the failure of the current one`() {
        pauseAndWait()
        val earlierProbe = admit()
        coolDown.report(admit(cacheOnly = true), Outcome.ANSWERED)
        pauseAndWait()
        val currentProbe = admit()

        val startedByEarlier = coolDown.report(earlierProbe, Outcome.FAILED)
        val whileCurrentProbes = nextKind()
        val startedByCurrent = coolDown.report(currentProbe, Outcome.FAILED)

        assertNull(startedByEarlier)
        assertEquals(Kind.SKIPPED, whileCurrentProbes)
        assertEquals(PAUSE_MILLIS * 2, startedByCurrent)
    }

    @Test
    fun `cache-only request is admitted during a pause, takes no probe slot and cannot pause the mirror`() {
        val whileOpen = admit(cacheOnly = true)
        val startedWhileOpen = coolDown.report(whileOpen, Outcome.FAILED)
        val afterItsFailure = nextKind()

        coolDown.report(admit(), Outcome.FAILED)
        val duringThePause = admit(cacheOnly = true)
        nowMillis += PAUSE_MILLIS
        val afterThePause = admit(cacheOnly = true)
        val reported = listOf(duringThePause, afterThePause).flatMap { admission ->
            listOf(Outcome.FAILED, Outcome.NO_VERDICT).map { coolDown.report(admission, it) }
        }

        assertNull(startedWhileOpen)
        assertEquals(Kind.OPEN, afterItsFailure)
        assertEquals(listOf(Kind.OPEN, Kind.OPEN, Kind.OPEN), listOf(whileOpen, duringThePause, afterThePause).map(::kindOf))
        assertTrue(reported.all { it == null })
        assertEquals("the probe slot is still free", Kind.PROBE, nextKind())
    }

    @Test
    fun `only one of the requests that arrive together after a pause becomes the probe`() {
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)

        try {
            repeat(ROUNDS) {
                nowMillis = 0
                val coolDown = NovaUtilsMirrorCoolDown(PAUSE_MILLIS, MAX_PAUSE_MILLIS) { nowMillis }
                coolDown.report(coolDown.admit(cacheOnly = false), Outcome.FAILED)
                nowMillis += PAUSE_MILLIS
                val start = CyclicBarrier(CONCURRENT_REQUESTS)
                val requests = List(CONCURRENT_REQUESTS) {
                    Callable {
                        start.await()
                        kindOf(coolDown.admit(cacheOnly = false))
                    }
                }

                val kinds = executor.invokeAll(requests).map { it.get() }

                assertEquals(1, kinds.count { it == Kind.PROBE })
                assertEquals(CONCURRENT_REQUESTS - 1, kinds.count { it == Kind.SKIPPED })
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private companion object {

        const val PAUSE_MILLIS = 60_000L

        const val MAX_PAUSE_MILLIS = PAUSE_MILLIS * 4

        const val CONCURRENT_REQUESTS = 8

        const val ROUNDS = 200
    }
}
