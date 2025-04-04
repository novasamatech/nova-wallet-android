package io.novafoundation.nova.feature_assets.presentation.novacard.overview

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardState
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.NovaCardWebViewControllerFactory
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.CardCreationInterceptor
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.CardCreationInterceptorFactory
import io.novafoundation.nova.feature_assets.presentation.common.trade.mercuryo.MercuryoSellRequestInterceptorFactory
import io.novafoundation.nova.feature_assets.presentation.common.trade.callback.TradeSellCallback
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlinx.coroutines.flow.map

class NovaCardViewModel(
    private val chainRegistry: ChainRegistry,
    private val accountInteractor: AccountInteractor,
    private val assetsRouter: AssetsRouter,
    private val novaCardInteractor: NovaCardInteractor,
    private val cardCreationInterceptorFactory: CardCreationInterceptorFactory,
    private val mercuryoSellRequestInterceptorFactory: MercuryoSellRequestInterceptorFactory,
    private val novaCardWebViewControllerFactory: NovaCardWebViewControllerFactory
) : BaseViewModel(), CardCreationInterceptor.Callback, TradeSellCallback {

    private val openedOrderIds = mutableSetOf<String>()

    private val metaAccount = flowOf { accountInteractor.selectedMetaAccount() }

    private val setupCardConfig = metaAccount.map { metaAccount ->
        val topUpChain = getTopUpChain()
        CardSetupConfig(
            refundAddress = metaAccount.requireAddressIn(topUpChain),
            spendToken = topUpChain.utilityAsset
        )
    }

    val novaCardWebViewControllerFlow = setupCardConfig.map { setupConfig ->
        novaCardWebViewControllerFactory.create(
            interceptors = listOf(
                cardCreationInterceptorFactory.create(this),
                mercuryoSellRequestInterceptorFactory.create(this)
            ),
            setupConfig = setupConfig,
            scope = viewModelScope
        )
    }

    init {
        ensureCardCreationIsBlocking()
    }

    override fun onSellStart(orderId: String, amount: BigDecimal, address: String) {
        if (orderId in openedOrderIds) return // To not handle same order twice
        openedOrderIds.add(orderId)

        launch {
            val payload = TopUpCardPayload(
                amount = amount,
                address = address,
                asset = setupCardConfig.first().spendToken.toAssetPayload()
            )

            assetsRouter.openTopUpCard(payload)
        }
    }

    override fun onSellCompleted(orderId: String) {
        launch {
            openedOrderIds.remove(orderId)

            novaCardInteractor.setTopUpFinishedEvent()
            setCardStateCreated()
        }
    }

    override fun onCardCreated() {
        setCardStateCreated()
    }

    private suspend fun getTopUpChain(): Chain {
        return chainRegistry.getChain(ChainGeneses.POLKADOT)
    }

    private fun ensureCardCreationIsBlocking() {
        launch {
            val novaCardState = novaCardInteractor.getNovaCardState()

            if (novaCardState == NovaCardState.CREATION) {
                assetsRouter.openAwaitingCardCreation()
            }
        }
    }

    private fun setCardStateCreated() {
        if (novaCardInteractor.isNovaCardCreated()) return

        novaCardInteractor.setNovaCardState(NovaCardState.CREATED)
    }
}
