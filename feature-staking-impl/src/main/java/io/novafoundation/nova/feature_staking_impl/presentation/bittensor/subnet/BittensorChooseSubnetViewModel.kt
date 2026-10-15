package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetFavorites
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class BittensorChooseSubnetViewModel(
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    assetIconProvider: AssetIconProvider,
    private val tokenFormatter: TokenFormatter,
    private val retryController: BittensorRetryController
) : BaseViewModel() {

    val retryEvents = retryController.events

    private val catalogueState = retryController.observe("subnet catalogue") { flowOf { bittensorSubnetInteractor.subnets() } }.shareInBackground()

    private val catalogue = catalogueState.filterLoaded().onStart { emit(emptyList()) }.shareInBackground()

    private val query = MutableStateFlow("")

    private val assetState = retryController.observe("native asset", bittensorSubnetInteractor::observeAsset).shareInBackground()

    private val asset = assetState.filterLoaded().shareInBackground()

    val isLoading = combine(catalogueState, assetState) { catalogue, asset -> catalogue.isLoading() || asset.isLoading() }.shareInBackground()

    private val hasError = combine(catalogueState, assetState) { catalogue, asset -> catalogue.isError() || asset.isError() }.shareInBackground()

    private val rootIcon = asset.map {
        assetIconProvider.getAssetIconOrFallback(it.token.configuration)
    }.onStart { emit(AssetIconProvider.fallbackIcon) }.shareInBackground()

    private val favorites = bittensorSubnetInteractor.observeFavorites()

    private val subnets = combine(catalogue, query, favorites, asset, ::mapSubnetRows).shareInBackground()

    val list = combine(rootIcon, subnets, asset) { icon, subnets, asset ->
        bittensorSubnetList(icon, subnets, asset.token.configuration.symbol)
    }.shareInBackground()

    val isEmpty = combine(subnets, isLoading, hasError) { subnets, loading, failed -> subnets.isEmpty() && !loading && !failed }
        .shareInBackground()

    fun search(query: String) {
        this.query.value = query.trim()
    }

    fun select(row: BittensorSubnetRowUi) = router.openBittensorSubnetDetails(BittensorSubnetDetailsPayload(row.subnet.id.value))

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

    private fun mapSubnetRows(
        subnets: List<BittensorSubnet>,
        query: String,
        favorites: BittensorSubnetFavorites,
        asset: Asset
    ): List<BittensorSubnetRowUi> {
        return subnets
            .filter { query.isBlank() || it.name.contains(query, true) || it.symbol.contains(query, true) || it.id.value.toString() == query }
            .sortedWith(compareByDescending<BittensorSubnet> { it.id.value in favorites.subnetIds }.thenBy { it.id.value })
            .map { subnet ->
                BittensorSubnetRowUi(
                    subnet = subnet,
                    metaId = favorites.metaId,
                    favorite = subnet.id.value in favorites.subnetIds,
                    price = subnet.priceInRao?.let { tokenFormatter.formatToken(it, asset).toString() } ?: "—"
                )
            }
    }
}

data class BittensorSubnetRowUi(val subnet: BittensorSubnet, val metaId: Long, val favorite: Boolean, val price: String)
