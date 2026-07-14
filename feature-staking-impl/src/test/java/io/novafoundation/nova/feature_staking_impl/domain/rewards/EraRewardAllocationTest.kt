package io.novafoundation.nova.feature_staking_impl.domain.rewards

import io.novafoundation.nova.feature_staking_api.domain.model.EraRewardAllocation
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration.Companion.hours

class EraRewardAllocationTest {

    @Test
    fun `view function id matches the id served in on-chain metadata`() {
        // Byte-for-byte the id served in Polkadot/Kusama Asset Hub metadata v16
        // (verified against both live chains on 2026-07-14).
        assertEquals(
            "0x5f3e4907f716ac89b6347d15ececedca56eb9b14a6bcf65e579d5d00b3093b6c",
            EraRewardAllocation.ERA_REWARD_ALLOCATION_VIEW_FUNCTION_ID.toHexString(withPrefix = true)
        )
    }

    @Test
    fun `decodes live mainnet response`() {
        // Captured verbatim from Polkadot Asset Hub for era 2229 (2026-07-14):
        // staker_rewards = 69,215.6368394402 DOT, validator_incentive = 34,607.8184197201 DOT.
        val response = "0x0080a298773683750200000000000000000051cc3b9bc13a01000000000000000000".fromHex()

        val allocation = EraRewardAllocation.fromStateCallResult(response)

        assertEquals(BigInteger("692156368394402"), allocation.stakerRewards)
        assertEquals(BigInteger("346078184197201"), allocation.validatorIncentive)
    }

    @Test
    fun `decodes zero allocation`() {
        // An era recorded outside DAP mode (or not yet completed) reads as all-zero.
        val response = byteArrayOf(0x00, 0x80.toByte()) + ByteArray(32)

        val allocation = EraRewardAllocation.fromStateCallResult(response)

        assertEquals(BigInteger.ZERO, allocation.stakerRewards)
        assertEquals(BigInteger.ZERO, allocation.validatorIncentive)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws on dispatch error envelope`() {
        // Err(ViewFunctionDispatchError) — e.g. NotImplemented on a runtime without
        // view function support.
        EraRewardAllocation.fromStateCallResult(byteArrayOf(0x01, 0x00))
    }

    @Test
    fun `throws on truncated payload`() {
        // Ok envelope whose payload is shorter than the two expected u128 values.
        val response = byteArrayOf(0x00, 0x20) + ByteArray(8)

        try {
            EraRewardAllocation.fromStateCallResult(response)
            fail("Expected a decoding exception")
        } catch (_: Exception) {
            // expected
        }
    }

    // Realistic post-DAP snapshot (verified on Polkadot Asset Hub, era ~2230):
    //   staker_rewards (era_reward_allocation) = 69,216 DOT/era  (the DAP staker allocation,
    //                                            45.2% of the ~153,132 DOT daily mint)
    //   total staked                           = 862,000,000 DOT
    //   era duration                           = 24h  => 365 eras/year
    // True return = 69,216 * 365 / 862,000,000 = 2.93%.
    //
    // Regression guard: the previous calculator used the Inflation runtime api's full period
    // mint (~153,132 DOT) as if it all went to stakers, producing ~6.5% average / ~8.9% max -
    // a 2.21x (= 1 / 0.452) overstatement.
    @Test
    fun `calculates the post-DAP staking return`() {
        val calculator = EraRewardAllocationRewardCalculator(
            stakersEraReward = dot(69_216),
            eraDuration = 24.hours,
            totalIssuance = dot(1_693_000_000),
            validators = (0 until 4).map { index ->
                RewardCalculationTarget(
                    accountIdHex = "0$index",
                    totalStake = dot(215_500_000), // 4 equal validators => 862,000,000 DOT staked
                    commission = BigDecimal.ZERO
                )
            }
        )

        assertEquals(0.0293, calculator.maxAPY, 5e-4)
        assertEquals(0.0293, calculator.expectedAPY.toDouble(), 5e-4)
    }

    @Test
    fun `zero staker reward yields zero return`() {
        val calculator = EraRewardAllocationRewardCalculator(
            stakersEraReward = BigInteger.ZERO,
            eraDuration = 24.hours,
            totalIssuance = dot(1_693_000_000),
            validators = listOf(
                RewardCalculationTarget(accountIdHex = "00", totalStake = dot(862_000_000), commission = BigDecimal.ZERO)
            )
        )

        assertEquals(0.0, calculator.maxAPY, 1e-9)
    }

    private fun dot(amount: Long): BigInteger = BigInteger.valueOf(amount) * BigInteger.TEN.pow(10)
}
