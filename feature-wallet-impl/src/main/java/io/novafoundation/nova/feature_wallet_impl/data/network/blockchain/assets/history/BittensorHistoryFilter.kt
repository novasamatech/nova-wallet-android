package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.domain.model.BittensorServiceFeeRecipients
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.network.model.response.SubqueryHistoryElementResponse.Query.HistoryElements.Node
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

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

    val feeRecipients = chain.feeRecipientIds(feeRecipientAddresses)

    return nodes.filter { node ->
        if (!node.isSuccessfulNativeTransfer()) return@filter true
        val transfer = requireNotNull(node.transfer)

        !isTechnicalTransfer(
            sender = chain.accountIdOrNull(transfer.from),
            recipient = chain.accountIdOrNull(transfer.to),
            withinExtrinsic = isExtrinsicHash(node.extrinsicHash),
            feeRecipients = feeRecipients
        )
    }
}

/**
 * The same rules for transfers read from a block, so the batched Nova fee is not stored
 * as a history row before SubQuery returns the extrinsic.
 */
internal fun filterBittensorTechnicalRealtimeTransfers(
    updates: List<RealtimeHistoryUpdate>,
    chain: Chain,
    asset: Chain.Asset,
    feeRecipientAddresses: List<String> = BittensorServiceFeeRecipients.historyAddresses
): List<RealtimeHistoryUpdate> {
    if (!supportsBittensorTransferFiltering(chain, asset)) return updates

    val feeRecipients = chain.feeRecipientIds(feeRecipientAddresses)

    return updates.filter { update ->
        val transfer = update.type as? RealtimeHistoryUpdate.Type.Transfer ?: return@filter true
        if (update.status != Operation.Status.COMPLETED) return@filter true

        !isTechnicalTransfer(
            sender = transfer.senderId,
            recipient = transfer.recipientId,
            withinExtrinsic = isExtrinsicHash(update.txHash),
            feeRecipients = feeRecipients
        )
    }
}

private fun supportsBittensorTransferFiltering(chain: Chain, asset: Chain.Asset): Boolean {
    return chain.id == ChainGeneses.BITTENSOR &&
        asset.type == Chain.Asset.Type.Native &&
        Chain.Asset.StakingType.BITTENSOR_ROOT in asset.staking
}

private fun Chain.feeRecipientIds(addresses: List<String>): Set<AccountIdKey> {
    return addresses.mapNotNull { accountIdOrNull(it)?.let(::AccountIdKey) }.toSet()
}

private fun isTechnicalTransfer(
    sender: AccountId?,
    recipient: AccountId?,
    withinExtrinsic: Boolean,
    feeRecipients: Set<AccountIdKey>
): Boolean {
    if (recipient != null && AccountIdKey(recipient) in feeRecipients) return true

    if (!withinExtrinsic) return false

    return sender.isSubnetAccount() || recipient.isSubnetAccount()
}

private fun Node.isSuccessfulNativeTransfer(): Boolean {
    return transfer?.success == true &&
        reward == null &&
        poolReward == null &&
        assetTransfer == null &&
        swap == null
}

private fun isExtrinsicHash(value: String?): Boolean {
    val hash = value?.removePrefix("0x") ?: return false
    return hash.length == 64 && hash.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
}

private fun AccountId?.isSubnetAccount(): Boolean {
    if (this == null) return false
    return size == 32 && SUBNET_ACCOUNT_PREFIX.indices.all { this[it] == SUBNET_ACCOUNT_PREFIX[it] }
}

private val SUBNET_ACCOUNT_PREFIX = "modlsubtensr".toByteArray(Charsets.UTF_8)
