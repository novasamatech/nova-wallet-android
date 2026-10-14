package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.onError
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import kotlinx.coroutines.CancellationException
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart

class BittensorChooseSubnetViewModel(
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    assetIconProvider: AssetIconProvider,
    private val logger: Logger,
    private val tokenFormatter: TokenFormatter
) : BaseViewModel() {

    private val reload = MutableStateFlow(0L)

    private val catalogueState = reload.flatMapLatest {
        flowOf { bittensorSubnetInteractor.subnets() }
            .withSafeLoading()
            .onError {
                logger.log("[BittensorChooseSubnet] Failed to load catalogue")
                logger.log(it)
            }
    }.shareInBackground()

    private val catalogue = catalogueState.filterLoaded().onStart { emit(emptyList()) }.shareInBackground()

    private val query = MutableStateFlow("")

    val isLoading = catalogueState.map { it.isLoading() }.shareInBackground()

    val hasError = catalogueState.map { it.isError() }.shareInBackground()

    private val asset = bittensorSubnetInteractor.observeAsset().catch { showError(it) }.shareInBackground()

    val rootIcon = asset.map {
        assetIconProvider.getAssetIconOrFallback(it.token.configuration)
    }.onStart { emit(AssetIconProvider.fallbackIcon) }.catch { showError(it) }.shareInBackground()

    private val favorites = bittensorSubnetInteractor.observeFavorites()

    val subnets = combine(catalogue, query, favorites, asset) { subnets, query, favorites, asset ->
        subnets.filter { query.isBlank() || it.name.contains(query, true) || it.symbol.contains(query, true) || it.id.value.toString() == query }
            .sortedWith(compareByDescending<BittensorSubnet> { it.id.value in favorites.subnetIds }.thenBy { it.id.value })
            .map {
                BittensorSubnetRowUi(
                    subnet = it,
                    metaId = favorites.metaId,
                    favorite = it.id.value in favorites.subnetIds,
                    price = it.priceInRao?.let { price -> tokenFormatter.formatToken(price, asset).toString() } ?: "—"
                )
            }
    }.shareInBackground()

    val list = combine(rootIcon, subnets, asset) { icon, subnets, asset ->
        bittensorSubnetList(icon, subnets, asset.token.configuration.symbol)
    }.shareInBackground()

    val isEmpty = combine(subnets, isLoading, hasError) { subnets, loading, failed -> subnets.isEmpty() && !loading && !failed }
        .shareInBackground()

    fun search(query: String) {
        this.query.value = query.trim()
    }

    fun retry() = launchUnit {
        if (!isLoading.first()) reload.update { it + 1 }
    }

    fun select(row: BittensorSubnetRowUi) = router.openBittensorSubnetDetails(row.subnet.id.value)

    fun toggleFavorite(row: BittensorSubnetRowUi) = launchUnit {
        try {
            bittensorSubnetInteractor.toggleFavorite(row.metaId, row.subnet.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        }
    }

    fun rootStaking() = router.openBittensorRootStaking()

    fun back() = router.back()
}

data class BittensorSubnetRowUi(val subnet: BittensorSubnet, val metaId: Long, val favorite: Boolean, val price: String)
