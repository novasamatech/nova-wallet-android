package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.ltrIsolated
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorPortfolioRowMapperTest {

    private val resources = mock(ResourceManager::class.java)
    private val icons = mock(AssetIconProvider::class.java)
    private val fiat = mock(FiatFormatter::class.java)
    private val tokens = object : TokenFormatter {
        override fun formatToken(amount: BigDecimal, token: TokenSymbol, config: TokenConfig): CharSequence =
            "${amount.stripTrailingZeros().toPlainString()} ${token.value}"
    }
    private val amounts = BittensorAmountFormatter(tokens, fiat, resources)
    private val mapper = BittensorPortfolioRowMapper(resources, amounts, icons)
    private val chain = mock(Chain::class.java)
    private val asset = mock(Asset::class.java)
    private val token = mock(Token::class.java)
    private val chainAsset = mock(Chain.Asset::class.java)
    private val currency = mock(Currency::class.java)
    private val subnetId = BittensorNetuid.Subnet(1)
    private val subnet = BittensorSubnet(subnetId, "Subnet", "ALPHA", null)
    private val position = BittensorSubnetPosition(subnetId, AccountIdKey(ByteArray(32)), BigInteger.valueOf(2_000_000_000))
    private val holdings = BittensorPortfolioPositions(emptyList(), listOf(position), mapOf(subnetId to BigInteger.TEN.pow(9)))
    private val portfolio = BittensorPortfolio(1, chain, asset, holdings, BittensorDataStatus.Fresh)

    @Before
    fun prepare() {
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(token.currency).thenReturn(currency)
        `when`(chainAsset.precision).thenReturn(Precision(9))
        `when`(chainAsset.symbol).thenReturn(TokenSymbol("NATIVE"))
        `when`(resources.getString(R.string.bittensor_root_staking)).thenReturn("Root")
        `when`(resources.getString(R.string.bittensor_subnet_number, 1)).thenReturn("Subnet 1")
        `when`(resources.getString(R.string.bittensor_chart_week)).thenReturn("7D")
        `when`(resources.getString(R.string.bittensor_weekly_change, "+0%", "7D")).thenReturn("+0% 7D")
        `when`(resources.getString(R.string.bittensor_weekly_change, "-16.4%", "7D")).thenReturn("-16.4% 7D")
        `when`(resources.getString(R.string.bittensor_portfolio_root_apy, "0%")).thenReturn("APY 0% · 30 days")
        `when`(resources.getString(R.string.bittensor_portfolio_root_apy, "—")).thenReturn("APY — · 30 days")
        `when`(resources.getString(R.string.bittensor_portfolio_root_claimable, "0.000000001 NATIVE")).thenReturn("Claimable rewards: 0.000000001 NATIVE")
    }

    @Test
    fun `mixed rewards only root and subnet display claimable rewards alpha and zero weekly change`() {
        val rows = mapper.map(
            portfolio,
            ExtendedLoadingState.Loaded(mapOf(subnetId to subnet)),
            BigInteger.ONE,
            ExtendedLoadingState.Loaded(BigDecimal.ZERO),
            ExtendedLoadingState.Loaded(mapOf(subnetId to BigDecimal.ZERO))
        )
        assertEquals(listOf("root", "subnet-1"), rows.map { it.id })
        assertEquals("0 NATIVE", rows[0].amount.token)
        assertEquals("Claimable rewards: 0.000000001 NATIVE", rows[0].subtitle)
        assertFalse(rows[0].isSubtitleLoading)
        assertEquals("2 ALPHA".ltrIsolated(), rows[1].subtitle)
        assertEquals("≈ 2 NATIVE", rows[1].amount.token)
        assertNull(rows[1].amount.fiat)
        assertEquals("+0% 7D", rows[1].change?.text)
        assertFalse(rows[1].isChangeLoading)
    }

    @Test
    fun `staked root row shows the 30 day APY with a dash for an unknown rate and nothing when rates failed`() {
        assertEquals("APY 0% · 30 days", stakedRootSubtitle(ExtendedLoadingState.Loaded(BigDecimal.ZERO)))
        assertEquals("APY — · 30 days", stakedRootSubtitle(ExtendedLoadingState.Loaded(null)))
        assertNull(stakedRootSubtitle(ExtendedLoadingState.Error(IllegalStateException("Unavailable"))))
    }

    @Test
    fun `rewards only root row does not wait for APY and zero rewards drop the row`() {
        val rows = mapper.map(portfolio, ExtendedLoadingState.Loading, BigInteger.ONE, ExtendedLoadingState.Loading, ExtendedLoadingState.Loading)
        assertEquals("Claimable rewards: 0.000000001 NATIVE", rows.first().subtitle)
        assertFalse(rows.first().isSubtitleLoading)

        val withoutRewards = mapper.map(portfolio, ExtendedLoadingState.Loading, BigInteger.ZERO, ExtendedLoadingState.Loading, ExtendedLoadingState.Loading)
        assertEquals(listOf("subnet-1"), withoutRewards.map { it.id })
    }

    @Test
    fun `metadata and weekly loading are distinct from settled unavailable`() {
        val loading = mapper.map(portfolio, ExtendedLoadingState.Loading, null, ExtendedLoadingState.Loading, ExtendedLoadingState.Loading).single()
        assertTrue(loading.isSubtitleLoading)
        assertNull(loading.subtitle)
        val resolved = mapper.map(
            portfolio,
            ExtendedLoadingState.Loaded(mapOf(subnetId to subnet)),
            null,
            ExtendedLoadingState.Loaded(null),
            ExtendedLoadingState.Loading
        ).single()
        assertEquals("2 ALPHA".ltrIsolated(), resolved.subtitle)
        assertTrue(resolved.isChangeLoading)
        val failed = mapper.map(
            portfolio,
            ExtendedLoadingState.Loaded(mapOf(subnetId to subnet)),
            null,
            ExtendedLoadingState.Loaded(null),
            ExtendedLoadingState.Error(IllegalStateException("Unavailable"))
        ).single()
        assertFalse(failed.isChangeLoading)
        assertNull(failed.change)
        assertEquals("2 ALPHA".ltrIsolated(), failed.subtitle)
    }


    @Test
    fun `weekly change has one decimal and a negative change is falling`() {
        val row = mapper.map(
            portfolio,
            ExtendedLoadingState.Loaded(mapOf(subnetId to subnet)),
            null,
            ExtendedLoadingState.Loaded(null),
            ExtendedLoadingState.Loaded(mapOf(subnetId to BigDecimal("-16.43")))
        ).single()
        assertEquals("-16.4% 7D", row.change?.text)
        assertFalse(row.change!!.isRising)
    }

    @Test
    fun `subnet without a price keeps the dash without an approximation sign`() {
        val unpriced = portfolio.copy(positions = holdings.copy(subnetPrices = emptyMap()))
        val row = mapper.map(unpriced, ExtendedLoadingState.Loaded(mapOf(subnetId to subnet)), null, ExtendedLoadingState.Loaded(null), ExtendedLoadingState.Loading).single()
        assertEquals("—", row.amount.token)
    }

    private fun stakedRootSubtitle(apy: ExtendedLoadingState<BigDecimal?>): CharSequence? {
        val root = BittensorRootPosition(AccountIdKey(ByteArray(32)), BigInteger.ONE)
        val staked = portfolio.copy(positions = holdings.copy(root = listOf(root)))
        return mapper.map(staked, ExtendedLoadingState.Loading, null, apy, ExtendedLoadingState.Loading).first().subtitle
    }
}
