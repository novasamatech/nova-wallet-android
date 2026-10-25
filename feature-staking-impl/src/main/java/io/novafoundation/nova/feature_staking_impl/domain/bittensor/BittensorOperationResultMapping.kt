package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.signer.isDelayed
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicWatchException
import kotlinx.coroutines.CancellationException
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvents
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError

val BittensorOperationResult.Failure.isSigningCancelled: Boolean
    get() = stage == BittensorFailureStage.NOT_SUBMITTED && cause is SigningCancelledException

/**
 * Whether Try again can succeed after a failed submission: a missing
 * subnet, a disabled subnet token, an unregistered hotkey, a coldkey swap, safe mode, the staking hotkey cap and a too
 * heavy root claim do not change on a retry.
 */
val BittensorOperationResult.Failure.isRetryable: Boolean
    get() {
        val error = cause as? DispatchError.Module ?: return true

        return (error.module.name to error.error.name) !in NON_RETRYABLE_DISPATCH_ERRORS
    }

private val NON_RETRYABLE_DISPATCH_ERRORS = setOf(
    "SubtensorModule" to "SubnetNotExists",
    "SubtensorModule" to "SubtokenDisabled",
    "Swap" to "SubtokenDisabled",
    "SubtensorModule" to "HotKeyAccountNotExists",
    "SubtensorModule" to "ColdkeySwapAnnounced",
    "SubtensorModule" to "ColdkeySwapDisputed",
    "SubtensorModule" to "TooManyStakingHotkeys",
    "SubtensorModule" to "RootClaimTooHeavy",
    "System" to "CallFiltered"
)

fun ExtrinsicExecutionResult.mapBittensorOperationResult(chain: Chain? = null): BittensorOperationResult = when (val dispatch = outcome) {
    is ExtrinsicDispatch.Failed -> BittensorOperationResult.Failure(dispatch.error, extrinsicHash, BittensorFailureStage.DISPATCHED)
    ExtrinsicDispatch.Unknown -> BittensorOperationResult.Unknown(extrinsicHash)
    is ExtrinsicDispatch.Ok -> {
        if (hasExecutedCallEvidence(dispatch, chain)) BittensorOperationResult.Success(extrinsicHash) else BittensorOperationResult.Unknown(extrinsicHash)
    }
}

private fun ExtrinsicExecutionResult.hasExecutedCallEvidence(dispatch: ExtrinsicDispatch.Ok, chain: Chain?): Boolean = runCatching {
    val path = submissionHierarchy.path
    if (path.isEmpty() || submissionHierarchy.isDelayed()) return@runCatching false
    if (path.first().account.type == LightMetaAccount.Type.PROXIED || path.first().account.type == LightMetaAccount.Type.MULTISIG) return@runCatching false
    val events = dispatch.emittedEvents
    if (events.any { it.instanceOf("Utility", "BatchInterrupted", "BatchCompletedWithErrors", "ItemFailed") }) return@runCatching false
    val wrappers = path.drop(1)
    val proxyNodes = wrappers.filter { it.account.type == LightMetaAccount.Type.PROXIED }
    val multisigNodes = wrappers.filter { it.account.type == LightMetaAccount.Type.MULTISIG }
    if (proxyNodes.size + multisigNodes.size != wrappers.size) return@runCatching false
    val proxies = events.findEvents("Proxy", "ProxyExecuted")
    val multisigs = events.findEvents("Multisig", "MultisigExecuted")
    if (proxies.size != proxyNodes.size || multisigs.size != multisigNodes.size) return@runCatching false
    // Same tuple contracts used by the existing runtime ProxyNode / MultisigNode walkers.
    if (proxies.any { it.arguments.size != PROXY_RESULT_FIELDS || it.arguments.single().castToDictEnum().name != "Ok" }) return@runCatching false
    val multisigAccounts = multisigNodes.map { node -> chain?.let { node.account.accountIdIn(it) }?.let(::AccountIdKey) }
    if (multisigAccounts.any { it == null } || multisigAccounts.distinct().size != multisigNodes.size) return@runCatching false
    multisigNodes.all { node ->
        val expected = chain?.let { node.account.accountIdIn(it) }
        expected != null && multisigs.count { event ->
            val args = event.arguments
            args.size == MULTISIG_RESULT_FIELDS &&
                bindAccountIdentifier(args[MULTISIG_ACCOUNT_INDEX]).contentEquals(expected) && args.last().castToDictEnum().name == "Ok"
        } == 1
    }
}.getOrDefault(false)

private const val PROXY_RESULT_FIELDS = 1
private const val MULTISIG_RESULT_FIELDS = 5
private const val MULTISIG_ACCOUNT_INDEX = 2

/** Submission/watch errors do not prove non-submission, even when no hash is available. */
fun Throwable.mapBittensorSubmissionError(): BittensorOperationResult.Unknown {
    if (this is CancellationException) throw this

    return BittensorOperationResult.Unknown((this as? ExtrinsicWatchException)?.status?.extrinsicHash)
}

/** Only for preparation/validation failures before invoking the submission API. */
fun Throwable.mapBittensorPreSubmissionError(): BittensorOperationResult.Failure {
    if (this is CancellationException) throw this

    return BittensorOperationResult.Failure(this, stage = BittensorFailureStage.NOT_SUBMITTED)
}
