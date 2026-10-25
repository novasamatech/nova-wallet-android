package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRankedValidatorsRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRecommendationsRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetCatalogueRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorsRemote
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class BittensorValidatorPreviewTest {

    private val repository = mock(BittensorSubnetValidatorRepository::class.java)

    private val chainRegistry = mock(ChainRegistry::class.java)

    private val yields = mock(BittensorAlphaYieldRepository::class.java)

    private val backend = mock(BittensorValidatorBackend::class.java)

    private val subnetCatalogue = mock(BittensorSubnetCatalogueRepository::class.java)

    private val chain = mock(Chain::class.java)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val subnet = BittensorNetuid.Subnet(92)

    private val interactor = BittensorValidatorInteractor(repository, chainRegistry, yields, backend, subnetCatalogue)

    @Before
    fun setup(): Unit = runBlocking {
        `when`(chain.addressPrefix).thenReturn(42)
        `when`(chainRegistry.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        `when`(backend.rankedValidators(subnet.value)).thenReturn(BittensorRankedValidatorsRemote(null, emptyList()))
        val row = BittensorValidatorsRemote.Item(subnet.value, chain.addressOf(hotkey), " Directory validator ", null)
        `when`(backend.validators(subnet.value)).thenReturn(BittensorValidatorsRemote(listOf(row)))
    }

    @Test
    fun `directory card remains visible without recommendations but cannot automatically preselect`() = runBlocking {
        val result = interactor.previews(subnet, null).single()
        assertEquals(hotkey, result.validator.hotkey)
        assertEquals("Directory validator", result.validator.name)
        assertFalse(result.canPreselect)
        verifyNoInteractions(repository, yields)
    }

    @Test
    fun `recommended card can supply an amount preset without loading runtime eligibility`() = runBlocking {
        val row = BittensorRankedValidatorsRemote.Item(subnet.value, chain.addressOf(hotkey), "Recommended", true, "balanced", null)
        `when`(backend.rankedValidators(subnet.value)).thenReturn(BittensorRankedValidatorsRemote(recommendationMeta("FRESH"), listOf(row)))
        val result = interactor.previews(subnet, null).single()
        assertEquals("Recommended", result.validator.name)
        assertTrue(result.canPreselect)
        verifyNoInteractions(repository, yields)
    }

    @Test
    fun `stale recommendation generation still supplies the recommended card`() = runBlocking {
        val row = BittensorRankedValidatorsRemote.Item(subnet.value, chain.addressOf(hotkey), "Recommended", true, "balanced", null)
        `when`(backend.rankedValidators(subnet.value)).thenReturn(BittensorRankedValidatorsRemote(recommendationMeta("STALE"), listOf(row)))
        val result = interactor.previews(subnet, null).single()
        assertEquals("Recommended", result.validator.name)
        assertTrue(result.canPreselect)
    }

    private fun recommendationMeta(freshness: String) = BittensorRecommendationsRemote.Meta(
        BittensorRecommendationsRemote.Gates("0.18", true, true),
        BittensorRecommendationsRemote.Components(BittensorRecommendationsRemote.Component("AVAILABLE", freshness, "2026-10-19T00:00:00Z"))
    )

    @Test
    fun `explicit selection retains its hotkey and can be passed to amount entry`() = runBlocking {
        val result = interactor.previews(subnet, chain.addressOf(hotkey)).single()
        assertEquals(hotkey, result.validator.hotkey)
        assertTrue(result.canPreselect)
        verifyNoInteractions(repository, yields)
    }

    @Test
    fun `automatic directory preview prefers a validator with a reported APY`() {
        val first = BittensorValidatorPreview(BittensorValidator(hotkey, "Unknown yield"), false)
        val secondKey = AccountIdKey(ByteArray(32) { 2 })
        val second = BittensorValidatorPreview(BittensorValidator(secondKey, "Known yield"), false)
        val result = selectBittensorValidatorPreview(listOf(first, second), mapOf(secondKey to java.math.BigDecimal("32.4")))
        assertEquals(second, result)
        assertFalse(requireNotNull(result).canPreselect)
    }

    @Test
    fun `automatic recommendation also prefers known APY without losing recommendation status`() {
        val first = BittensorValidatorPreview(BittensorValidator(hotkey, "Unknown yield"), true)
        val secondKey = AccountIdKey(ByteArray(32) { 2 })
        val second = BittensorValidatorPreview(BittensorValidator(secondKey, "Known yield"), true)
        assertEquals(second, selectBittensorValidatorPreview(listOf(first, second), mapOf(secondKey to java.math.BigDecimal.TEN)))
    }

    @Test
    fun `explicit selection or recommendation is never replaced to display another APY`() {
        val selected = BittensorValidatorPreview(BittensorValidator(hotkey, "Selected"), true)
        val otherKey = AccountIdKey(ByteArray(32) { 2 })
        val other = BittensorValidatorPreview(BittensorValidator(otherKey, "Other"), false)
        assertEquals(selected, selectBittensorValidatorPreview(listOf(selected, other), mapOf(otherKey to java.math.BigDecimal.TEN)))
        assertNull(selectBittensorValidatorPreview(emptyList(), emptyMap()))
    }

    @Test
    fun `empty or malformed directory does not fabricate a validator`() = runBlocking {
        `when`(backend.validators(subnet.value)).thenReturn(BittensorValidatorsRemote(emptyList()))
        assertTrue(interactor.previews(subnet, null).isEmpty())
        val invalid = BittensorValidatorsRemote.Item(subnet.value, "invalid", "Bad", null)
        `when`(backend.validators(subnet.value)).thenReturn(BittensorValidatorsRemote(listOf(invalid)))
        assertTrue(interactor.previews(subnet, null).isEmpty())
    }
}
