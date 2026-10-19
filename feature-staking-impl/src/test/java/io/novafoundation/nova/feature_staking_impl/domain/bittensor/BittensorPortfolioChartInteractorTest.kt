package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorPortfolioChartInteractorTest {

    private val repository = mock(BittensorPortfolioHistoryRepository::class.java)

    private val calculator = BittensorPortfolioChartCalculator()

    private val accounts = mock(AccountRepository::class.java)

    private val chains = mock(ChainRegistry::class.java)

    private val interactor = BittensorPortfolioChartInteractor(repository, calculator, accounts, chains)

    private val chain = mock(Chain::class.java)

    private val asset = mock(Asset::class.java)

    private val point = BittensorPortfolioHistoryPoint(1000, BigDecimal("12"), BigDecimal("4800"), true)

    private val status = BittensorPortfolioHistoryStatus(BigDecimal.ZERO, true)

    private val account = BittensorAccountHistory(BittensorPortfolioPeriod.ONE_DAY, 0, 2000, listOf(point), status)

    private val history = BittensorPortfolioHistory(account, null)

    @Test
    fun `no holdings still returns historical points`() {
        assertHistory(null)
    }

    @Test
    fun `loading or stale zero holdings cannot add a false drop to zero`() {
        val empty = BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap())
        val loading = BittensorPortfolio(1L, chain, asset, empty, BittensorDataStatus.Loading)
        val stale = loading.copy(dataStatus = BittensorDataStatus.Stale(IllegalStateException("Read failed")))
        val expected = listOf(BittensorPortfolioValuePoint(1000, point.tao, point.usd))

        assertEquals(expected, interactor.calculate(loading, history).points)
        assertEquals(expected, interactor.calculate(stale, history).points)
    }

    @Test
    fun `incomplete total still returns historical points`() {
        val subnetId = BittensorNetuid.Subnet(1)
        val hotkey = AccountIdKey(ByteArray(32))
        val subnet = BittensorSubnetPosition(subnetId, hotkey, BigInteger.ONE)
        val positions = BittensorPortfolioPositions(emptyList(), listOf(subnet), emptyMap())
        assertHistory(positions)
    }

    @Test
    fun `missing current rate still returns history without zero anchor`() {
        val token = mock(Token::class.java)
        val configuration = mock(Chain.Asset::class.java)
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(configuration)
        `when`(configuration.precision).thenReturn(Precision(9))
        val hotkey = AccountIdKey(ByteArray(32))
        val root = BittensorRootPosition(hotkey, BigInteger.TEN.pow(9))
        val positions = BittensorPortfolioPositions(listOf(root), emptyList(), emptyMap())
        assertHistory(positions)
    }

    private fun assertHistory(positions: BittensorPortfolioPositions?) {
        val portfolio = BittensorPortfolio(1L, chain, asset, positions, BittensorDataStatus.Loading)
        val result = interactor.calculate(portfolio, history)
        assertEquals(listOf(BittensorPortfolioValuePoint(1000, point.tao, point.usd)), result.points)
    }
}
