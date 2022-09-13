package io.novafoundation.nova.feature_assets.presentation.balance.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.data.mappers.mappers.mapTokenToTokenModel
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksInteractor
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.BuyMixinFactory
import io.novafoundation.nova.feature_assets.presentation.common.mapBalanceIdToUi
import io.novafoundation.nova.feature_assets.presentation.model.BalanceLocksModel
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryMixin
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryUi
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type.Orml
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private typealias LedgerWarningMessage = String

class BalanceDetailViewModel(
    private val walletInteractor: WalletInteractor,
    private val balanceLocksInteractor: BalanceLocksInteractor,
    private val sendInteractor: SendInteractor,
    private val router: AssetsRouter,
    private val assetPayload: AssetPayload,
    buyMixinFactory: BuyMixinFactory,
    private val transactionHistoryMixin: TransactionHistoryMixin,
    private val accountUseCase: SelectedAccountUseCase,
    private val missingKeysPresenter: WatchOnlyMissingKeysPresenter,
    private val resourceManager: ResourceManager,
    private val currencyInteractor: CurrencyInteractor,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
) : BaseViewModel(),
    TransactionHistoryUi by transactionHistoryMixin {

    val acknowledgeLedgerWarning = actionAwaitableMixinFactory.confirmingAction<LedgerWarningMessage>()

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent

    private val _showLockedDetailsEvent = MutableLiveData<Event<BalanceLocksModel>>()
    val showLockedDetailsEvent: LiveData<Event<BalanceLocksModel>> = _showLockedDetailsEvent

    private val assetFlow = walletInteractor.assetFlow(assetPayload.chainId, assetPayload.chainAssetId)
        .inBackground()
        .share()

    private val balanceLocksFlow = balanceLocksInteractor.balanceLocksFlow(assetPayload.chainId, assetPayload.chainAssetId)
        .inBackground()
        .share()

    val assetDetailsModel = assetFlow
        .map { mapAssetToUi(it) }
        .inBackground()
        .share()

    private val lockedBalanceModel = balanceLocksFlow
        .map { mapBalanceLocksToUi(it, assetFlow.first()) }
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
        launch {
            val currency = currencyInteractor.getSelectedCurrency()
            val deferredAssetSync = async { walletInteractor.syncAssetsRates(currency) }
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

    fun receiveClicked() = checkControllableAsset {
        router.openReceive(assetPayload)
    }

    fun buyClicked() = checkControllableAsset {
        buyMixin.buyClicked()
    }

    fun lockedInfoClicked() = launch {
        val balanceLocks = lockedBalanceModel.first()
        _showLockedDetailsEvent.value = Event(balanceLocks)
    }

    private fun checkControllableAsset(action: () -> Unit) {
        launch {
            val metaAccount = accountUseCase.getSelectedMetaAccount()
            val chainAsset = assetFlow.first().token.configuration

            when {
                metaAccount.type == Type.LEDGER && chainAsset.type is Orml -> showLedgerAssetNotSupportedWarning(chainAsset)
                metaAccount.type == Type.WATCH_ONLY -> missingKeysPresenter.presentNoKeysFound()
                else -> action()
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

    @OptIn(ExperimentalStdlibApi::class)
    private fun mapBalanceLocksToUi(balanceLocks: List<BalanceLock>, asset: Asset): BalanceLocksModel {
        val mappedLocks = balanceLocks.map {
            BalanceLocksModel.Lock(
                mapBalanceIdToUi(resourceManager, it.id),
                mapAmountToAmountModel(it.amountInPlanks, asset)
            )
        }

        val reservedBalance = BalanceLocksModel.Lock(
            resourceManager.getString(R.string.wallet_balance_reserved),
            mapAmountToAmountModel(asset.reserved, asset)
        )

        return BalanceLocksModel(
            buildList {
                addAll(mappedLocks)
                add(reservedBalance)
            }
        )
    }

    private fun showLedgerAssetNotSupportedWarning(chainAsset: Chain.Asset) = launch {
        val assetSymbol = chainAsset.symbol
        val warningMessage = resourceManager.getString(R.string.assets_receive_ledger_not_supported_message, assetSymbol, assetSymbol)

        acknowledgeLedgerWarning.awaitAction(warningMessage)
    }
}
