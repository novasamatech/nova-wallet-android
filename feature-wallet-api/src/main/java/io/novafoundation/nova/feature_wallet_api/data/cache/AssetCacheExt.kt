package io.novafoundation.nova.feature_wallet_api.data.cache

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot

suspend fun AssetCache.updateAsset(
    metaId: Long,
    chainAsset: Chain.Asset,
    accountInfo: AccountInfo,
) = updateAsset(metaId, chainAsset, nativeBalanceUpdater(accountInfo))

suspend fun AssetCache.updateAsset(
    accountId: AccountId,
    chainAsset: Chain.Asset,
    accountInfo: AccountInfo,
) = updateAsset(accountId, chainAsset, nativeBalanceUpdater(accountInfo))

private fun nativeBalanceUpdater(accountInfo: AccountInfo) = { asset: AssetLocal ->
    val data = accountInfo.data

    val frozen = data.miscFrozen.max(data.feeFrozen)

    asset.copy(
        freeInPlanks = data.free,
        frozenInPlanks = frozen,
        reservedInPlanks = accountInfo.data.reserved
    )
}

fun bindAccountInfoOrDefault(hex: String?, runtime: RuntimeSnapshot): AccountInfo {
    return hex?.let { bindAccountInfo(it, runtime) } ?: AccountInfo.empty()
}
