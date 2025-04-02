package io.novafoundation.nova.feature_assets.presentation.balance.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksInteractor
import io.novafoundation.nova.feature_assets.domain.price.ChartsInteractor
import io.novafoundation.nova.feature_assets.domain.price.AssetPriceChart
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.BuySellMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.BuySellMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.mapTokenToTokenModel
import io.novafoundation.nova.feature_assets.presentation.model.BalanceLocksModel
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.TransactionHistoryFilterPayload
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryMixin
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryUi
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.PriceChartModel
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.RealDateChartTextInjector
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.RealPriceChangeTextInjector
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.RealPricePriceTextInjector
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceHold
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.unlabeledReserves
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.balanceId
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.mapBalanceIdToUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.fullId
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    tradeMixinFactory: TradeMixin.Factory,
    private val transactionHistoryMixin: TransactionHistoryMixin,
    private val accountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val currencyInteractor: CurrencyInteractor,
    private val controllableAssetCheck: ControllableAssetCheckMixin,
    private val externalBalancesInteractor: ExternalBalancesInteractor,
    private val swapAvailabilityInteractor: SwapAvailabilityInteractor,
    private val assetIconProvider: AssetIconProvider,
    private val chartsInteractor: ChartsInteractor,
    private val buySellMixinFactory: BuySellMixinFactory
) : BaseViewModel(),
    TransactionHistoryUi by transactionHistoryMixin {

    val acknowledgeLedgerWarning = controllableAssetCheck.acknowledgeLedgerWarning

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent

    private val _showLockedDetailsEvent = MutableLiveData<Event<BalanceLocksModel>>()
    val showLockedDetailsEvent: LiveData<Event<BalanceLocksModel>> = _showLockedDetailsEvent

    private val chainFlow = walletInteractor.chainFlow(assetPayload.chainId)
        .shareInBackground()

    private val assetFlow = walletInteractor.assetFlow(assetPayload.chainId, assetPayload.chainAssetId)
        .inBackground()
        .share()

    private val balanceLocksFlow = balanceLocksInteractor.balanceLocksFlow(assetPayload.chainId, assetPayload.chainAssetId)
        .shareInBackground()

    private val balanceHoldsFlow = balanceLocksInteractor.balanceHoldsFlow(assetPayload.chainId, assetPayload.chainAssetId)
        .shareInBackground()

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

    val supportExpandableBalanceDetails = assetFlow.map { it.totalInPlanks.isPositive() }
        .shareInBackground()

    private val lockedBalanceModel = combine(balanceLocksFlow, balanceHoldsFlow, externalBalancesFlow, assetFlow) { locks, holds, externalBalances, asset ->
        mapBalanceLocksToUi(locks, holds, externalBalances, asset)
    }
        .inBackground()
        .share()

    val buySellMixin = buySellMixinFactory.create()

    val chainUI = chainFlow.map { mapChainToUi(it) }

    val buyMixin = tradeMixinFactory.create(scope = this)

    val swapButtonEnabled = assetFlow.flatMapLatest {
        swapAvailabilityInteractor.swapAvailableFlow(it.token.configuration, viewModelScope)
    }
        .onStart { emit(false) }
        .shareInBackground()

    val buySellEnabled: Flow<Boolean> = assetFlow
        .flatMapLatest { buyMixin.tradeEnabledFlow(it.token.configuration) }
        .inBackground()
        .share()

    val sendEnabled = assetFlow.map {
        sendInteractor.areTransfersEnabled(it.token.configuration)
    }
        .inBackground()
        .share()

    val priceChartTitle = assetFlow.map {
        val tokenName = it.token.configuration.symbol.value
        resourceManager.getString(R.string.price_chart_title, tokenName)
    }.shareInBackground()

    val priceChartFormatters: Flow<PriceChartTextInjectors> = assetFlow.map { asset ->
        val lastCoinRate = asset.token.coinRate?.rate
        val currency = currencyInteractor.getSelectedCurrency()

        PriceChartTextInjectors(
            RealPricePriceTextInjector(currency, lastCoinRate),
            RealPriceChangeTextInjector(resourceManager, currency),
            RealDateChartTextInjector(resourceManager)
        )
    }.shareInBackground()

    private val priceCharts: Flow<List<AssetPriceChart>?> = assetFlow.map { it.token.configuration.priceId }
        .distinctUntilChanged()
        .flatMapLatest {
            val priceId = it ?: return@flatMapLatest flowOf { null }
            chartsInteractor.chartsFlow(priceId)
        }.shareInBackground()

    val priceChartModels = priceCharts.map { charts ->
        charts?.map { mapChartsToUi(it) }
    }.shareInBackground()

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
            swapAvailabilityInteractor.sync(viewModelScope)

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

    fun swapClicked() {
        launch {
            val chainAsset = assetFlow.first().token.configuration
            val payload = SwapSettingsPayload.DefaultFlow(chainAsset.fullId.toAssetPayload())
            router.openSwapSetupAmount(payload)
        }
    }

    fun buyClicked() = checkControllableAsset {
        launch {
            val chainAsset = assetFlow.first().token.configuration
            buySellMixin.openSelector(BuySellMixin.Selector.Asset(chainAsset.chainId, chainAsset.id))
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
            locked = mapAmountToAmountModel(asset.locked + totalContributed, asset),
            assetIcon = assetIconProvider.getAssetIconOrFallback(asset.token.configuration)
        )
    }

    private fun mapBalanceLocksToUi(
        balanceLocks: List<BalanceLock>,
        holds: List<BalanceHold>,
        externalBalances: List<ExternalBalance>,
        asset: Asset
    ): BalanceLocksModel {
        val mappedLocks = balanceLocks.map {
            BalanceLocksModel.Lock(
                mapBalanceIdToUi(resourceManager, it.id.value),
                mapAmountToAmountModel(it.amountInPlanks, asset)
            )
        }

        val mappedHolds = holds.map {
            BalanceLocksModel.Lock(
                mapBalanceIdToUi(resourceManager, it.identifier),
                mapAmountToAmountModel(it.amountInPlanks, asset)
            )
        }

        val unlabeledReserves = asset.unlabeledReserves(holds)

        val reservedBalance = BalanceLocksModel.Lock(
            resourceManager.getString(R.string.wallet_balance_reserved),
            mapAmountToAmountModel(unlabeledReserves, asset)
        )

        val external = externalBalances.map { externalBalance ->
            BalanceLocksModel.Lock(
                name = mapBalanceIdToUi(resourceManager, externalBalance.type.balanceId),
                amount = mapAmountToAmountModel(externalBalance.amount, asset)
            )
        }

        val locks = buildList {
            addAll(mappedLocks)
            addAll(mappedHolds)
            add(reservedBalance)
            addAll(external)
        }

        return BalanceLocksModel(locks)
    }

    private fun mapChartsToUi(assetPriceChart: AssetPriceChart): PriceChartModel {
        val buttonText = mapButtonText(assetPriceChart.range)

        return if (assetPriceChart.chart is ExtendedLoadingState.Loaded) {
            val periodName = mapPeriodName(assetPriceChart.range)
            val supportTimeShowing = supportTimeShowing(assetPriceChart.range)
            val mappedChart = assetPriceChart.chart.data.map { PriceChartModel.Chart.Price(it.timestamp, it.rate) }
            PriceChartModel.Chart(buttonText, periodName, supportTimeShowing, mappedChart)
        } else {
            PriceChartModel.Loading(buttonText)
        }
    }

    private fun mapButtonText(pricePeriod: PricePeriod): String {
        val buttonTextRes = when (pricePeriod) {
            PricePeriod.DAY -> R.string.price_chart_day
            PricePeriod.WEEK -> R.string.price_chart_week
            PricePeriod.MONTH -> R.string.price_chart_month
            PricePeriod.YEAR -> R.string.price_chart_year
            PricePeriod.MAX -> R.string.price_chart_max
        }

        return resourceManager.getString(buttonTextRes)
    }

    private fun mapPeriodName(pricePeriod: PricePeriod): String {
        val periodNameRes = when (pricePeriod) {
            PricePeriod.DAY -> R.string.price_charts_period_today
            PricePeriod.WEEK -> R.string.price_charts_period_week
            PricePeriod.MONTH -> R.string.price_charts_period_month
            PricePeriod.YEAR -> R.string.price_charts_period_year
            PricePeriod.MAX -> R.string.price_charts_period_all
        }

        return resourceManager.getString(periodNameRes)
    }

    private fun supportTimeShowing(pricePeriod: PricePeriod): Boolean {
        return when (pricePeriod) {
            PricePeriod.DAY, PricePeriod.WEEK, PricePeriod.MONTH -> true
            PricePeriod.YEAR, PricePeriod.MAX -> false
        }
    }
}
