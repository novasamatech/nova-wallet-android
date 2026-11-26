package io.novafoundation.nova.common.data.network

import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

// How many times longer the next pause is while the mirror keeps failing.
private const val PAUSE_GROWTH_FACTOR = 2

/**
 * The pause of the nova-utils mirror after a mirror-wide failure: decides whether a request may try the mirror.
 *
 * While the mirror is open every request tries it, and the first [Outcome.FAILED] starts a pause of
 * [initialMillis]. During a pause requests are [Admission.Skipped]. When it ends, one request is admitted as
 * the [Admission.Probe] and the others stay skipped until the probe reports back:
 * - [Outcome.ANSWERED] ends the pause for everyone, and the next pause starts from [initialMillis] again;
 * - [Outcome.FAILED] starts the next pause, [PAUSE_GROWTH_FACTOR] times longer, up to [maxMillis];
 * - [Outcome.NO_VERDICT] gives the probe slot back, so the next request probes.
 *
 * The state lives in memory only: a new process tries the mirror again.
 *
 * @param clock monotonic time in milliseconds.
 */
internal class NovaUtilsMirrorCoolDown(
    private val initialMillis: Long,
    private val maxMillis: Long,
    private val clock: () -> Long
) {

    /**
     * What a request may do with the mirror. A request that tried the mirror reports back through [report];
     * a [Probe] must always do so, otherwise the mirror stays skipped.
     */
    sealed interface Admission {

        /** The mirror may be tried. */
        class Open : Admission

        /** The mirror may be tried, and this request is the one that decides whether the pause ends. */
        class Probe : Admission

        /** The mirror is paused: read GitHub. */
        object Skipped : Admission
    }

    /** What an attempt has shown about the mirror as a whole. */
    enum class Outcome {

        /** The mirror answered over the network with a status that does not start a pause. */
        ANSWERED,

        /** A mirror-wide failure: the mirror is to be paused. */
        FAILED,

        /** Nothing was learned: an answer from a local cache, a cancelled call, a device without connectivity. */
        NO_VERDICT
    }

    private sealed interface State {

        /**
         * @param ticket handed to every request admitted under this state. A failure starts a pause only while its
         * ticket is the current one, so requests that were in flight together start one pause, not several.
         */
        class Open(val ticket: Admission.Open) : State

        /**
         * @param lengthMillis length of this pause, the base of the next one.
         * @param probe the request probing the mirror after [untilMillis], or null while the slot is free.
         */
        class Paused(val untilMillis: Long, val lengthMillis: Long, val probe: Admission.Probe?) : State
    }

    private val state = AtomicReference<State>(State.Open(Admission.Open()))

    /**
     * @param cacheOnly the request cannot reach the network. It is never skipped, as it costs no wait and its file
     * may be cached under the mirror URL, and it never becomes the probe, as it cannot bring a verdict.
     */
    fun admit(cacheOnly: Boolean): Admission {
        // A ticket of its own: no state holds it, so whatever is reported with it changes nothing.
        if (cacheOnly) return Admission.Open()

        while (true) {
            val current = state.get()
            val paused = when (current) {
                is State.Open -> return current.ticket
                is State.Paused -> current
            }

            val isProbeDue = clock() >= paused.untilMillis && paused.probe == null
            if (!isProbeDue) return Admission.Skipped

            val probe = Admission.Probe()
            val probing = State.Paused(paused.untilMillis, paused.lengthMillis, probe)
            if (state.compareAndSet(paused, probing)) return probe
            // Another request changed the state in between: look again.
        }
    }

    /**
     * @return the length of the pause this report has started, or null when it started none.
     */
    fun report(admission: Admission, outcome: Outcome): Long? {
        return when (outcome) {
            Outcome.ANSWERED -> {
                endPause()
                null
            }
            Outcome.FAILED -> startPause(admission)
            Outcome.NO_VERDICT -> {
                releaseProbe(admission)
                null
            }
        }
    }

    // An answer is evidence whoever brought it, a request admitted before the pause included.
    private fun endPause() {
        state.updateAndGet { current ->
            when (current) {
                is State.Open -> current
                is State.Paused -> State.Open(Admission.Open())
            }
        }
    }

    private fun startPause(admission: Admission): Long? {
        val current = state.get()
        val lengthMillis = when {
            current is State.Open && current.ticket === admission -> initialMillis
            current is State.Paused && current.probe === admission -> min(current.lengthMillis * PAUSE_GROWTH_FACTOR, maxMillis)
            // Admitted under a state that is gone: another request has already reported for it.
            else -> return null
        }
        val paused = State.Paused(untilMillis = clock() + lengthMillis, lengthMillis = lengthMillis, probe = null)

        // Losing the race means the state was replaced by another report, which then stands.
        return lengthMillis.takeIf { state.compareAndSet(current, paused) }
    }

    private fun releaseProbe(admission: Admission) {
        val current = state.get()
        if (current !is State.Paused || current.probe !== admission) return

        state.compareAndSet(current, State.Paused(current.untilMillis, current.lengthMillis, probe = null))
    }
}
