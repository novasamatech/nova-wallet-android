package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.bindBittensorDelegatePermits
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorTimeoutException
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDelegateSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRecommendationsRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetCatalogueRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetCatalogueRemoteSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mapBittensorListedSubnets
import io.novafoundation.nova.runtime.ext.ChainGeneses
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorValidatorCoverageTest {

    @Test
    fun `coverage counts only permits on listed subnets`() {
        val permits = setOf(BittensorNetuid.Subnet(1), BittensorNetuid.Subnet(5), BittensorNetuid.Subnet(99))
        val listed = (1..10).map { BittensorNetuid.Subnet(it) }.toSet()

        assertEquals(BittensorValidatorCoverage(permitted = 2, total = 10), bittensorValidatorCoverage(permits, listed))
    }

    @Test
    fun `every listed subnet counts towards the total once and root never does`() {
        val rows = listOf(0, 1, 7, 7, 64).map { BittensorSubnetCatalogueRemote.Subnet(it, taoPerAlpha = null) }
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", "STALE")
        val response = BittensorSubnetCatalogueRemote(rows, BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(component, component)))

        assertEquals(setOf(BittensorNetuid.Subnet(1), BittensorNetuid.Subnet(7), BittensorNetuid.Subnet(64)), mapBittensorListedSubnets(response))
    }

    @Test
    fun `a hotkey that is not a delegate has unknown coverage rather than zero`() {
        assertNull(bittensorValidatorCoverage(null, setOf(BittensorNetuid.Subnet(1))))
        assertNull(bindBittensorDelegatePermits(null))
    }

    @Test
    fun `delegate permits drop the root network`() {
        val delegate = Struct.Instance(mapOf("validatorPermits" to listOf(BigInteger.ZERO, BigInteger.ONE, 64.toBigInteger())))

        assertEquals(setOf(BittensorNetuid.Subnet(1), BittensorNetuid.Subnet(64)), bindBittensorDelegatePermits(delegate))
    }

    @Test
    fun `a hanging chain read times out as a recoverable failure`() = runTest {
        val delegates = object : BittensorDelegateSource {
            override suspend fun validatorPermits(chainId: String, hotkey: AccountIdKey): Set<BittensorNetuid.Subnet>? = awaitCancellation()
        }
        val catalogue = mock(BittensorSubnetCatalogueRemoteSource::class.java)
        val hotkey = AccountIdKey(ByteArray(32))
        `when`(catalogue.catalogue()).thenReturn(emptyCatalogue())
        val interactor = BittensorValidatorCoverageInteractor(delegates, catalogue, timeoutMillis = 100)

        val failure = runCatching { interactor.coverage(hotkey) }.exceptionOrNull()

        assertTrue(failure is BittensorTimeoutException)
        assertEquals("Validator coverage timed out", failure?.message)
    }

    @Test
    fun `non-delegate hotkey resolves to unknown coverage without timing out`() = runTest {
        val delegates = mock(BittensorDelegateSource::class.java)
        val catalogue = mock(BittensorSubnetCatalogueRemoteSource::class.java)
        val hotkey = AccountIdKey(ByteArray(32))
        `when`(delegates.validatorPermits(ChainGeneses.BITTENSOR, hotkey)).thenReturn(null)
        `when`(catalogue.catalogue()).thenReturn(emptyCatalogue())

        assertNull(BittensorValidatorCoverageInteractor(delegates, catalogue).coverage(hotkey))
    }

    private fun emptyCatalogue(): BittensorSubnetCatalogueRemote {
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")
        return BittensorSubnetCatalogueRemote(emptyList(), BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(component, component)))
    }
}
