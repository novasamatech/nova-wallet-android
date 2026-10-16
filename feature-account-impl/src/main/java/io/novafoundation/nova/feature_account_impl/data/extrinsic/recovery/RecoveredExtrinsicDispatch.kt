package io.novafoundation.nova.feature_account_impl.data.extrinsic.recovery

import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.RecoveredExtrinsicOutcome
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicStatus
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

internal fun containsMultisigCall(call: GenericCall.Instance): Boolean = extrinsicCallTree(call).any { it.module.name == "Multisig" }

private fun extrinsicCallTree(call: GenericCall.Instance): List<GenericCall.Instance> = buildList {
    add(call)
    call.arguments.values.forEach { argument ->
        when (argument) {
            is GenericCall.Instance -> addAll(extrinsicCallTree(argument))
            is List<*> -> argument.filterIsInstance<GenericCall.Instance>().forEach { addAll(extrinsicCallTree(it)) }
        }
    }
}

/** System success proves envelope acceptance; proxy/multisig need their own execution result. */
internal fun recoveredExtrinsicDispatch(
    call: GenericCall.Instance,
    systemStatus: ExtrinsicStatus,
    events: List<GenericEvent.Instance>
): RecoveredExtrinsicOutcome {
    if (systemStatus == ExtrinsicStatus.FAILURE) return RecoveredExtrinsicOutcome.FAILURE
    val tree = extrinsicCallTree(call)
    val proxies = tree.count { it.module.name == "Proxy" }
    val proxyEvents = events.filter { it.module.name == "Proxy" && it.event.name == "ProxyExecuted" }
    require(proxyEvents.size >= proxies) { "Proxy execution evidence is unavailable" }
    proxyEvents.forEach { event ->
        when (event.arguments.first().castToDictEnum().name) {
            "Err" -> return RecoveredExtrinsicOutcome.FAILURE
            "Ok" -> Unit
            else -> error("Unknown proxy dispatch result")
        }
    }
    val multisigs = tree.count { it.module.name == "Multisig" }
    val multisigEvents = events.filter { it.module.name == "Multisig" && it.event.name == "MultisigExecuted" }
    require(multisigEvents.size >= multisigs) { "Multisig approval is not proof of staking execution" }
    multisigEvents.forEach { event ->
        when (event.arguments[4].castToDictEnum().name) {
            "Err" -> return RecoveredExtrinsicOutcome.FAILURE
            "Ok" -> Unit
            else -> error("Unknown multisig dispatch result")
        }
    }
    if (events.any { it.module.name == "Utility" && it.event.name in setOf("BatchInterrupted", "ItemFailed") }) {
        return RecoveredExtrinsicOutcome.FAILURE
    }
    return RecoveredExtrinsicOutcome.SUCCESS
}
