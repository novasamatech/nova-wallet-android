package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ControllableAssetCheckMixin(
    private val missingKeysPresenter: WatchOnlyMissingKeysPresenter,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager
) {

    val acknowledgeLedgerWarning = actionAwaitableMixinFactory.confirmingAction<String>()

    suspend fun check(metaAccount: MetaAccount, chainAsset: Chain.Asset, action: () -> Unit) {
        when {
            metaAccount.type == LightMetaAccount.Type.LEDGER_LEGACY && chainAsset.type is Chain.Asset.Type.Orml -> showLedgerAssetNotSupportedWarning(chainAsset)
            metaAccount.type == LightMetaAccount.Type.WATCH_ONLY -> missingKeysPresenter.presentNoKeysFound()
            else -> action()
        }
    }

    private suspend fun showLedgerAssetNotSupportedWarning(chainAsset: Chain.Asset) {
        val assetSymbol = chainAsset.symbol
        val warningMessage = resourceManager.getString(R.string.assets_receive_ledger_not_supported_message, assetSymbol, assetSymbol)

        acknowledgeLedgerWarning.awaitAction(warningMessage)
    }
}
