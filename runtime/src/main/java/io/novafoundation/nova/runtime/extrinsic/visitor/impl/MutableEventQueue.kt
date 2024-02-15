package io.novafoundation.nova.runtime.extrinsic.visitor.impl

import io.novafoundation.nova.common.utils.instanceOf
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Event

internal interface MutableEventQueue : EventQueue {

    /**
     * Removes last event matching one of eventTypes
     */
    fun popFromEnd(vararg eventTypes: Event)

    /**
     * Takes and removes all events that go after last event matching one of eventTypes. If no matched event found,
     * all available events are returned
     */
    fun takeTail(vararg eventTypes: Event): List<GenericEvent.Instance>

    /**
     * Takes and removes all events that go after specified inclusive index
     * @param endInclusive
     */
    fun takeAllAfterInclusive(endInclusive: Int): List<GenericEvent.Instance>

    /**
     * Takes and removes last event matching one of eventTypes
     */
    fun takeFromEnd(vararg eventTypes: Event): GenericEvent.Instance?
}

internal interface EventQueue {

    fun all(): List<GenericEvent.Instance>

    fun peekItemFromEnd(vararg eventTypes: Event, endExclusive: Int): EventWithIndex?

    fun indexOfLast(vararg eventTypes: Event, endExclusive: Int): Int?
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun EventQueue.peekItemFromEndOrThrow(vararg eventTypes: Event, endExclusive: Int): EventWithIndex {
    return requireNotNull(peekItemFromEnd(*eventTypes, endExclusive = endExclusive)) {
        "No required event found for types ${eventTypes.joinToString { it.name }}"
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun MutableEventQueue.takeFromEndOrThrow(vararg eventTypes: Event): GenericEvent.Instance {
    return requireNotNull(takeFromEnd(*eventTypes)) {
        "No required event found for types ${eventTypes.joinToString { it.name }}"
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun EventQueue.indexOfLastOrThrow(vararg eventTypes: Event, endExclusive: Int): Int {
    return requireNotNull(indexOfLast(*eventTypes, endExclusive = endExclusive)) {
        "No required event found for types ${eventTypes.joinToString { it.name }}"
    }
}

data class EventWithIndex(val event: GenericEvent.Instance, val eventIndex: Int)

class RealEventQueue(event: List<GenericEvent.Instance>) : MutableEventQueue {

    private val events: MutableList<GenericEvent.Instance> = event.toMutableList()

    override fun all(): List<GenericEvent.Instance> {
        return events
    }

    override fun peekItemFromEnd(vararg eventTypes: Event, endExclusive: Int): EventWithIndex? {
        return findEventAndIndex(eventTypes, endExclusive)
    }

    override fun indexOfLast(vararg eventTypes: Event, endExclusive: Int): Int? {
        return findEventAndIndex(eventTypes, endExclusive)?.eventIndex
    }

    override fun popFromEnd(vararg eventTypes: Event) {
        takeFromEnd(*eventTypes)
    }

    override fun takeTail(vararg eventTypes: Event): List<GenericEvent.Instance> {
        val eventWithIndex = this.findEventAndIndex(eventTypes)

        return if (eventWithIndex != null) {
            this.removeAllAfterExclusive(eventWithIndex.eventIndex)
        } else {
            this.removeAllAfterInclusive(0)
        }
    }

    override fun takeAllAfterInclusive(endInclusive: Int): List<GenericEvent.Instance> {
        return removeAllAfterInclusive(endInclusive)
    }

    override fun takeFromEnd(vararg eventTypes: Event): GenericEvent.Instance? {
        return findEventAndIndex(eventTypes)?.let { (event, index) ->
            removeAllAfterInclusive(index)

            event
        }
    }

    private fun findEventAndIndex(eventTypes: Array<out Event>, endExclusive: Int = this.events.size): EventWithIndex? {
        val eventsQueue = this.events
        val limit = endExclusive.coerceAtMost(eventsQueue.size)

        for (i in (limit - 1) downTo 0) {
            val nextEvent = eventsQueue[i]

            eventTypes.forEach { event ->
                if (nextEvent.instanceOf(event)) return EventWithIndex(nextEvent, i)
            }
        }

        return null
    }

    private fun removeAllAfterInclusive(index: Int): List<GenericEvent.Instance> {
        if (index > this.events.size) return emptyList()

        val subList = this.events.subList(index, this.events.size)
        val subListCopy = subList.toList()

        subList.clear()

        return subListCopy
    }

    private fun removeAllAfterExclusive(index: Int): List<GenericEvent.Instance> {
        val subList = this.events.subList(index + 1, this.events.size)
        val subListCopy = subList.toList()

        subList.clear()

        return subListCopy
    }
}
