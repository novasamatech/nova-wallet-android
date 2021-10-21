package io.novafoundation.nova.feature_wallet_impl.presentation.balance.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapTokenToTokenModel
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.assetActions.buy.BuyMixin
import io.novafoundation.nova.feature_wallet_impl.presentation.model.AssetModel
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.history.mixin.TransactionHistoryMixin
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.history.mixin.TransactionHistoryUi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BalanceDetailViewModel(
    private val interactor: WalletInteractor,
    private val router: WalletRouter,
    private val assetPayload: AssetPayload,
    private val buyMixin: BuyMixin.Presentation,
    private val transactionHistoryMixin: TransactionHistoryMixin,
) : BaseViewModel(),
    TransactionHistoryUi by transactionHistoryMixin,
    BuyMixin by buyMixin {

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent

    private val _showFrozenDetailsEvent = MutableLiveData<Event<AssetModel>>()
    val showFrozenDetailsEvent: LiveData<Event<AssetModel>> = _showFrozenDetailsEvent

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

    val buyEnabled = buyMixin.isBuyEnabled(assetPayload.chainId, assetPayload.chainAssetId)

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

            val results = awaitAll(deferredAssetSync, deferredTransactionsSync)

            val firstError = results.mapNotNull { it.exceptionOrNull() }
                .firstOrNull()

            firstError?.let(::showError)

            _hideRefreshEvent.value = Event(Unit)
        }
    }

    fun backClicked() {
        router.back()
    }

    fun sendClicked() {
        router.openChooseRecipient(assetPayload)
    }

    fun receiveClicked() {
        router.openReceive(assetPayload)
    }

    fun buyClicked() {
        viewModelScope.launch {
            buyMixin.buyClicked(assetPayload.chainId, assetPayload.chainAssetId)
        }
    }

    fun frozenInfoClicked() = launch {
        _showFrozenDetailsEvent.value = Event(assetModel.first())
    }

    private fun mapAssetToUi(asset: Asset): AssetDetailsModel {
        return AssetDetailsModel(
            token = mapTokenToTokenModel(asset.token),
            total = mapAmountToAmountModel(asset.total, asset),
            transferable = mapAmountToAmountModel(asset.transferable, asset),
            locked = mapAmountToAmountModel(asset.frozen, asset)
        )
    }
}
