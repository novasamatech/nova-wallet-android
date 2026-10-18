package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock

class BittensorStakeConfirmPayloadTest {

    private val chain = mock(Chain::class.java)

    private val validatorAccountId: AccountId = ByteArray(32) { 1 }

    @Test
    fun `confirmation reconstructs every operation input from its own payload`() {
        val payload = BittensorStakeConfirmPayload(42L, BigInteger.TEN, chain.addressOf(validatorAccountId), "Selected validator")

        val request = payload.toStakeRequest(chain)

        assertEquals(42L, request.metaId)
        assertEquals(BigInteger.TEN, request.amountInPlanks)
        assertArrayEquals(validatorAccountId, request.validator.hotkey.value)
        assertEquals("Selected validator", request.validator.name)
        assertSame(chain, request.chain)
    }

    @Test
    fun `creating another confirmation does not replace the first operation`() {
        val firstPayload = BittensorStakeConfirmPayload(1L, BigInteger.ONE, chain.addressOf(validatorAccountId), "First validator")
        val otherValidatorAccountId: AccountId = ByteArray(32) { 2 }
        val secondPayload = BittensorStakeConfirmPayload(2L, BigInteger.TEN, chain.addressOf(otherValidatorAccountId), "Second validator")

        val secondRequest = secondPayload.toStakeRequest(chain)
        val firstRequest = firstPayload.toStakeRequest(chain)

        assertEquals(1L, firstRequest.metaId)
        assertEquals(BigInteger.ONE, firstRequest.amountInPlanks)
        assertArrayEquals(validatorAccountId, firstRequest.validator.hotkey.value)
        assertEquals(2L, secondRequest.metaId)
        assertEquals(BigInteger.TEN, secondRequest.amountInPlanks)
        assertArrayEquals(otherValidatorAccountId, secondRequest.validator.hotkey.value)
    }
}
