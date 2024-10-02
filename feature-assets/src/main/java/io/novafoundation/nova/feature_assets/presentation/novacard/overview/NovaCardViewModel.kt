package io.novafoundation.nova.feature_assets.presentation.novacard.overview

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.NovaCardEventHandler
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal

class NovaCardViewModel(
    private val chainRegistry: ChainRegistry,
    private val accountInteractor: AccountInteractor,
    private val assetsRouter: AssetsRouter,
    private val novaCardInteractor: NovaCardInteractor
) : BaseViewModel() {

    private val metaAccount = flowOf { accountInteractor.selectedMetaAccount() }

    private val topUpChain = flowOf { chainRegistry.getChain(ChainGeneses.POLKADOT) }
        .shareInBackground()

    val setupCardConfig = combine(metaAccount, topUpChain) { metaAccount, topUpChain ->
        CardSetupConfig(
            refundAddress = metaAccount.requireAddressIn(topUpChain),
            spendToken = topUpChain.utilityAsset
        )
    }.shareInBackground()

    fun onTransactionStatusChanged(event: NovaCardEventHandler.TransactionStatus) {
        showMessage("New status: $event")
    }

    fun openTopUp(amount: BigDecimal, address: String) = launch {
        novaCardInteractor.setTimeCardBeingIssued(System.currentTimeMillis())

        val payload = TopUpCardPayload(
            amount = amount,
            address = address,
            asset = setupCardConfig.first().spendToken.toAssetPayload()
        )

        assetsRouter.openTopUpCard(payload)
    }

    fun onCardCreated() {
        // No need to open/close timer dialog if card is already active
        if (novaCardInteractor.isNovaCardStateActive()) return

        novaCardInteractor.setNovaCardState(true)
    }
}
