package io.novafoundation.nova.feature_assets.presentation.balance.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_assets.data.mappers.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_assets.data.mappers.mappers.mapTokenToTokenModel
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.BuyMixinFactory
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryMixin
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryUi
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BalanceDetailViewModel(
    private val interactor: WalletInteractor,
    private val sendInteractor: SendInteractor,
    private val router: WalletRouter,
    private val assetPayload: AssetPayload,
    buyMixinFactory: BuyMixinFactory,
    private val transactionHistoryMixin: TransactionHistoryMixin,
    private val accountUseCase: SelectedAccountUseCase,
    private val missingKeysPresenter: WatchOnlyMissingKeysPresenter,
) : BaseViewModel(),
    TransactionHistoryUi by transactionHistoryMixin {

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent

    private val _showLockedDetailsEvent = MutableLiveData<Event<AssetModel>>()
    val showFrozenDetailsEvent: LiveData<Event<AssetModel>> = _showLockedDetailsEvent

    private val assetFlow = interactor.assetFlow(assetPayload.chainId, assetPayload.chainAssetId)
        .inBackground()
        .share()

    val assetDetailsModel = assetFlow
        .map { mapAssetToUi(it) }
        .inBackground()
        .share()

    private val assetModel = assetFlow
        .map { mapAssetToAssetModel(it) }
        .inBackground()
        .share()

    val buyMixin = buyMixinFactory.create(scope = this, assetPayload)

    val sendEnabled = assetFlow.map {
        sendInteractor.areTransfersEnabled(it.token.configuration)
    }
        .inBackground()
        .share()

    override fun onCleared() {
        super.onCleared()

        transactionHistoryMixin.cancel()
    }

    fun transactionsScrolled(index: Int) {
        transactionHistoryMixin.scrolled(index)
    }

    fun filterClicked() {
        router.openFilter()
    }

    fun sync() {
        viewModelScope.launch {
            val deferredAssetSync = async { interactor.syncAssetsRates() }
            val deferredTransactionsSync = async { transactionHistoryMixin.syncFirstOperationsPage() }

            awaitAll(deferredAssetSync, deferredTransactionsSync)

            _hideRefreshEvent.value = Event(Unit)
        }
    }

    fun backClicked() {
        router.back()
    }

    fun sendClicked() {
        router.openSend(assetPayload)
    }

    fun receiveClicked() = requireSecretsWallet {
        router.openReceive(assetPayload)
    }

    fun buyClicked() = requireSecretsWallet {
        buyMixin.buyClicked()
    }

    fun lockedInfoClicked() = launch {
        _showLockedDetailsEvent.value = Event(assetModel.first())
    }

    private fun requireSecretsWallet(action: () -> Unit) {
        launch {
            val metaAccount = accountUseCase.getSelectedMetaAccount()

            when (metaAccount.type) {
                Type.SECRETS, Type.PARITY_SIGNER -> action()
                Type.WATCH_ONLY -> missingKeysPresenter.presentNoKeysFound()
            }
        }
    }

    private fun mapAssetToUi(asset: Asset): AssetDetailsModel {
        return AssetDetailsModel(
            token = mapTokenToTokenModel(asset.token),
            total = mapAmountToAmountModel(asset.total, asset),
            transferable = mapAmountToAmountModel(asset.transferable, asset),
            locked = mapAmountToAmountModel(asset.locked, asset)
        )
    }
}
