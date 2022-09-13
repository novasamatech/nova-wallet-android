package io.novafoundation.nova.feature_assets.presentation.balance.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.list.AssetsListInteractor
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdown
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdownInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownAmount
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownItem
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.BalanceBreakdownTotal
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.TotalBreakdownModel
import io.novafoundation.nova.feature_assets.presentation.balance.common.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.NftPreviewUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.TotalBalanceModel
import io.novafoundation.nova.feature_assets.presentation.common.mapBalanceIdToUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import kotlinx.coroutines.flow.combine

private typealias SyncAction = suspend (MetaAccount) -> Unit

@OptIn(ExperimentalTime::class)
class BalanceListViewModel(
    private val walletInteractor: WalletInteractor,
    private val assetsListInteractor: AssetsListInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val router: AssetsRouter,
    private val currencyInteractor: CurrencyInteractor,
    private val balanceBreakdownInteractor: BalanceBreakdownInteractor,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()
        .inBackground()
        .share()

    private val fullSyncActions: List<SyncAction> = listOf(
        { walletInteractor.syncAssetsRates(selectedCurrency.first()) },
        walletInteractor::syncNfts
    )

    private val assetsFlow = walletInteractor.assetsFlow()

    private val filteredAssetsFlow = walletInteractor.filterAssets(assetsFlow)

    private val accountChangeSyncActions: List<SyncAction> = listOf(
        walletInteractor::syncNfts
    )

    private val selectedMetaAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .share()

    val selectedWalletModelFlow = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    private val balanceBreakdown = balanceBreakdownInteractor.balanceBreakdownFlow(assetsFlow)
        .share()

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

    val assetModelsFlow = combine(filteredAssetsFlow, selectedCurrency) { assets, currensy ->
        walletInteractor.groupAssets(assets).mapGroupedAssetsToUi(currensy)
    }
        .distinctUntilChanged()
        .shareInBackground()

    val totalBalanceFlow = balanceBreakdown.map {
        val currency = selectedCurrency.first()
        val assets = assetsFlow.first()
        TotalBalanceModel(
            shouldShowPlaceholder = assets.isEmpty(),
            totalBalanceFiat = it.total.formatAsCurrency(currency),
            lockedBalanceFiat = it.locksTotal.amount.formatAsCurrency(currency)
        )
    }
        .inBackground()
        .share()

    val balanceBreakdownFlow = balanceBreakdown.map {
        val currency = selectedCurrency.first()
        val total = it.total.formatAsCurrency(currency)
        TotalBreakdownModel(total, mapBreakdownToList(it, currency))
    }
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

    fun manageClicked() {
        router.openAssetFilters()
    }

    fun goToNftsClicked() {
        router.openNfts()
    }

    fun searchClicked() {
        router.openAssetSearch()
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

    @OptIn(ExperimentalStdlibApi::class)
    private fun mapBreakdownToList(balanceBreakdown: BalanceBreakdown, currency: Currency): List<BalanceBreakdownItem> {
        return buildList {
            add(
                BalanceBreakdownTotal(
                    resourceManager.getString(R.string.wallet_balance_transferable),
                    balanceBreakdown.transferableTotal.amount.formatAsCurrency(currency),
                    R.drawable.ic_staking_operations,
                    mapPercentage(balanceBreakdown.transferableTotal)
                )
            )

            add(
                BalanceBreakdownTotal(
                    resourceManager.getString(R.string.wallet_balance_locked),
                    balanceBreakdown.locksTotal.amount.formatAsCurrency(currency),
                    R.drawable.ic_lock,
                    mapPercentage(balanceBreakdown.locksTotal)
                )
            )

            val breakdown = balanceBreakdown.breakdown.map {
                BalanceBreakdownAmount(
                    it.chainAsset.symbol + " " + mapBalanceIdToUi(resourceManager, it.id),
                    it.fiatAmount.formatAsCurrency(currency)
                )
            }

            addAll(breakdown)
        }
    }

    private fun mapPercentage(percentageAmount: BalanceBreakdown.PercentageAmount): String {
        return resourceManager.getString(
            R.string.common_percentage,
            percentageAmount.percentage.roundToInt()
        )
    }
}
