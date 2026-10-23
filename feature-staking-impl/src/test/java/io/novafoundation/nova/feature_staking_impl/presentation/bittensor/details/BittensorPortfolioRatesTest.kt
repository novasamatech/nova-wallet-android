package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorPortfolioRatesTest {

    private val hotkey = AccountIdKey(ByteArray(32))
    private val subnetId = BittensorNetuid.Subnet(1)
    private val root = BittensorRootPosition(hotkey, BigInteger.TEN)
    private val subnet = BittensorSubnetPosition(subnetId, hotkey, BigInteger.TEN)
    private val priced = BittensorPortfolioPositions(listOf(root), listOf(subnet), mapOf(subnetId to BigInteger.ONE))
    private val metadata = ExtendedLoadingState.Loaded(Unit)
    private val apy = ExtendedLoadingState.Loaded<BigDecimal?>(BigDecimal.ONE)

    @Test
    fun `rates are available while everything resolves`() {
        assertEquals(BittensorPortfolioRates.AVAILABLE, bittensorPortfolioRates(priced, metadata, apy))
        assertEquals(BittensorPortfolioRates.AVAILABLE, bittensorPortfolioRates(priced, ExtendedLoadingState.Loading, ExtendedLoadingState.Loading))
        assertEquals(BittensorPortfolioRates.AVAILABLE, bittensorPortfolioRates(null, ExtendedLoadingState.Error(Exception()), apy))
    }

    @Test
    fun `missing subnet rates hide the chart`() {
        val failedCatalogue = ExtendedLoadingState.Error(IllegalStateException())
        assertEquals(BittensorPortfolioRates.SUBNETS_UNAVAILABLE, bittensorPortfolioRates(priced, failedCatalogue, apy))
        val unpriced = priced.copy(subnetPrices = emptyMap())
        assertEquals(BittensorPortfolioRates.SUBNETS_UNAVAILABLE, bittensorPortfolioRates(unpriced, metadata, apy))
    }

    @Test
    fun `missing root rate keeps the chart and needs root stake`() {
        val noRate = ExtendedLoadingState.Loaded<BigDecimal?>(null)
        assertEquals(BittensorPortfolioRates.ROOT_UNAVAILABLE, bittensorPortfolioRates(priced, metadata, noRate))
        assertEquals(BittensorPortfolioRates.ROOT_UNAVAILABLE, bittensorPortfolioRates(priced, metadata, ExtendedLoadingState.Error(Exception())))
        assertEquals(BittensorPortfolioRates.AVAILABLE, bittensorPortfolioRates(priced.copy(root = emptyList()), metadata, noRate))
    }

    @Test
    fun `catalogue failure without subnet holdings is not a rates problem`() {
        val rootOnly = priced.copy(subnets = emptyList())
        assertEquals(BittensorPortfolioRates.AVAILABLE, bittensorPortfolioRates(rootOnly, ExtendedLoadingState.Error(Exception()), apy))
    }

    @Test
    fun `no banner while cached positions are refreshing`() {
        val failedCatalogue = ExtendedLoadingState.Error(IllegalStateException())
        assertEquals(BittensorPortfolioRates.AVAILABLE, bittensorPortfolioRates(priced, failedCatalogue, apy, isPortfolioLoading = true))
    }
}
