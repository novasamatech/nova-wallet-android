package io.novafoundation.nova.feature_assets.presentation.novacard.overview

import android.util.Log
import io.novafoundation.nova.common.BuildConfig
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardState
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.NovaCardEventHandler
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.ChainIds
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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

    private val topUpChain = flowOf { getTopUpChain() }
        .shareInBackground()

    val setupCardConfig = combine(metaAccount, topUpChain) { metaAccount, topUpChain ->
        CardSetupConfig(
            refundAddress = metaAccount.requireAddressIn(topUpChain),
            spendToken = topUpChain.utilityAsset
        )
    }.shareInBackground()

    init {
        launch {
            val novaCardState = novaCardInteractor.getNovaCardState()

            if (novaCardState == NovaCardState.CREATION) {
                assetsRouter.openAwaitingCardCreation()
            }
        }
    }

    fun onTransactionStatusChanged(event: NovaCardEventHandler.TransactionStatus) {
        Log.d("NovaCardView", "onTransactionStatusChanged: $event")
    }

    fun openTopUp(amount: BigDecimal, address: String) = launch {
        val payload = TopUpCardPayload(
            amount = amount,
            address = address,
            asset = setupCardConfig.first().spendToken.toAssetPayload()
        )

        assetsRouter.openTopUpCard(payload)
    }

    fun onCardCreated() {
        // No need to open/close timer dialog if card is already created
        if (novaCardInteractor.isNovaCardCreated()) return

        novaCardInteractor.setNovaCardState(NovaCardState.CREATED)
    }

    private suspend fun getTopUpChain(): Chain {
        return chainRegistry.getChain(ChainGeneses.POLKADOT)
    }
}
