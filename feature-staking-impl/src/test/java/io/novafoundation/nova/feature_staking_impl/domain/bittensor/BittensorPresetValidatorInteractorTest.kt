package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRecommendationsRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.runtime.ext.addressOf
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

    private val backend = mock(BittensorValidatorBackend::class.java)

    private val chains = mock(ChainRegistry::class.java)

    private val logger = mock(Logger::class.java)

    private val chain = mock(Chain::class.java)

    private val subnet = BittensorNetuid.Subnet(1)

    private val first = candidate(1)

    private val second = candidate(2)

    private val third = candidate(3)

    private val portfolio = mock(BittensorPortfolioRepository::class.java)

    private val interactor = BittensorPresetValidatorInteractor(repository, yields, backend, chains, portfolio, logger)

    @Before
    fun prepare() = runBlocking<Unit> {
        `when`(chains.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        stubDirectory(first, second, third)
        `when`(yields.alphaYields(chain, subnet)).thenReturn(mapOf(first.validator.hotkey to BigDecimal.ONE, second.validator.hotkey to BigDecimal.TEN))
        `when`(portfolio.observePortfolio()).thenReturn(flowOf(portfolio(emptyList())))
    }

    @Test
    fun `known highest yield recommendation is checked and selected`() = runBlocking<Unit> {
        assertEquals(second, interactor.preset(subnet).preset)
        verify(repository).validate(chain, subnet, second.validator)
        verify(repository, never()).validate(chain, subnet, first.validator)
    }

    @Test
    fun `inactive highest yield is skipped even when recommendation and gates allow it`() = runBlocking<Unit> {
        stubDirectory(second.copy(inactive = true), first, third)
        assertEquals(first, interactor.preset(subnet).preset)
        verify(repository, never()).validate(chain, subnet, second.validator)
    }

    @Test
    fun `unavailable automatic candidate falls back once`() = runBlocking<Unit> {
        doThrow(IllegalStateException("inactive")).`when`(repository).validate(chain, subnet, second.validator)

        assertEquals(first, interactor.preset(subnet).preset)
        verify(repository).validate(chain, subnet, first.validator)
    }

    @Test
    fun `two failures leave the preset empty without searching the whole directory`() = runBlocking<Unit> {
        doThrow(IllegalStateException("inactive")).`when`(repository).validate(chain, subnet, second.validator)
        doThrow(IllegalStateException("inactive")).`when`(repository).validate(chain, subnet, first.validator)

        assertNull(interactor.preset(subnet).preset)
        verify(repository, never()).validate(chain, subnet, third.validator)
    }

    @Test
    fun `missing optional yields do not prevent a preset`() = runBlocking<Unit> {
        `when`(yields.alphaYields(chain, subnet)).thenThrow(IllegalStateException("unavailable"))
        assertEquals(first, interactor.preset(subnet).preset)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation does not select a fallback`() = runBlocking<Unit> {
        doThrow(CancellationException()).`when`(repository).validate(chain, subnet, second.validator)
        try {
            interactor.preset(subnet)
        } finally {
            verify(repository, never()).validate(chain, subnet, first.validator)
        }
    }

    @Test
    fun `unrecommended and disabled rows are never preset candidates`() = runBlocking<Unit> {
        stubDirectory(second.copy(recommended = false), first.copy(selectable = false), third)
        assertEquals(third, interactor.preset(subnet).preset)
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

    @Test
    fun `primary root position takes priority over the recommended preset without reading recommendations`() = runBlocking<Unit> {
        `when`(repository.chainValidator(chain, BittensorNetuid.Root, second.validator.hotkey)).thenReturn(second)
        val positions = listOf(
            BittensorRootPosition(first.validator.hotkey, BigInteger.ONE),
            BittensorRootPosition(second.validator.hotkey, BigInteger.TEN)
        )

        assertEquals(second.validator, interactor.rootPreset(chain, positions))
        verify(repository, never()).directory(chain, BittensorNetuid.Root)
    }

    @Test
    fun `new root position presets the recommended root validator with the highest root APY`() = runBlocking<Unit> {
        stubDirectory(first, second, netuid = BittensorNetuid.Root)
        val rootValidators = BittensorRootValidatorRemote(listOf(rootRow(first, "5"), rootRow(second, "9")), rootMeta())
        `when`(backend.rootValidators()).thenReturn(rootValidators)

        assertEquals(second.validator, interactor.rootPreset(chain, emptyList()))
        verify(repository).validate(chain, BittensorNetuid.Root, second.validator)
        verify(repository, never()).validate(chain, BittensorNetuid.Root, first.validator)
    }

    @Test
    fun `existing root validator the chain cannot read falls back to the recommendation`() = runBlocking {
        stubDirectory(first, second, netuid = BittensorNetuid.Root)
        `when`(repository.chainValidator(chain, BittensorNetuid.Root, second.validator.hotkey)).thenThrow(IllegalStateException("offline"))

        assertEquals(first.validator, interactor.rootPreset(chain, listOf(BittensorRootPosition(second.validator.hotkey, BigInteger.TEN))))
    }

    @Test
    fun `no selectable root recommendation leaves the validator unselected for manual choice`() = runBlocking {
        stubDirectory(first.copy(recommended = false), second.copy(selectable = false), netuid = BittensorNetuid.Root)

        assertNull(interactor.rootPreset(chain, emptyList()))
    }

    @Test
    fun `root directory failure cannot choose an arbitrary validator`() = runBlocking {
        stubDirectory(first, failure = IllegalStateException("Recommendations unavailable"), netuid = BittensorNetuid.Root)

        assertEquals("Recommendations unavailable", runCatching { interactor.rootPreset(chain, emptyList()) }.exceptionOrNull()?.message)
    }

    private suspend fun stubDirectory(vararg rows: BittensorSubnetValidator, failure: Throwable? = null, netuid: BittensorNetuid = subnet) {
        val status = mock(BittensorValidatorDirectoryStatus::class.java)
        `when`(repository.directory(chain, netuid)).thenReturn(BittensorValidatorDirectory(rows.toList(), status, failure))
    }

    private fun rootRow(validator: BittensorSubnetValidator, apy: String) = BittensorRootValidatorRemote.Validator(
        chain.addressOf(validator.validator.hotkey),
        validator.validator.name,
        BittensorRootValidatorRemote.Yield(BittensorRootValidatorRemote.Window(apy)),
        "100"
    )

    private fun rootMeta(): BittensorRootValidatorRemote.Meta {
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH", "2026-10-19T00:00:00Z")
        return BittensorRootValidatorRemote.Meta(BittensorRootValidatorRemote.Components(component, component))
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
