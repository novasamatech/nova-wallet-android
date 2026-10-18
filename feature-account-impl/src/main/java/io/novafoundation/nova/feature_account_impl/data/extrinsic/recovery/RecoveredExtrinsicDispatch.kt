package io.novafoundation.nova.feature_account_impl.data.extrinsic.recovery

import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.RecoveredExtrinsicOutcome
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
    val proxyResult = nestedDispatchOutcome(
        tree,
        events,
        "Proxy",
        "ProxyExecuted",
        resultIndex = 0
    )
    if (proxyResult == RecoveredExtrinsicOutcome.FAILURE) return proxyResult

    val multisigResult = nestedDispatchOutcome(
        tree,
        events,
        "Multisig",
        "MultisigExecuted",
        resultIndex = 4
    )
    if (multisigResult == RecoveredExtrinsicOutcome.FAILURE) return multisigResult
    if (events.any { it.module.name == "Utility" && it.event.name in setOf("BatchInterrupted", "ItemFailed") }) {
        return RecoveredExtrinsicOutcome.FAILURE
    }
    return RecoveredExtrinsicOutcome.SUCCESS
}

private fun nestedDispatchOutcome(
    calls: List<GenericCall.Instance>,
    events: List<GenericEvent.Instance>,
    module: String,
    eventName: String,
    resultIndex: Int
): RecoveredExtrinsicOutcome {
    val expectedExecutions = calls.count { it.module.name == module }
    val executionEvents = events.filter { it.module.name == module && it.event.name == eventName }
    require(executionEvents.size >= expectedExecutions) { "$module execution evidence is unavailable" }
    for (event in executionEvents) {
        val result = event.arguments[resultIndex].castToDictEnum()
        when (result.name) {
            "Err" -> return RecoveredExtrinsicOutcome.FAILURE
            "Ok" -> Unit
            else -> error("Unknown $module dispatch result")
        }
    }
    return RecoveredExtrinsicOutcome.SUCCESS
}
