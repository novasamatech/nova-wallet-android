package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorRootStakePayloadTest {

    private val chain = mock(Chain::class.java)

    private val asset = mock(Asset::class.java)

    @Test
    fun `rewards only root keeps explicit wallet without inventing a validator`() {
        val positions = BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap())
        val portfolio = BittensorPortfolio(7L, chain, asset, positions, BittensorDataStatus.Fresh)
        val payload = rootStakeAmountPayload(portfolio)
        assertEquals(7L, payload.metaId)
        assertNull(payload.validatorAddress)
    }

    @Test
    fun `existing root fixes wallet and deterministic primary validator`() {
        `when`(chain.addressPrefix).thenReturn(42)
        val small = BittensorRootPosition(AccountIdKey(ByteArray(32) { 1 }), BigInteger.ONE)
        val large = BittensorRootPosition(AccountIdKey(ByteArray(32) { 2 }), BigInteger.TEN)
        val positions = BittensorPortfolioPositions(listOf(small, large), emptyList(), emptyMap())
        val portfolio = BittensorPortfolio(7L, chain, asset, positions, BittensorDataStatus.Fresh)
        val payload = rootStakeAmountPayload(portfolio)
        assertEquals(7L, payload.metaId)
        assertEquals(chain.addressOf(large.validatorHotkey), payload.validatorAddress)
    }
}
