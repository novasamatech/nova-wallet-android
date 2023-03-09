package io.novafoundation.nova.feature_staking_impl.domain

import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.IndividualExposure
import org.junit.Assert.assertEquals
import org.junit.Test

class StakingInteractorExtKtTest {

    private val exposures = listOf(
        Exposure(
            total = 6.toBigInteger(),
            own = 0.toBigInteger(),
            others = listOf(
                IndividualExposure(byteArrayOf(3), 3.toBigInteger()),
                IndividualExposure(byteArrayOf(1), 1.toBigInteger()),
                IndividualExposure(byteArrayOf(2), 2.toBigInteger()),
            )
        ),
        Exposure(
            total = 3.toBigInteger(),
            own = 0.toBigInteger(),
            others = listOf(
                IndividualExposure(byteArrayOf(1), 1.toBigInteger()),
                IndividualExposure(byteArrayOf(2), 2.toBigInteger()),
            )
        )
    )

    @Test
    fun `account not from nominators should not be rewarded`() {
        runWillBeRewardedTest(expected = false, who = byteArrayOf(4), maxRewarded = 3)
    }

    @Test
    fun `account from first maxRewarded should be rewarded`() {
        runWillBeRewardedTest(expected = true, who = byteArrayOf(2), maxRewarded = 2)
    }

    @Test
    fun `account not from first maxRewarded should not be rewarded`() {
        runWillBeRewardedTest(expected = true, who = byteArrayOf(3), maxRewarded = 2)
    }

    @Test
    fun `should report NOT_PRESENT if stash is not in any validators nominations`() {
        runIsActiveTest(expected = NominationStatus.NOT_PRESENT, who = byteArrayOf(4), maxRewarded = 3)
    }

    @Test
    fun `should report ACTIVE if at least one stake portion is not in oversubscribed section of validator`() {
        // 1 is in oversubscribed section for first validator, but not for the second
        runIsActiveTest(expected = NominationStatus.ACTIVE,  who = byteArrayOf(1), maxRewarded = 2)
    }

    @Test
    fun `should report OVERSUBSCRIBED if all stake portions are in oversubscribed section of validator`() {
        runIsActiveTest(expected = NominationStatus.OVERSUBSCRIBED, who =  byteArrayOf(1), maxRewarded = 1)
    }

    @Test
    fun `should report ACTIVE if all stake portions are not in oversubscribed section of validator`() {
        runIsActiveTest(expected = NominationStatus.ACTIVE, who =  byteArrayOf(3), maxRewarded = 1)
    }

    private fun runIsActiveTest(expected: NominationStatus, who: ByteArray, maxRewarded: Int) {
        val actual = nominationStatus(who, exposures, maxRewarded)

        assertEquals(expected, actual)
    }

    private fun runWillBeRewardedTest(expected: Boolean, who: ByteArray, maxRewarded: Int) {
        val exposure = exposures.first()

        val actual = exposure.willAccountBeRewarded(who, maxRewarded)

        assertEquals(expected, actual)
    }
}
