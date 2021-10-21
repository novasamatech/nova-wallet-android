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
) = updateAsset(metaId, chainAsset, accountInfoUpdater(accountInfo))

suspend fun AssetCache.updateAsset(
    accountId: AccountId,
    chainAsset: Chain.Asset,
    accountInfo: AccountInfo,
) = updateAsset(accountId, chainAsset, accountInfoUpdater(accountInfo))

private fun accountInfoUpdater(accountInfo: AccountInfo) = { asset: AssetLocal ->
    val data = accountInfo.data

    asset.copy(
        freeInPlanks = data.free,
        reservedInPlanks = data.reserved,
        miscFrozenInPlanks = data.miscFrozen,
        feeFrozenInPlanks = data.feeFrozen
    )
}

fun bindAccountInfoOrDefault(hex: String?, runtime: RuntimeSnapshot): AccountInfo {
    return hex?.let { bindAccountInfo(it, runtime) } ?: AccountInfo.empty()
}
