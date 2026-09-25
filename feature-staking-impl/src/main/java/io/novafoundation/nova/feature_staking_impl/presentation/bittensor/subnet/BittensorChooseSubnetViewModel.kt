package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal

class BittensorChooseSubnetViewModel(
    private val interactor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    assetIconProvider: AssetIconProvider
) : BaseViewModel() {

    private val catalogue = MutableStateFlow<List<BittensorSubnet>>(emptyList())
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(false)
    val isLoading = loading.asStateFlow()
    private val failed = MutableStateFlow(false)
    val hasError = failed.asStateFlow()
    val rootIcon = interactor.observePortfolio().take(1).map {
        assetIconProvider.getAssetIconOrFallback(it.asset.token.configuration)
    }.onStart { emit(AssetIconProvider.fallbackIcon) }.catch { showError(it) }.shareInBackground()

    private val favorites = interactor.observeFavorites()
    val subnets = combine(catalogue, query, favorites) { subnets, query, favorites ->
        subnets.filter { query.isBlank() || it.name.contains(query, true) || it.symbol.contains(query, true) || it.id.value.toString() == query }
            .sortedWith(compareByDescending<BittensorSubnet> { it.id.value in favorites.subnetIds }.thenBy { it.id.value })
            .map {
                BittensorSubnetRowUi(
                    subnet = it,
                    metaId = favorites.metaId,
                    favorite = it.id.value in favorites.subnetIds,
                    price = it.priceInRao?.let { price -> BigDecimal(price, 9).format() + " TAO" } ?: "—"
                )
            }
    }.shareInBackground()

    val list = combine(rootIcon, subnets, ::bittensorSubnetList).shareInBackground()

    val isEmpty = combine(subnets, isLoading, hasError) { subnets, loading, failed -> subnets.isEmpty() && !loading && !failed }
        .shareInBackground()

    init {
        retry()
    }

    fun search(query: String) {
        this.query.value = query.trim()
    }

    fun retry() = launchUnit {
        if (loading.value) return@launchUnit
        loading.value = true
        failed.value = false
        try {
            catalogue.value = interactor.subnets()
        } catch (e: TimeoutCancellationException) {
            failed.value = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed.value = true
        } finally {
            loading.value = false
        }
    }

    fun select(row: BittensorSubnetRowUi) = router.openBittensorSubnetDetails(row.subnet.id.value)

    fun toggleFavorite(row: BittensorSubnetRowUi) = launchUnit {
        try {
            interactor.toggleFavorite(row.metaId, row.subnet.id)
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
