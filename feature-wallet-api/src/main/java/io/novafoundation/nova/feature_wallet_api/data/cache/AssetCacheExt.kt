package io.novafoundation.nova.feature_wallet_api.data.cache

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.common.domain.balance.EDCountingMode
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.core_db.model.AssetLocal.EDCountingModeLocal
import io.novafoundation.nova.core_db.model.AssetLocal.TransferableModeLocal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot

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

suspend fun AssetCache.updateNonLockableAsset(
    metaId: Long,
    chainAsset: Chain.Asset,
    assetBalance: Balance,
) {
    updateAsset(metaId, chainAsset) {
        it.copy(
            freeInPlanks = assetBalance,
            frozenInPlanks = Balance.ZERO,
            reservedInPlanks = Balance.ZERO,
            transferableMode = TransferableModeLocal.REGULAR,
            edCountingMode = EDCountingModeLocal.TOTAL,
        )
    }
}

suspend fun AssetCache.updateFromChainBalance(
    metaId: Long,
    chainAssetBalance: ChainAssetBalance
) {
    updateAsset(metaId, chainAssetBalance.chainAsset) {
        it.copy(
            freeInPlanks = chainAssetBalance.free,
            frozenInPlanks = chainAssetBalance.frozen,
            reservedInPlanks = chainAssetBalance.reserved,
            transferableMode = chainAssetBalance.transferableMode.toLocal(),
            edCountingMode = chainAssetBalance.edCountingMode.toLocal()
        )
    }
}

fun TransferableMode.toLocal(): TransferableModeLocal {
    return when (this) {
        TransferableMode.REGULAR -> TransferableModeLocal.REGULAR
        TransferableMode.HOLDS_AND_FREEZES -> TransferableModeLocal.HOLDS_AND_FREEZES
    }
}

fun EDCountingMode.toLocal(): EDCountingModeLocal {
    return when (this) {
        EDCountingMode.TOTAL -> EDCountingModeLocal.TOTAL
        EDCountingMode.FREE -> EDCountingModeLocal.FREE
    }
}

private fun nativeBalanceUpdater(accountInfo: AccountInfo) = { asset: AssetLocal ->
    val data = accountInfo.data

    val transferableMode: TransferableModeLocal
    val edCountingMode: EDCountingModeLocal

    if (data.flags.holdsAndFreezesEnabled()) {
        transferableMode = TransferableModeLocal.HOLDS_AND_FREEZES
        edCountingMode = EDCountingModeLocal.FREE
    } else {
        transferableMode = TransferableModeLocal.REGULAR
        edCountingMode = EDCountingModeLocal.TOTAL
    }

    asset.copy(
        freeInPlanks = data.free,
        frozenInPlanks = data.frozen,
        reservedInPlanks = data.reserved,
        transferableMode = transferableMode,
        edCountingMode = edCountingMode
    )
}

fun bindAccountInfoOrDefault(hex: String?, runtime: RuntimeSnapshot): AccountInfo {
    return hex?.let { bindAccountInfo(it, runtime) } ?: AccountInfo.empty()
}
