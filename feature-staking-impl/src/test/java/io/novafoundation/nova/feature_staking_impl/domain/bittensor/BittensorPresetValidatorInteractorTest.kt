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
        `when`(repository.validators(chain, subnet)).thenReturn(listOf(first, second, third))
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
        `when`(repository.validators(chain, subnet)).thenReturn(listOf(second.copy(inactive = true), first, third))
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
        `when`(repository.validators(chain, subnet)).thenReturn(listOf(second.copy(recommended = false), first.copy(selectable = false), third))
        assertEquals(third, interactor.validators(subnet).first())
        verify(repository, never()).validate(chain, subnet, second.validator)
    }

    @Test
    fun `preset is the highest reliable APY validator active on the subnet`() = runBlocking<Unit> {
        val yuma = candidate(4)
        val tbai = candidate(5)
        val arbos = candidate(6).copy(inactive = true)
        `when`(repository.validators(chain, subnet)).thenReturn(listOf(arbos, tbai, yuma))
        `when`(yields.alphaYields(chain, subnet)).thenReturn(
            mapOf(yuma.validator.hotkey to BigDecimal("54.24"), tbai.validator.hotkey to BigDecimal("76.85"), arbos.validator.hotkey to BigDecimal("77.8"))
        )

        val result = interactor.preset(subnet)

        assertEquals(tbai, result.preset)
        assertEquals(listOf(arbos, tbai, yuma), result.directory)
        verify(repository, never()).validate(chain, subnet, arbos.validator)
    }

    @Test
    fun `no qualifying recommendation leaves the preset empty instead of failing`() = runBlocking<Unit> {
        `when`(repository.validators(chain, subnet)).thenReturn(listOf(first.copy(recommended = false), second.copy(inactive = true)))

        assertNull(interactor.preset(subnet).preset)
        val failure = runCatching { interactor.validators(subnet) }.exceptionOrNull()
        assertEquals("No eligible recommended validator in this subnet", failure?.message)
    }

    @Test
    fun `existing position keeps its largest active validator`() = runBlocking<Unit> {
        val held = candidate(7).copy(recommended = false)
        `when`(repository.validators(chain, subnet)).thenReturn(listOf(first, second, held))
        `when`(portfolio.observePortfolio()).thenReturn(
            flowOf(portfolio(listOf(position(first, 10), position(held, 20), position(second, 30, BittensorNetuid.Subnet(2)))))
        )

        assertEquals(held, interactor.preset(subnet).preset)
        verify(repository, never()).validate(chain, subnet, second.validator)
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
    fun `inactive existing validator falls back to the recommendation`() = runBlocking<Unit> {
        val held = candidate(7).copy(inactive = true)
        `when`(repository.validators(chain, subnet)).thenReturn(listOf(first, second, held))
        `when`(portfolio.observePortfolio()).thenReturn(flowOf(portfolio(listOf(position(held, 20)))))

        assertEquals(second, interactor.preset(subnet).preset)
    }

    @Test
    fun `unavailable positions do not block the recommended preset`() = runBlocking<Unit> {
        `when`(portfolio.observePortfolio()).thenReturn(flow { throw IllegalStateException("offline") })

        assertEquals(second, interactor.preset(subnet).preset)
    }

    @Test
    fun `unavailable recommendations are reported for retry`() = runBlocking<Unit> {
        `when`(repository.validators(chain, subnet)).thenThrow(IllegalStateException("503"))

        assertEquals("503", runCatching { interactor.preset(subnet) }.exceptionOrNull()?.message)
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
