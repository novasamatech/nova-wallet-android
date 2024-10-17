package io.novafoundation.nova.feature_assets.presentation.balance.list

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.formatting.toAmountWithFraction
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.list.AssetsListInteractor
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdown
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdownInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownAmount
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownItem
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownTotal
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.TotalBalanceBreakdownModel
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetListMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.NftPreviewUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.TotalBalanceModel
import io.novafoundation.nova.feature_assets.presentation.balance.list.view.AssetViewModeModel
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.simpleFormatAsCurrency
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.mapBalanceIdToUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mapNumberOfActiveSessionsToUi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private typealias SyncAction = suspend (MetaAccount) -> Unit

@OptIn(ExperimentalTime::class)
class BalanceListViewModel(
    private val walletInteractor: WalletInteractor,
    private val assetsListInteractor: AssetsListInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val router: AssetsRouter,
    private val currencyInteractor: CurrencyInteractor,
    private val balanceBreakdownInteractor: BalanceBreakdownInteractor,
    private val externalBalancesInteractor: ExternalBalancesInteractor,
    private val resourceManager: ResourceManager,
    private val walletConnectSessionsUseCase: WalletConnectSessionsUseCase,
    private val swapAvailabilityInteractor: SwapAvailabilityInteractor,
    private val assetListMixinFactory: AssetListMixinFactory
) : BaseViewModel() {

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent

    private val _showBalanceBreakdownEvent = MutableLiveData<Event<TotalBalanceBreakdownModel>>()
    val showBalanceBreakdownEvent: LiveData<Event<TotalBalanceBreakdownModel>> = _showBalanceBreakdownEvent

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()
        .inBackground()
        .share()

    private val fullSyncActions: List<SyncAction> = listOf(
        { walletInteractor.syncAssetsRates(selectedCurrency.first()) },
        walletInteractor::syncAllNfts
    )

    private val externalBalancesFlow = externalBalancesInteractor.observeExternalBalances()

    val assetListMixin = assetListMixinFactory.create()

    private val isFiltersEnabledFlow = walletInteractor.isFiltersEnabledFlow()

    private val accountChangeSyncActions: List<SyncAction> = listOf(
        walletInteractor::syncAllNfts
    )

    private val selectedMetaAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .share()

    val selectedWalletModelFlow = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    private val balanceBreakdown = balanceBreakdownInteractor.balanceBreakdownFlow(assetListMixin.assetsFlow, externalBalancesFlow)
        .shareInBackground()

    private val nftsPreviews = assetsListInteractor.observeNftPreviews()
        .inBackground()
        .share()

    val nftCountFlow = nftsPreviews
        .map { it.totalNftsCount.format() }
        .inBackground()
        .share()

    val nftPreviewsUi = nftsPreviews
        .map { it.nftPreviews.map(::mapNftPreviewToUi) }
        .inBackground()
        .share()

    val totalBalanceFlow = combine(
        balanceBreakdown,
        swapAvailabilityInteractor.anySwapAvailableFlow()
    ) { breakdown, swapSupported ->
        val currency = selectedCurrency.first()
        TotalBalanceModel(
            isBreakdownAbailable = breakdown.breakdown.isNotEmpty(),
            totalBalanceFiat = breakdown.total.simpleFormatAsCurrency(currency).formatAsTotalBalance(),
            lockedBalanceFiat = breakdown.locksTotal.amount.formatAsCurrency(currency),
            enableSwap = swapSupported
        )
    }
        .inBackground()
        .share()

    val shouldShowPlaceholderFlow = assetListMixin.filteredAssetsFlow.map { it.isEmpty() }

    val balanceBreakdownFlow = balanceBreakdown.map {
        val currency = selectedCurrency.first()
        val total = it.total.formatAsCurrency(currency)
        TotalBalanceBreakdownModel(total, mapBreakdownToList(it, currency))
    }
        .shareInBackground()

    private val walletConnectAccountSessionCount = selectedMetaAccount.flatMapLatest {
        walletConnectSessionsUseCase.activeSessionsNumberFlow(it)
    }
        .shareInBackground()

    val walletConnectAccountSessionsUI = walletConnectAccountSessionCount
        .map(::mapNumberOfActiveSessionsToUi)
        .shareInBackground()

    val filtersIndicatorIcon = isFiltersEnabledFlow
        .map { if (it) R.drawable.ic_chip_filter_indicator else R.drawable.ic_chip_filter }
        .shareInBackground()

    val shouldShowCrowdloanBanner = assetsListInteractor.shouldShowCrowdloansBanner()
        .shareInBackground()

    val assetViewModeModelFlow = assetListMixin.assetsViewModeFlow.map {
        when (it) {
            AssetViewMode.NETWORKS -> AssetViewModeModel(R.drawable.ic_asset_view_networks, R.string.asset_view_networks)
            AssetViewMode.TOKENS -> AssetViewModeModel(R.drawable.ic_asset_view_tokens, R.string.asset_view_tokens)
        }
    }

    init {
        selectedCurrency
            .onEach { fullSync() }
            .launchIn(this)

        nftsPreviews
            .debounce(1L.seconds)
            .onEach { nfts ->
                nfts.nftPreviews
                    .filter { it.details is Nft.Details.Loadable }
                    .forEach { assetsListInteractor.fullSyncNft(it) }
            }
            .inBackground()
            .launchIn(this)

        selectedMetaAccount
            .mapLatest { syncWith(accountChangeSyncActions, it) }
            .launchIn(this)

        walletInteractor.nftSyncTrigger()
            .onEach { trigger -> walletInteractor.syncChainNfts(selectedMetaAccount.first(), trigger.chain) }
            .launchIn(viewModelScope)
    }

    fun fullSync() {
        viewModelScope.launch {
            syncWith(fullSyncActions, selectedMetaAccount.first())

            _hideRefreshEvent.value = Event(Unit)
        }
    }

    fun assetClicked(asset: AssetModel) {
        val payload = AssetPayload(
            chainId = asset.token.configuration.chainId,
            chainAssetId = asset.token.configuration.id
        )

        router.openAssetDetails(payload)
    }

    fun avatarClicked() {
        router.openSwitchWallet()
    }

    fun filtersClicked() {
        router.openAssetFilters()
    }

    fun manageClicked() {
        router.openManageTokens()
    }

    fun goToNftsClicked() {
        router.openNfts()
    }

    fun searchClicked() {
        router.openAssetSearch()
    }

    fun walletConnectClicked() {
        launch {
            if (walletConnectAccountSessionCount.first() > 0) {
                val metaAccount = selectedMetaAccount.first()
                router.openWalletConnectSessions(metaAccount.id)
            } else {
                router.openWalletConnectScan()
            }
        }
    }

    fun balanceBreakdownClicked() {
        launch {
            val totalBalance = totalBalanceFlow.first()
            if (totalBalance.isBreakdownAbailable) {
                val balanceBreakdown = balanceBreakdownFlow.first()
                _showBalanceBreakdownEvent.value = Event(balanceBreakdown)
            }
        }
    }

    private suspend fun syncWith(syncActions: List<SyncAction>, metaAccount: MetaAccount) = if (syncActions.size == 1) {
        val syncAction = syncActions.first()
        syncAction(metaAccount)
    } else {
        val syncJobs = syncActions.map { async { it(metaAccount) } }
        syncJobs.joinAll()
    }

    private fun mapNftPreviewToUi(nftPreview: Nft): NftPreviewUi {
        return when (val details = nftPreview.details) {
            Nft.Details.Loadable -> LoadingState.Loading()
            is Nft.Details.Loaded -> LoadingState.Loaded(details.media)
        }
    }

    private fun mapBreakdownToList(balanceBreakdown: BalanceBreakdown, currency: Currency): List<BalanceBreakdownItem> {
        return buildList {
            add(
                BalanceBreakdownTotal(
                    resourceManager.getString(R.string.wallet_balance_transferable),
                    balanceBreakdown.transferableTotal.amount.formatAsCurrency(currency),
                    R.drawable.ic_transferable,
                    balanceBreakdown.transferableTotal.percentage.formatAsPercentage()
                )
            )

            add(
                BalanceBreakdownTotal(
                    resourceManager.getString(R.string.wallet_balance_locked),
                    balanceBreakdown.locksTotal.amount.formatAsCurrency(currency),
                    R.drawable.ic_lock,
                    balanceBreakdown.locksTotal.percentage.formatAsPercentage()
                )
            )

            val breakdown = balanceBreakdown.breakdown.map {
                BalanceBreakdownAmount(
                    name = it.token.configuration.symbol.value + " " + mapBalanceIdToUi(resourceManager, it.id),
                    amount = mapAmountToAmountModel(it.tokenAmount, it.token)
                )
            }

            addAll(breakdown)
        }
    }

    private fun String.formatAsTotalBalance(): CharSequence {
        val amountWithFraction = toAmountWithFraction()

        val textColor = resourceManager.getColor(R.color.text_secondary)
        val colorSpan = ForegroundColorSpan(textColor)
        val sizeSpan = AbsoluteSizeSpan(resourceManager.getDimensionPixelSize(R.dimen.total_balance_fraction_size))

        return with(amountWithFraction) {
            val spannableBuilder = SpannableStringBuilder()
                .append(amount)
            if (fraction != null) {
                spannableBuilder.append(separator + fraction)
                val startIndex = amount.length
                val endIndex = amount.length + separator.length + fraction!!.length
                spannableBuilder.setSpan(colorSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannableBuilder.setSpan(sizeSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            spannableBuilder
        }
    }

    fun sendClicked() {
        router.openSendFlow()
    }

    fun receiveClicked() {
        router.openReceiveFlow()
    }

    fun buyClicked() {
        router.openBuyFlow()
    }

    fun crowdloanBannerClicked() {
        router.openStaking()

        hideCrowdloanBanner()
    }

    fun crowdloanBannerCloseClicked() {
        hideCrowdloanBanner()
    }

    fun swapClicked() {
        router.openSwapFlow()
    }

    private fun hideCrowdloanBanner() = launch {
        assetsListInteractor.hideCrowdloanBanner()
    }

    fun switchViewMode() {
        launch { assetListMixin.switchViewMode() }
    }
}
