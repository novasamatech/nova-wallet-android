package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_wallet_api.domain.model.BittensorServiceFeeRecipients
import io.novafoundation.nova.feature_wallet_impl.data.network.model.response.SubqueryHistoryElementResponse.Query.HistoryElements.Node
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

/**
 * Successful transfers to current/historical Nova fee recipients are hidden,
 * including standalone transfers, matching the approved iOS recipient rule.
 *
 * For subnet-prefix filtering, hash alone does not prove a technical transfer.
 * Require a successful direct staking call in the same page, block and account
 * context. Missing/cross-page evidence keeps non-fee-recipient transfers.
 *
 * The separate subnet-prefix rule remains conservative: the current client DTO
 * has no nested calls, so utility/proxy/multisig prefix transfers remain visible.
 * Synthetic fixtures cover this contract; live Bittensor indexer co-occurrence
 * of the extrinsic and transfer rows has not been established.
 */
internal fun filterBittensorTechnicalTransfers(
    nodes: List<Node>,
    chain: Chain,
    asset: Chain.Asset,
    feeRecipientAddresses: List<String> = BittensorServiceFeeRecipients.historyAddresses
): List<Node> {
    if (!supportsBittensorTransferFiltering(chain, asset)) return nodes

    val feeRecipients = feeRecipientAddresses.mapNotNull { chain.accountIdOrNull(it)?.let(::AccountIdKey) }.toSet()
    val extrinsics = nodes.filter { it.extrinsic != null }
        .groupBy { it.extrinsicContext() }

    return nodes.filter { node ->
        if (!node.isSuccessfulNativeTransfer()) return@filter true
        val transfer = requireNotNull(node.transfer)

        val recipient = chain.accountIdOrNull(transfer.to)?.let(::AccountIdKey)
        if (recipient in feeRecipients) return@filter false

        if (node.extrinsic != null) return@filter true
        val context = node.extrinsicContext() ?: return@filter true
        val parents = extrinsics[context].orEmpty()
        if (parents.isEmpty() || parents.any { !it.isSuccessfulDirectStakeCall() }) return@filter true

        !chain.isSubnetAccount(transfer.from) && !chain.isSubnetAccount(transfer.to)
    }
}

private data class ExtrinsicContext(val hash: String, val blockNumber: Long, val address: String)

private fun supportsBittensorTransferFiltering(chain: Chain, asset: Chain.Asset): Boolean {
    return chain.id == ChainGeneses.BITTENSOR &&
        asset.type == Chain.Asset.Type.Native &&
        Chain.Asset.StakingType.BITTENSOR_ROOT in asset.staking
}

private fun Node.isSuccessfulNativeTransfer(): Boolean {
    return transfer?.success == true &&
        reward == null &&
        poolReward == null &&
        assetTransfer == null &&
        swap == null
}

private fun Node.extrinsicContext(): ExtrinsicContext? {
    val hash = extrinsicHash?.removePrefix("0x") ?: return null
    if (hash.length != 64 || hash.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) return null
    val accountAddress: String? = address
    // Gson supplies zero for an absent primitive blockNumber. It is not evidence.
    if (blockNumber <= 0 || accountAddress.isNullOrBlank()) return null
    return ExtrinsicContext(hash.lowercase(), blockNumber, accountAddress)
}

private fun Node.isSuccessfulDirectStakeCall(): Boolean {
    val call = extrinsic ?: return false
    return call.success && call.module.equals("subtensorModule", ignoreCase = true) && call.call in DIRECT_STAKE_CALLS
}

private fun Chain.isSubnetAccount(address: String): Boolean {
    val accountId = accountIdOrNull(address) ?: return false
    return accountId.size == 32 && SUBNET_ACCOUNT_PREFIX.indices.all { accountId[it] == SUBNET_ACCOUNT_PREFIX[it] }
}

private val SUBNET_ACCOUNT_PREFIX = "modlsubtensr".toByteArray(Charsets.UTF_8)

private val DIRECT_STAKE_CALLS = setOf(
    "addStake", "add_stake",
    "addStakeLimit", "add_stake_limit",
    "removeStake", "remove_stake",
    "removeStakeLimit", "remove_stake_limit",
    "removeStakeFullLimit", "remove_stake_full_limit"
)
