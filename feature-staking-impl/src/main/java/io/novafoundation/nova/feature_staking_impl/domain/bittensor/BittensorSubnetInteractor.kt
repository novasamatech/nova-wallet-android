package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetFavoritesRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import java.math.BigDecimal
import java.math.BigInteger

data class BittensorSubnet(
    val id: BittensorSubnetId,
    val name: String,
    val symbol: String,
    val logoUrl: String?,
    val priceInRao: BigInteger? = null,
    val taoReserveInPlanks: BigInteger? = null
)

class BittensorSubnetInteractor(
    private val repository: BittensorSubnetRepository,
    private val portfolioRepository: BittensorPortfolioRepository,
    private val bittensorSubnetQuoteInteractor: BittensorSubnetQuoteInteractor,
    private val favorites: BittensorSubnetFavoritesRepository
) {

    val slippage = BittensorSlippage(BigDecimal("0.005"))

    suspend fun subnets() = repository.subnets()

    suspend fun subnet(subnetId: BittensorSubnetId): BittensorSubnet = repository.subnets().first { it.id == subnetId }

    fun observePortfolio() = portfolioRepository.observePortfolio()

    fun observeAsset() = portfolioRepository.observeAsset()

    fun observeFavorites() = favorites.observeFavorites()

    suspend fun toggleFavorite(metaId: Long, subnetId: BittensorSubnetId) = favorites.toggle(metaId, subnetId)

    suspend fun quote(subnetId: BittensorSubnetId, amount: BigInteger): BittensorSubnetBuyQuote =
        bittensorSubnetQuoteInteractor.quoteBuy(ChainGeneses.BITTENSOR, subnetId, amount, slippage)
}
