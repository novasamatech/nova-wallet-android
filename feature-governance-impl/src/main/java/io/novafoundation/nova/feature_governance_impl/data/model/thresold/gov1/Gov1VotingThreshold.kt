package io.novafoundation.nova.feature_governance_impl.data.model.thresold.gov1

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.common.utils.intSqrt
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

enum class Gov1VotingThreshold : VotingThreshold {

    SUPER_MAJORITY_APPROVE {

        // https://github.com/paritytech/substrate/blob/5ae005c244295d23586c93da43148b2bc826b137/frame/democracy/src/vote_threshold.rs#L101
        // nays / sqrt(turnout) < ayes / sqrt(total_issuance) =>
        // ayes > nays * sqrt(total_issuance) / sqrt(turnout)  =>
        // ayes / (ayes + nays) > [nays / (ayes + nays)] * [sqrt(total_issuance) / sqrt(turnout)]
        override fun ayesFractionNeeded(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Perbill {
            val sqrtTurnout = tally.support.intSqrt()
            val sqrtTotalIssuance = totalIssuance.intSqrt()

            val naysFraction = tally.nays.divideToDecimal(tally.ayes + tally.nays)
            val totalIssuanceToTurnout = sqrtTotalIssuance.divideToDecimal(sqrtTurnout)

            return naysFraction * totalIssuanceToTurnout
        }
    },

    SUPER_MAJORITY_AGAINST {

        // https://github.com/paritytech/substrate/blob/5ae005c244295d23586c93da43148b2bc826b137/frame/democracy/src/vote_threshold.rs#L103
        // nays / sqrt(total_issuance) < ayes / sqrt(turnout) =>
        // ayes > nays * sqrt(turnout) / sqrt(total_issuance)  =>
        // ayes / (ayes + nays) > [nays / (ayes + nays)] * [sqrt(turnout) / sqrt(total_issuance)]
        override fun ayesFractionNeeded(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Perbill {
            val sqrtTurnout = tally.support.intSqrt()
            val sqrtTotalIssuance = totalIssuance.intSqrt()

            val naysFraction = tally.nays.divideToDecimal(tally.ayes + tally.nays)
            val turnoutToTotalIssuance = sqrtTurnout.divideToDecimal(sqrtTotalIssuance)

            return naysFraction * turnoutToTotalIssuance
        }
    },

    SIMPLE_MAJORITY {

        // https://github.com/paritytech/substrate/blob/5ae005c244295d23586c93da43148b2bc826b137/frame/democracy/src/vote_threshold.rs#L105
        // ayes > nays =>
        // ayes / (ayes + nays) > 0.5
        override fun ayesFractionNeeded(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Perbill {
            return 0.5.toBigDecimal()
        }
    };

    override fun supportNeeded(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Balance {
        return BigInteger.ZERO
    }
}
