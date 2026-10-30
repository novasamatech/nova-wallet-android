package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationKind
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOutcomeTarget
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findEvents
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import java.math.BigInteger

private const val SUBTENSOR = "SubtensorModule"

private const val STAKE_CHANGE_FIELDS = 6

private const val ALPHA_FEE_FIELDS = 4

private const val CLAIM_FIELDS = 3

private const val ROOT_CLAIM_FIELDS = 2

private const val TRANSFER_FIELDS = 3

private const val NATIVE_FEE_FIELDS = 3

/**
 * Tuple shapes follow SubtensorPallet+Events and SubtensorEventMatcher at iOS d17ee70c.
 * Fee-sale exclusion follows the first matching StakeRemoved rule in that parser.
 */
fun bindBittensorOperationOutcome(
    events: List<GenericEvent.Instance>,
    target: BittensorOutcomeTarget,
    coldkey: AccountIdKey,
    feePayer: AccountIdKey?,
    blockHash: String
): BittensorOperationOutcome {
    val alphaFeeEvents = events.findEvents(SUBTENSOR, "TransactionFeePaidWithAlpha")
    val hasAlphaFee = alphaFeeEvents.isNotEmpty()
    val alphaFee = bindAlphaFee(alphaFeeEvents, feePayer)
    val changes = if (hasAlphaFee && alphaFee == null && target.kind == BittensorOperationKind.UNSTAKE) {
        null
    } else {
        bindStakeChanges(events, target, coldkey, alphaFee?.takeIf { feePayer == coldkey })
    }
    val alphaChanges = changes?.takeIf { it.all { change -> change.alpha != null } }

    return BittensorOperationOutcome(
        blockHash = blockHash,
        tao = changes?.sumOf { it.tao },
        alpha = alphaChanges?.sumOf { requireNotNull(it.alpha) },
        serviceFee = bindServiceFee(events, target, coldkey),
        networkFee = alphaFee?.taoAmount ?: bindNativeFee(events, feePayer, hasAlphaFee),
        feePayer = feePayer,
        alphaFee = alphaFee?.alpha,
        netuid = target.netuid,
        coldkey = coldkey,
        alphaFeeNetuid = alphaFee?.netuid
    )
}

private fun bindStakeChanges(
    events: List<GenericEvent.Instance>,
    target: BittensorOutcomeTarget,
    coldkey: AccountIdKey,
    alphaFee: AlphaFee?
): List<StakeChange>? = runCatching {
    if (target.hotkeys.isEmpty()) return@runCatching null
    if (target.kind == BittensorOperationKind.CLAIM && !target.netuid.isRoot()) return@runCatching null

    val eventName = when (target.kind) {
        BittensorOperationKind.STAKE -> "StakeAdded"
        BittensorOperationKind.UNSTAKE -> "StakeRemoved"
        BittensorOperationKind.CLAIM -> "BasketClaimed"
    }
    val operationEvents = events.findEvents(SUBTENSOR, eventName)
    val feeSaleIndex = if (target.kind == BittensorOperationKind.UNSTAKE && alphaFee != null) {
        operationEvents.indexOfFirst { event -> matchesFeeSale(event.arguments, coldkey, alphaFee.netuid) }
    } else {
        null
    }
    val matched = operationEvents
        .mapIndexedNotNull { index, event ->
            if (index == feeSaleIndex) return@mapIndexedNotNull null
            if (target.kind == BittensorOperationKind.CLAIM) {
                bindClaimChange(event.arguments, target, coldkey)
            } else {
                bindTradeChange(event.arguments, target, coldkey)
            }
        }

    val canUseRootClaim = target.kind == BittensorOperationKind.CLAIM && matched.isEmpty() && operationEvents.none { event ->
        val (_, eventColdkey) = event.arguments
        bindOutcomeAccount(eventColdkey) == coldkey
    }
    if (canUseRootClaim) {
        return@runCatching bindRootClaimChange(events, target, coldkey)?.let(::listOf)
    }
    // One call may emit several changes for the same hotkey. Still require all reviewed targets.
    matched.takeIf { it.map { change -> change.hotkey }.toSet() == target.hotkeys }
}.getOrNull()

private fun matchesFeeSale(args: List<Any?>, coldkey: AccountIdKey, netuid: BittensorNetuid): Boolean {
    require(args.size == STAKE_CHANGE_FIELDS)
    val (eventColdkey, _, _, _, eventNetuid) = args
    return bindOutcomeAccount(eventColdkey) == coldkey && bindNumber(eventNetuid) == BigInteger.valueOf(netuid.value.toLong())
}

private fun bindRootClaimChange(events: List<GenericEvent.Instance>, target: BittensorOutcomeTarget, coldkey: AccountIdKey): StakeChange? {
    // The root claim builder submits one claim_root_with_hotkey; no basket evidence may conflict.
    val hotkey = target.hotkeys.singleOrNull() ?: return null
    val claims = events.findEvents(SUBTENSOR, "RootClaimed").mapNotNull { event ->
        require(event.arguments.size == ROOT_CLAIM_FIELDS)
        val (account, tao) = event.arguments
        if (bindOutcomeAccount(account) == coldkey) bindOutcomeAmount(tao) else null
    }
    return claims.singleOrNull()?.let { StakeChange(hotkey, it, null) }
}

private fun bindClaimChange(args: List<Any?>, target: BittensorOutcomeTarget, coldkey: AccountIdKey): StakeChange? {
    require(args.size == CLAIM_FIELDS)
    val (eventHotkey, eventColdkey, tao) = args
    val hotkey = bindOutcomeAccount(eventHotkey)
    if (bindOutcomeAccount(eventColdkey) != coldkey || hotkey !in target.hotkeys) return null

    return StakeChange(hotkey, bindOutcomeAmount(tao), null)
}

private fun bindTradeChange(args: List<Any?>, target: BittensorOutcomeTarget, coldkey: AccountIdKey): StakeChange? {
    require(args.size == STAKE_CHANGE_FIELDS)
    val (eventColdkey, eventHotkey, tao, eventAlpha, netuid) = args
    val hotkey = bindOutcomeAccount(eventHotkey)
    if (bindOutcomeAccount(eventColdkey) != coldkey || hotkey !in target.hotkeys || !matchesNetuid(netuid, target)) return null

    val fee = if (target.kind == BittensorOperationKind.UNSTAKE) bindOutcomeAmount(args.last()) else BigInteger.ZERO
    val alpha = bindOutcomeAmount(eventAlpha) + fee
    return StakeChange(hotkey, bindOutcomeAmount(tao), alpha)
}

private fun bindServiceFee(events: List<GenericEvent.Instance>, target: BittensorOutcomeTarget, coldkey: AccountIdKey): BigInteger? = runCatching {
    val beneficiary = target.serviceFeeBeneficiary ?: return@runCatching null
    events.findEvents("Balances", "Transfer")
        .mapNotNull { event ->
            val args = event.arguments
            require(args.size == TRANSFER_FIELDS)
            val (sender, receiver, value) = args
            if (bindOutcomeAccount(sender) == coldkey && bindOutcomeAccount(receiver) == beneficiary) bindOutcomeAmount(value) else null
        }
        .takeIf { it.isNotEmpty() }
        ?.fold(BigInteger.ZERO, BigInteger::add)
}.getOrNull()

private fun bindNativeFee(events: List<GenericEvent.Instance>, feePayer: AccountIdKey?, hasAlphaFee: Boolean): BigInteger? = runCatching {
    if (feePayer == null || hasAlphaFee) return@runCatching null
    val feeEvent = events.findEvents("TransactionPayment", "TransactionFeePaid")
        .singleOrNull() ?: return@runCatching null
    val args = feeEvent.arguments
    require(args.size == NATIVE_FEE_FIELDS)
    val (payer, actualFee) = args
    if (bindOutcomeAccount(payer) != feePayer) return@runCatching null

    // iOS TransactionPaymentPallet reads the reported amount directly, without adding tip.
    bindOutcomeAmount(actualFee)
}.getOrNull()

private fun bindAlphaFee(events: List<GenericEvent.Instance>, feePayer: AccountIdKey?): AlphaFee? = runCatching {
    val args = events.singleOrNull()?.arguments ?: return@runCatching null
    require(args.size == ALPHA_FEE_FIELDS)
    val (payer, netuid, alpha, taoAmount) = args
    if (bindOutcomeAccount(payer) != feePayer) return@runCatching null
    val feeNetuid = bindOutcomeAmount(netuid)
    require(feeNetuid <= BigInteger.valueOf(UShort.MAX_VALUE.toLong()))
    AlphaFee(BittensorNetuid.from(feeNetuid.toInt()), bindOutcomeAmount(alpha), bindOutcomeAmount(taoAmount))
}.getOrNull()

private fun bindOutcomeAccount(value: Any?): AccountIdKey = AccountIdKey(bindAccountIdentifier(value))

private fun bindOutcomeAmount(value: Any?): BigInteger = bindNumber(value).also { require(it.isNonNegative) }

private fun matchesNetuid(value: Any?, target: BittensorOutcomeTarget): Boolean =
    bindNumber(value) == BigInteger.valueOf(target.netuid.value.toLong())

private data class StakeChange(val hotkey: AccountIdKey, val tao: BigInteger, val alpha: BigInteger?)

private data class AlphaFee(val netuid: BittensorNetuid, val alpha: BigInteger, val taoAmount: BigInteger)
