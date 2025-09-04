package io.novafoundation.nova.feature_assets.presentation.novacard.overview

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardState
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.NovaCardWebViewControllerFactory
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.CardCreationInterceptor
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.CardCreationInterceptorFactory
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressPayload
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressRequester
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressResponder
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoSellRequestInterceptorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class NovaCardViewModel(
    private val chainRegistry: ChainRegistry,
    private val accountInteractor: AccountInteractor,
    private val assetsRouter: AssetsRouter,
    private val novaCardInteractor: NovaCardInteractor,
    private val cardCreationInterceptorFactory: CardCreationInterceptorFactory,
    private val mercuryoSellRequestInterceptorFactory: MercuryoSellRequestInterceptorFactory,
    private val novaCardWebViewControllerFactory: NovaCardWebViewControllerFactory,
    private val topUpRequester: TopUpAddressRequester,
    private val resourceManager: ResourceManager
) : BaseViewModel(), CardCreationInterceptor.Callback, OnSellOrderCreatedListener, OnTradeOperationFinishedListener {

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
                mercuryoSellRequestInterceptorFactory.create(this, this)
            ),
            setupConfig = setupConfig,
            scope = viewModelScope
        )
    }

    init {
        ensureCardCreationIsBlocking()

        observeTopUp()
    }

    override fun onSellOrderCreated(orderId: String, address: String, amount: BigDecimal) {
        launch {
            val asset = setupCardConfig.first().spendToken

            val payload = TopUpAddressPayload(
                amount = amount,
                address = address,
                asset = setupCardConfig.first().spendToken.toAssetPayload(),
                screenTitle = resourceManager.getString(R.string.fragment_top_up_card_title, asset.symbol.value)
            )

            topUpRequester.openRequest(payload)
        }
    }

    override fun onTradeOperationFinished(success: Boolean) { // Always success for mercuryo
        launch {
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

    private fun observeTopUp() {
        topUpRequester.responseFlow
            .onEach {
                when (it) {
                    TopUpAddressResponder.Response.Cancel -> {
                        assetsRouter.returnToMainScreen()
                    }

                    TopUpAddressResponder.Response.Success -> {
                        updateCardState()
                        updateLastTopUpTime()
                        assetsRouter.openAwaitingCardCreation()
                    }
                }
            }
            .launchIn(this)
    }

    private fun updateCardState() {
        if (!novaCardInteractor.isNovaCardCreated()) {
            novaCardInteractor.setNovaCardState(NovaCardState.CREATION)
        }
    }

    private fun updateLastTopUpTime() {
        novaCardInteractor.setLastTopUpTime(System.currentTimeMillis())
    }
}
