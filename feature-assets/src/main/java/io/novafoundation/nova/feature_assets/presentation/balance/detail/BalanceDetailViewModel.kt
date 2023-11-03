package io.novafoundation.nova.feature_assets.presentation.balance.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.mapTokenToTokenModel
import io.novafoundation.nova.feature_assets.presentation.model.BalanceLocksModel
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.TransactionHistoryFilterPayload
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryMixin
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryUi
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixin
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.balanceId
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.mapBalanceIdToUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class BalanceDetailViewModel(
    private val walletInteractor: WalletInteractor,
    private val balanceLocksInteractor: BalanceLocksInteractor,
    private val sendInteractor: SendInteractor,
    private val router: AssetsRouter,
    private val assetPayload: AssetPayload,
    buyMixinFactory: BuyMixin.Factory,
    private val transactionHistoryMixin: TransactionHistoryMixin,
    private val accountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val currencyInteractor: CurrencyInteractor,
    private val controllableAssetCheck: ControllableAssetCheckMixin,
    private val externalBalancesInteractor: ExternalBalancesInteractor,
) : BaseViewModel(),
    TransactionHistoryUi by transactionHistoryMixin {

    val acknowledgeLedgerWarning = controllableAssetCheck.acknowledgeLedgerWarning

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

    private val selectedAccountFlow = accountUseCase.selectedMetaAccountFlow()
        .share()

    private val externalBalancesFlow = externalBalancesInteractor.observeExternalBalances(assetPayload.fullChainAssetId)
        .onStart { emit(emptyList()) }
        .shareInBackground()

    val assetDetailsModel = combine(assetFlow, externalBalancesFlow) { asset, externalBalances ->
        mapAssetToUi(asset, externalBalances)
    }
        .inBackground()
        .share()

    private val lockedBalanceModel = combine(balanceLocksFlow, externalBalancesFlow, assetFlow) { locks, externalBalances, asset ->
        mapBalanceLocksToUi(locks, externalBalances, asset)
    }
        .inBackground()
        .share()

    val buyMixin = buyMixinFactory.create(scope = this)

    val buyEnabled: Flow<Boolean> = assetFlow
        .flatMapLatest { buyMixin.buyEnabledFlow(it.token.configuration) }
        .inBackground()
        .share()

    val sendEnabled = assetFlow.map {
        sendInteractor.areTransfersEnabled(it.token.configuration)
    }
        .inBackground()
        .share()

    init {
        sync()
    }

    override fun onCleared() {
        super.onCleared()

        transactionHistoryMixin.cancel()
    }

    fun transactionsScrolled(index: Int) {
        transactionHistoryMixin.scrolled(index)
    }

    fun filterClicked() {
        val payload = TransactionHistoryFilterPayload(assetPayload)
        router.openFilter(payload)
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
        router.openSend(SendPayload.SpecifiedOrigin(assetPayload))
    }

    fun receiveClicked() = checkControllableAsset {
        router.openReceive(assetPayload)
    }

    fun buyClicked() = checkControllableAsset {
        launch {
            val chainAsset = assetFlow.first().token.configuration
            buyMixin.buyClicked(chainAsset)
        }
    }

    fun lockedInfoClicked() = launch {
        val balanceLocks = lockedBalanceModel.first()
        _showLockedDetailsEvent.value = Event(balanceLocks)
    }

    private fun checkControllableAsset(action: () -> Unit) {
        launch {
            val metaAccount = selectedAccountFlow.first()
            val chainAsset = assetFlow.first().token.configuration
            controllableAssetCheck.check(metaAccount, chainAsset) { action() }
        }
    }

    private fun mapAssetToUi(asset: Asset, externalBalances: List<ExternalBalance>): AssetDetailsModel {
        val totalContributedPlanks = externalBalances.sumByBigInteger { it.amount }
        val totalContributed = asset.token.amountFromPlanks(totalContributedPlanks)

        return AssetDetailsModel(
            token = mapTokenToTokenModel(asset.token),
            total = mapAmountToAmountModel(asset.total + totalContributed, asset),
            transferable = mapAmountToAmountModel(asset.transferable, asset),
            locked = mapAmountToAmountModel(asset.locked + totalContributed, asset)
        )
    }

    private fun mapBalanceLocksToUi(
        balanceLocks: List<BalanceLock>,
        externalBalances: List<ExternalBalance>,
        asset: Asset
    ): BalanceLocksModel {
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

        val external = externalBalances.map { externalBalance ->
            BalanceLocksModel.Lock(
                name = mapBalanceIdToUi(resourceManager, externalBalance.type.balanceId),
                amount = mapAmountToAmountModel(externalBalance.amount, asset)
            )
        }

        val locks = buildList {
            addAll(mappedLocks)
            add(reservedBalance)
            addAll(external)
        }

        return BalanceLocksModel(locks)
    }
}
