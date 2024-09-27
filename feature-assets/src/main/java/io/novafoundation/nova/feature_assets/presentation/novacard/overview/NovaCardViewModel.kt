package io.novafoundation.nova.feature_assets.presentation.novacard.overview

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.NovaCardEventHandler
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NovaCardViewModel(
    private val chainRegistry: ChainRegistry,
    private val accountInteractor: AccountInteractor,
    private val assetsRouter: AssetsRouter
) : BaseViewModel() {

    private val metaAccount = flowOf { accountInteractor.selectedMetaAccount() }
    private val topUpChain = flowOf { chainRegistry.getChain(ChainGeneses.POLKADOT) }
    private val topUpAsset = topUpChain.map { it.utilityAsset }

    suspend fun getRefundAddress(): String {
        val chain = topUpChain.first()
        return metaAccount.first().requireAddressIn(chain)
    }

    fun onTransactionStatusChanged(event: NovaCardEventHandler.TransactionStatus) {
        // TODO
    }

    fun openTopUp(amount: BigDecimal, address: String) = launch {
        assetsRouter.openTopUpCard(
            TopUpCardPayload(
                amount = amount,
                address = address,
                asset = topUpAsset.first().toAssetPayload()
            )
        )
    }
}
