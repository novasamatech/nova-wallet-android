package io.novafoundation.nova.runtime.multiNetwork.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.EventRecord
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

typealias EventRecords = List<EventRecord>

fun EventRecords.events(): List<GenericEvent.Instance> = map { it.event }
