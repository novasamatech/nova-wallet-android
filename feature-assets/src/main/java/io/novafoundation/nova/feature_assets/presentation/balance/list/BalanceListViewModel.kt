package io.novafoundation.nova.feature_assets.presentation.balance.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.data.model.MaskingMode
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.usecase.MaskingModeUseCase
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.list.AssetsListInteractor
import io.novafoundation.nova.feature_assets.domain.assets.list.NftPreviews
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdown
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdownInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownAmount
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownItem
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownTotal
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.TotalBalanceBreakdownModel
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetListMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.BuySellSelectorMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.buySell.BuySellSelectorMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.NftPreviewUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.TotalBalanceModel
import io.novafoundation.nova.feature_assets.presentation.balance.list.view.AssetViewModeModel
import io.novafoundation.nova.feature_assets.presentation.balance.list.view.PendingOperationsCountModel
import io.novafoundation.nova.feature_assets.presentation.novacard.common.NovaCardRestrictionCheckMixin
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.assetsSource
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.mapBalanceIdToUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatter
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatterProvider
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import io.novafoundation.nova.feature_wallet_api.presentation.model.FractionPartStyling
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mapNumberOfActiveSessionsToUi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private typealias SyncAction = suspend (MetaAccount) -> Unit

class BalanceListViewModel(
    private val promotionBannersMixinFactory: PromotionBannersMixinFactory,
    private val bannerSourceFactory: BannersSourceFactory,
    private val walletInteractor: WalletInteractor,
    private val assetsListInteractor: AssetsListInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val router: AssetsRouter,
    private val currencyInteractor: CurrencyInteractor,
    private val balanceBreakdownInteractor: BalanceBreakdownInteractor,
    private val resourceManager: ResourceManager,
    private val walletConnectSessionsUseCase: WalletConnectSessionsUseCase,
    private val swapAvailabilityInteractor: SwapAvailabilityInteractor,
    private val assetListMixinFactory: AssetListMixinFactory,
    private val amountFormatter: AmountFormatter,
    private val fiatFormatter: FiatFormatter,
    private val maskableValueFormatterProvider: MaskableValueFormatterProvider,
    private val buySellSelectorMixinFactory: BuySellSelectorMixinFactory,
    private val multisigPendingOperationsService: MultisigPendingOperationsService,
    private val novaCardRestrictionCheckMixin: NovaCardRestrictionCheckMixin,
    private val maskingModeUseCase: MaskingModeUseCase
) : BaseViewModel() {

    private val maskableAmountFormatterFlow = maskableValueFormatterProvider.provideFormatter()
        .shareInBackground()

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent

    private val _showBalanceBreakdownEvent = MutableLiveData<Event<TotalBalanceBreakdownModel>>()
    val showBalanceBreakdownEvent: LiveData<Event<TotalBalanceBreakdownModel>> = _showBalanceBreakdownEvent

    val bannersMixin = promotionBannersMixinFactory.create(bannerSourceFactory.assetsSource(), viewModelScope)

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()
        .inBackground()
        .share()

    private val fullSyncActions: List<SyncAction> = listOf(
        { walletInteractor.syncAssetsRates(selectedCurrency.first()) },
        walletInteractor::syncAllNfts
    )

    val buySellSelectorMixin = buySellSelectorMixinFactory.create(BuySellSelectorMixin.SelectorType.AllAssets, viewModelScope)

    val assetListMixin = assetListMixinFactory.create(viewModelScope)

    private val externalBalancesFlow = assetListMixin.externalBalancesFlow

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
        .combine(maskableAmountFormatterFlow, ::formatNftCount)
        .inBackground()
        .share()

    val nftPreviewsUi = nftsPreviews
        .combine(maskableAmountFormatterFlow, ::mapNftPreviewToUi)
        .inBackground()
        .share()

    val maskingModeEnableFlow = maskingModeUseCase.observeMaskingMode()
        .map { it == MaskingMode.ENABLED }
        .shareInBackground()

    val totalBalanceFlow = combine(
        balanceBreakdown,
        swapAvailabilityInteractor.anySwapAvailableFlow(),
        maskableAmountFormatterFlow
    ) { breakdown, swapSupported, maskableAmountFormatter ->
        val currency = selectedCurrency.first()
        TotalBalanceModel(
            isBreakdownAvailable = breakdown.breakdown.isNotEmpty(),
            totalBalanceFiat = maskableAmountFormatter.format {
                fiatFormatter.formatFiat(
                    breakdown.total,
                    currency,
                    config = FiatConfig(
                        abbreviationStyle = FiatConfig.AbbreviationStyle.SIMPLE_ABBREVIATION,
                        fractionPartStyling = FractionPartStyling.Styled(R.dimen.total_balance_fraction_size)
                    )
                )
            },
            lockedBalanceFiat = maskableAmountFormatter.format { fiatFormatter.formatFiat(breakdown.locksTotal.amount, currency) },
            enableSwap = swapSupported
        )
    }
        .inBackground()
        .share()

    val shouldShowPlaceholderFlow = assetListMixin.assetModelsFlow.map { it.isEmpty() }

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

    val assetViewModeModelFlow = assetListMixin.assetsViewModeFlow.map {
        when (it) {
            AssetViewMode.NETWORKS -> AssetViewModeModel(R.drawable.ic_asset_view_networks, R.string.asset_view_networks)
            AssetViewMode.TOKENS -> AssetViewModeModel(R.drawable.ic_asset_view_tokens, R.string.asset_view_tokens)
        }
    }.distinctUntilChanged()

    val pendingOperationsCountModel = multisigPendingOperationsService.pendingOperationsCountFlow()
        .withSafeLoading()
        .combine(maskableAmountFormatterFlow, ::formatPendingOperationsCount)
        .shareInBackground()

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

    fun assetClicked(asset: Chain.Asset) {
        val payload = AssetPayload(
            chainId = asset.chainId,
            chainAssetId = asset.id
        )

        router.openAssetDetails(payload)
    }

    fun avatarClicked() {
        router.openSwitchWallet()
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
            if (totalBalance.isBreakdownAvailable) {
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

    private fun mapNftPreviewToUi(nftPreviews: NftPreviews, maskableValueFormatter: MaskableValueFormatter): MaskableModel<List<NftPreviewUi>> {
        return maskableValueFormatter.format {
            nftPreviews.nftPreviews.map {
                when (val details = it.details) {
                    Nft.Details.Loadable -> LoadingState.Loading()
                    is Nft.Details.Loaded -> {
                        LoadingState.Loaded(details.media)
                    }
                }
            }
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
                    amount = amountFormatter.formatAmountToAmountModel(it.tokenAmount, it.token)
                )
            }

            addAll(breakdown)
        }
    }

    private fun formatPendingOperationsCount(
        operationsLoadingState: ExtendedLoadingState<Int>,
        formatter: MaskableValueFormatter
    ): PendingOperationsCountModel {
        return when (val count = operationsLoadingState.dataOrNull) {
            null, 0 -> PendingOperationsCountModel.Gone

            else -> PendingOperationsCountModel.Visible(formatter.format { count.format() })
        }
    }

    fun sendClicked() {
        router.openSendFlow()
    }

    fun receiveClicked() {
        router.openReceiveFlow()
    }

    fun buySellClicked() {
        buySellSelectorMixin.openSelector()
    }

    fun swapClicked() {
        router.openSwapFlow()
    }

    fun giftClicked() {
        router.openGiftsFlow()
    }

    fun novaCardClicked() = launchUnit {
        novaCardRestrictionCheckMixin.checkRestrictionAndDo {
            router.openNovaCard()
        }
    }

    fun switchViewMode() {
        launch { assetListMixin.switchViewMode() }
    }

    fun pendingOperationsClicked() {
        router.openPendingMultisigOperations()
    }

    private fun formatNftCount(nftPreviews: NftPreviews, formatter: MaskableValueFormatter): MaskableModel<String>? {
        if (nftPreviews.totalNftsCount == 0) return null

        return formatter.format { nftPreviews.totalNftsCount.format() }
    }

    fun toggleMasking() {
        maskingModeUseCase.toggleMaskingMode()
    }
}
