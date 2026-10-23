package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_wallet_api.domain.model.BittensorServiceFeeRecipients
import io.novafoundation.nova.feature_wallet_impl.data.network.model.response.SubqueryHistoryElementResponse.Query.HistoryElements.Node
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

/**
 * Mirrors iOS SubtensorHistoryFiltersProvider:
 * - successful transfers to current/historical Nova fee recipients are hidden, including standalone ones;
 * - transfers from/to a subnet account (`modlsubtensr` prefix) are hidden when they carry an extrinsic
 *   hash, i.e. they happened inside an extrinsic (stake, unstake, a batch with the Nova fee). The staking
 *   extrinsic itself stays visible. Transfers without a hash keep showing.
 */
internal fun filterBittensorTechnicalTransfers(
    nodes: List<Node>,
    chain: Chain,
    asset: Chain.Asset,
    feeRecipientAddresses: List<String> = BittensorServiceFeeRecipients.historyAddresses
): List<Node> {
    if (!supportsBittensorTransferFiltering(chain, asset)) return nodes

    val feeRecipients = feeRecipientAddresses.mapNotNull { chain.accountIdOrNull(it)?.let(::AccountIdKey) }.toSet()

    return nodes.filter { node ->
        if (!node.isSuccessfulNativeTransfer()) return@filter true
        val transfer = requireNotNull(node.transfer)

        val recipient = chain.accountIdOrNull(transfer.to)?.let(::AccountIdKey)
        if (recipient in feeRecipients) return@filter false

        if (!node.hasExtrinsicHash()) return@filter true

        !chain.isSubnetAccount(transfer.from) && !chain.isSubnetAccount(transfer.to)
    }
}

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

private fun Node.hasExtrinsicHash(): Boolean {
    val hash = extrinsicHash?.removePrefix("0x") ?: return false
    return hash.length == 64 && hash.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
}

private fun Chain.isSubnetAccount(address: String): Boolean {
    val accountId = accountIdOrNull(address) ?: return false
    return accountId.size == 32 && SUBNET_ACCOUNT_PREFIX.indices.all { accountId[it] == SUBNET_ACCOUNT_PREFIX[it] }
}

private val SUBNET_ACCOUNT_PREFIX = "modlsubtensr".toByteArray(Charsets.UTF_8)
