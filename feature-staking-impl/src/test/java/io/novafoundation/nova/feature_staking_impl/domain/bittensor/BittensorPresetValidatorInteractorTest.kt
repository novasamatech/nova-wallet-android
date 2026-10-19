package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    private val interactor = BittensorPresetValidatorInteractor(repository, yields, chains, logger)

    @Before
    fun prepare() = runBlocking<Unit> {
        `when`(chains.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        `when`(repository.validators(chain, subnet)).thenReturn(listOf(first, second, third))
        `when`(yields.alphaYields(chain, subnet)).thenReturn(mapOf(first.validator.hotkey to BigDecimal.ONE, second.validator.hotkey to BigDecimal.TEN))
    }

    @Test
    fun `known highest yield recommendation is checked and selected`() = runBlocking<Unit> {
        assertEquals(second, interactor.validators(subnet).first())
        verify(repository).validate(chain, subnet, second.validator)
        verify(repository, never()).validate(chain, subnet, first.validator)
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

    private fun candidate(id: Int): BittensorSubnetValidator {
        val hotkey = AccountIdKey(ByteArray(32) { id.toByte() })
        val validator = BittensorValidator(hotkey, "Validator $id")
        return BittensorSubnetValidator(validator, "address-$id", BigDecimal.ZERO, selectable = true, recommended = true)
    }
}
