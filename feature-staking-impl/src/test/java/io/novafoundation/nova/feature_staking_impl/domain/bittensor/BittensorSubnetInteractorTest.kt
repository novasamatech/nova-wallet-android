package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetFavoritesRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorSubnetInteractorTest {

    private val repository = mock(BittensorSubnetRepository::class.java)
    private val portfolioRepository = mock(BittensorPortfolioRepository::class.java)
    private val bittensorSubnetQuoteInteractor = mock(BittensorSubnetQuoteInteractor::class.java)
    private val favorites = mock(BittensorSubnetFavoritesRepository::class.java)
    private val bittensorSubnetInteractor = BittensorSubnetInteractor(repository, portfolioRepository, bittensorSubnetQuoteInteractor, favorites)

    @Test
    fun `subnet lookup uses the id rather than catalogue order`() = runBlocking {
        val first = BittensorSubnet(BittensorSubnetId(2), "Second", "B", null)
        val requested = BittensorSubnet(BittensorSubnetId(1), "First", "A", null)
        `when`(repository.subnets()).thenReturn(listOf(first, requested))

        assertEquals(requested, bittensorSubnetInteractor.subnet(BittensorSubnetId(1)))
    }

    @Test
    fun `missing subnet does not select a different subnet`() = runBlocking {
        val other = BittensorSubnet(BittensorSubnetId(2), "Second", "B", null)
        `when`(repository.subnets()).thenReturn(listOf(other))

        val failure = runCatching { bittensorSubnetInteractor.subnet(BittensorSubnetId(1)) }.exceptionOrNull()

        assertTrue(failure is NoSuchElementException)
    }
}
