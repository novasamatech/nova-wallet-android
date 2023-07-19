package io.novafoundation.nova.feature_assets.presentation.balance.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.BuyMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.mapTokenToTokenModel
import io.novafoundation.nova.feature_assets.presentation.model.BalanceLocksModel
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.TransactionHistoryFilterPayload
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryMixin
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryUi
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsWithTotalAmount
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.mapBalanceIdToUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import jp.co.soramitsu.fearless_utils.hash.isPositive
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
    buyMixinFactory: BuyMixinFactory,
    private val transactionHistoryMixin: TransactionHistoryMixin,
    private val accountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val currencyInteractor: CurrencyInteractor,
    private val controllableAssetCheck: ControllableAssetCheckMixin,
    private val contributionsInteractor: ContributionsInteractor,
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

    private val contributionsFlow = selectedAccountFlow.flatMapLatest {
        contributionsInteractor.observeChainContributions(it, assetPayload.chainId, assetPayload.chainAssetId)
    }
        .onStart { emit(ContributionsWithTotalAmount.empty()) }
        .shareInBackground()

    val assetDetailsModel = combine(assetFlow, contributionsFlow) { asset, contributions ->
        mapAssetToUi(asset, contributions)
    }
        .inBackground()
        .share()

    private val lockedBalanceModel = combine(balanceLocksFlow, contributionsFlow, assetFlow) { locks, contributions, asset ->
        mapBalanceLocksToUi(locks, contributions, asset)
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
        router.openSend(assetPayload)
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

    private fun mapAssetToUi(asset: Asset, contributions: ContributionsWithTotalAmount<Contribution>): AssetDetailsModel {
        val totalContributed = asset.token.amountFromPlanks(contributions.totalContributed)

        return AssetDetailsModel(
            token = mapTokenToTokenModel(asset.token),
            total = mapAmountToAmountModel(asset.total + totalContributed, asset),
            transferable = mapAmountToAmountModel(asset.transferable, asset),
            locked = mapAmountToAmountModel(asset.locked + totalContributed, asset)
        )
    }

    private fun mapBalanceLocksToUi(
        balanceLocks: List<BalanceLock>,
        contributions: ContributionsWithTotalAmount<Contribution>,
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

        return BalanceLocksModel(
            buildList {
                addAll(mappedLocks)
                add(reservedBalance)

                if (contributions.totalContributed.isPositive()) {
                    val totalContributedBalance = BalanceLocksModel.Lock(
                        resourceManager.getString(R.string.assets_balance_details_locks_crowdloans),
                        mapAmountToAmountModel(contributions.totalContributed, asset)
                    )

                    add(totalContributedBalance)
                }
            }
        )
    }
}
