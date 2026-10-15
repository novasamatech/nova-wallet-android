package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class BittensorFeeEstimateTest {

    private val chain = mock(Chain::class.java)

    private val fee = mock(Fee::class.java)

    private val validator = BittensorValidator(AccountIdKey(ByteArray(32) { 1 }), "Validator")

    private val request = BittensorRootStakeRequest(1L, chain, validator, BigInteger.TEN)

    private val estimate = BittensorFeeEstimate(request, fee)

    private val display = FeeDisplay("fee", null)

    private val loadedStatus = FeeStatus.Loaded(FeeModel(estimate, display))

    @Test
    fun `matching request retains the loaded fee and its display`() {
        assertSame(loadedStatus, loadedStatus.forRequest(request.copy()))
        assertSame(fee, loadedStatus.forRequest(request).loadedFeeOrNull()?.fee)
    }

    @Test
    fun `amount change immediately invalidates a loaded estimate`() {
        val changedRequest = request.copy(amountInPlanks = BigInteger.ONE)

        assertTrue(loadedStatus.forRequest(changedRequest) is FeeStatus.Loading)
        assertNull(loadedStatus.forRequest(changedRequest).loadedFeeOrNull())
    }

    @Test
    fun `validator change immediately invalidates a loaded estimate`() {
        val changedValidator = BittensorValidator(AccountIdKey(ByteArray(32) { 2 }), "Other validator")
        val changedRequest = request.copy(validator = changedValidator)

        assertTrue(loadedStatus.forRequest(changedRequest) is FeeStatus.Loading)
    }

    @Test
    fun `wallet change immediately invalidates a loaded estimate`() {
        assertTrue(loadedStatus.forRequest(request.copy(metaId = 2L)) is FeeStatus.Loading)
    }

    @Test
    fun `missing request clears a loaded fee`() {
        assertSame(FeeStatus.NoFee, loadedStatus.forRequest(null))
    }

    @Test
    fun `loading error and no fee states remain controlled by the mixin`() {
        val statuses = listOf<FeeStatus<BittensorFeeEstimate<BittensorRootStakeRequest>, FeeDisplay>>(
            FeeStatus.Loading(true),
            FeeStatus.Error,
            FeeStatus.NoFee
        )

        statuses.forEach { status ->
            assertSame(status, status.forRequest(request))
            assertNull(status.forRequest(request).loadedFeeOrNull())
            assertSame(FeeStatus.NoFee, status.forRequest(null))
        }
    }
}
