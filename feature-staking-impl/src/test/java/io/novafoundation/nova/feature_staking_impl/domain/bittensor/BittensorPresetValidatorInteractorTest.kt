package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class BittensorPresetValidatorInteractorTest {

    private val repository = mock(BittensorSubnetValidatorRepository::class.java)

    private val yields = mock(BittensorAlphaYieldRepository::class.java)

    private val chains = mock(ChainRegistry::class.java)

    private val logger = mock(Logger::class.java)

    private val chain = mock(Chain::class.java)

    private val subnet = BittensorNetuid.Subnet(1)

    private val first = candidate(1)

    private val second = candidate(2)

    private val third = candidate(3)

    private val portfolio = mock(BittensorPortfolioRepository::class.java)

    private val interactor = BittensorPresetValidatorInteractor(repository, yields, chains, portfolio, logger)

    @Before
    fun prepare() = runBlocking<Unit> {
        `when`(chains.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        stubDirectory(first, second, third)
        `when`(yields.alphaYields(chain, subnet)).thenReturn(mapOf(first.validator.hotkey to BigDecimal.ONE, second.validator.hotkey to BigDecimal.TEN))
        `when`(portfolio.observePortfolio()).thenReturn(flowOf(portfolio(emptyList())))
    }

    @Test
    fun `known highest yield recommendation is checked and selected`() = runBlocking<Unit> {
        assertEquals(second, interactor.validators(subnet).first())
        verify(repository).validate(chain, subnet, second.validator)
        verify(repository, never()).validate(chain, subnet, first.validator)
    }

    @Test
    fun `inactive highest yield is skipped even when recommendation and gates allow it`() = runBlocking<Unit> {
        stubDirectory(second.copy(inactive = true), first, third)
        assertEquals(first, interactor.validators(subnet).first())
        verify(repository, never()).validate(chain, subnet, second.validator)
    }

    @Test
    fun `unavailable automatic candidate falls back once`() = runBlocking<Unit> {
        doThrow(IllegalStateException("inactive")).`when`(repository).validate(chain, subnet, second.validator)

        assertEquals(first, interactor.validators(subnet).first())
        verify(repository).validate(chain, subnet, first.validator)
    }

    @Test
    fun `two failures are reported without searching the whole directory`() = runBlocking<Unit> {
        doThrow(IllegalStateException("inactive")).`when`(repository).validate(chain, subnet, second.validator)
        doThrow(IllegalStateException("inactive")).`when`(repository).validate(chain, subnet, first.validator)

        val failure = runCatching { interactor.validators(subnet) }.exceptionOrNull()
        assertEquals("No eligible recommended validator in this subnet", failure?.message)
        verify(repository, never()).validate(chain, subnet, third.validator)
    }

    @Test
    fun `missing optional yields do not prevent a preset`() = runBlocking<Unit> {
        `when`(yields.alphaYields(chain, subnet)).thenThrow(IllegalStateException("unavailable"))
        assertEquals(first, interactor.validators(subnet).first())
    }

    @Test(expected = CancellationException::class)
    fun `cancellation does not select a fallback`() = runBlocking<Unit> {
        doThrow(CancellationException()).`when`(repository).validate(chain, subnet, second.validator)
        try {
            interactor.validators(subnet)
        } finally {
            verify(repository, never()).validate(chain, subnet, first.validator)
        }
    }

    @Test
    fun `unrecommended and disabled rows are never preset candidates`() = runBlocking<Unit> {
        stubDirectory(second.copy(recommended = false), first.copy(selectable = false), third)
        assertEquals(third, interactor.validators(subnet).first())
        verify(repository, never()).validate(chain, subnet, second.validator)
    }

    @Test
    fun `preset is the highest reliable APY validator active on the subnet`() = runBlocking<Unit> {
        val yuma = candidate(4)
        val tbai = candidate(5)
        val arbos = candidate(6).copy(inactive = true)
        stubDirectory(arbos, tbai, yuma)
        `when`(yields.alphaYields(chain, subnet)).thenReturn(
            mapOf(yuma.validator.hotkey to BigDecimal("54.24"), tbai.validator.hotkey to BigDecimal("76.85"), arbos.validator.hotkey to BigDecimal("77.8"))
        )

        val result = interactor.preset(subnet)

        assertEquals(tbai, result.preset)
        assertEquals(listOf(arbos, tbai, yuma), result.directory)
        verify(repository, never()).validate(chain, subnet, arbos.validator)
    }

    @Test
    fun `equal or unknown yields follow the ranking instead of the name order like iOS`() = runBlocking<Unit> {
        val alpha = candidate(4).copy(recommendationRank = 2)
        val beta = candidate(5).copy(recommendationRank = 0)
        val gamma = candidate(6).copy(recommendationRank = 1)
        stubDirectory(alpha, beta, gamma)
        `when`(yields.alphaYields(chain, subnet)).thenReturn(mapOf(alpha.validator.hotkey to BigDecimal.TEN, gamma.validator.hotkey to BigDecimal("10.0")))

        assertEquals(gamma, interactor.preset(subnet).preset)

        `when`(yields.alphaYields(chain, subnet)).thenReturn(emptyMap())
        assertEquals(beta, interactor.preset(subnet).preset)
    }

    @Test
    fun `no qualifying recommendation leaves the preset empty instead of failing`() = runBlocking<Unit> {
        stubDirectory(first.copy(recommended = false), second.copy(inactive = true))

        assertNull(interactor.preset(subnet).preset)
        val failure = runCatching { interactor.validators(subnet) }.exceptionOrNull()
        assertEquals("No eligible recommended validator in this subnet", failure?.message)
    }

    @Test
    fun `existing position keeps its largest validator from the chain whatever the recommendation gates`() = runBlocking<Unit> {
        val held = candidate(7).copy(recommended = false)
        `when`(repository.chainValidator(chain, subnet, held.validator.hotkey)).thenReturn(held)
        `when`(portfolio.observePortfolio()).thenReturn(
            flowOf(portfolio(listOf(position(first, 10), position(held, 20), position(second, 30, BittensorNetuid.Subnet(2)))))
        )

        val result = interactor.preset(subnet)

        assertEquals(held, result.preset)
        assertEquals(listOf(held, first, second, third), result.directory)
        verify(repository, never()).validate(chain, subnet, second.validator)
    }

    @Test
    fun `existing validator is preset even when recommendations cannot be read`() = runBlocking<Unit> {
        val held = candidate(7)
        stubDirectory(first, held.copy(selectable = false), failure = IllegalStateException("503"))
        `when`(repository.chainValidator(chain, subnet, held.validator.hotkey)).thenReturn(held)
        `when`(portfolio.observePortfolio()).thenReturn(flowOf(portfolio(listOf(position(held, 20)))))

        val result = interactor.preset(subnet)

        assertEquals(held, result.preset)
        assertEquals(listOf(first, held), result.directory)
    }

    @Test
    fun `equal existing stakes resolve to the lexicographically smaller hotkey like iOS`() {
        val low = candidate(1)
        val high = candidate(0x90)
        val positions = listOf(position(high, 20), position(low, 20))

        assertEquals(low.validator.hotkey, bittensorPrimaryHotkey(positions, subnet))
        assertEquals(high.validator.hotkey, bittensorPrimaryHotkey(listOf(position(high, 21), position(low, 20)), subnet))
    }

    @Test
    fun `equal root stakes resolve to the lexicographically smaller hotkey like iOS`() {
        val low = candidate(1).validator.hotkey
        val high = candidate(0x90).validator.hotkey

        assertEquals(low, bittensorRootPrimaryHotkey(listOf(BittensorRootPosition(high, BigInteger.TEN), BittensorRootPosition(low, BigInteger.TEN))))
        assertEquals(high, bittensorRootPrimaryHotkey(listOf(BittensorRootPosition(high, BigInteger.TEN), BittensorRootPosition(low, BigInteger.ONE))))
    }

    @Test
    fun `inactive or unreadable existing validator falls back to the recommendation`() = runBlocking<Unit> {
        val held = candidate(7)
        `when`(repository.chainValidator(chain, subnet, held.validator.hotkey)).thenReturn(held.copy(inactive = true))
        `when`(portfolio.observePortfolio()).thenReturn(flowOf(portfolio(listOf(position(held, 20)))))

        assertEquals(second, interactor.preset(subnet).preset)
        `when`(repository.chainValidator(chain, subnet, held.validator.hotkey)).thenThrow(IllegalStateException("offline"))
        assertEquals(second, interactor.preset(subnet).preset)
    }

    @Test
    fun `unavailable positions do not block the recommended preset`() = runBlocking<Unit> {
        `when`(portfolio.observePortfolio()).thenReturn(flow { throw IllegalStateException("offline") })

        assertEquals(second, interactor.preset(subnet).preset)
    }

    @Test
    fun `unavailable directory or recommendations are reported for retry`() = runBlocking<Unit> {
        stubDirectory(first, second, failure = IllegalStateException("503"))
        assertEquals("503", runCatching { interactor.preset(subnet) }.exceptionOrNull()?.message)

        `when`(repository.directory(chain, subnet)).thenThrow(IllegalStateException("offline"))
        assertEquals("offline", runCatching { interactor.preset(subnet) }.exceptionOrNull()?.message)
    }

    private suspend fun stubDirectory(vararg rows: BittensorSubnetValidator, failure: Throwable? = null) {
        val status = mock(BittensorValidatorDirectoryStatus::class.java)
        `when`(repository.directory(chain, subnet)).thenReturn(BittensorValidatorDirectory(rows.toList(), status, failure))
    }

    private fun portfolio(subnets: List<BittensorSubnetPosition>) = BittensorPortfolio(
        1L,
        chain,
        mock(Asset::class.java),
        BittensorPortfolioPositions(emptyList(), subnets, emptyMap()),
        BittensorDataStatus.Fresh
    )

    private fun position(validator: BittensorSubnetValidator, alpha: Long, netuid: BittensorNetuid.Subnet = subnet) =
        BittensorSubnetPosition(netuid, validator.validator.hotkey, BigInteger.valueOf(alpha))

    private fun candidate(id: Int): BittensorSubnetValidator {
        val hotkey = AccountIdKey(ByteArray(32) { id.toByte() })
        val validator = BittensorValidator(hotkey, "Validator $id")
        return BittensorSubnetValidator(validator, "address-$id", BigDecimal.ZERO, selectable = true, recommended = true)
    }
}
