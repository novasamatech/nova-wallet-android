package io.novafoundation.nova.feature_governance_api.data.thresold.gov1

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.common.utils.intSqrt
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingThreshold.Threshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ayeVotes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.notPassing
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.simple
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.passing
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

enum class Gov1VotingThreshold(val readableName: String) : VotingThreshold {

    SUPER_MAJORITY_APPROVE("SimpleMajorityApprove") {

        // https://github.com/paritytech/substrate/blob/5ae005c244295d23586c93da43148b2bc826b137/frame/democracy/src/vote_threshold.rs#L101
        // nays / sqrt(turnout) < ayes / sqrt(total_issuance) =>
        // ayes > nays * sqrt(total_issuance) / sqrt(turnout)  =>
        // ayes / (ayes + nays) > [nays / (ayes + nays)] * [sqrt(total_issuance) / sqrt(turnout)]
        // let a = ayes / (ayes + nays), to = sqrt(total_issuance), tu = sqrt(turnout)
        // a > (1 - a) * to / tu
        // a > to / tu - a * to / tu => a > (to / tu) / (1 + to / tu)
        // a > to / (tu + to)
        override fun ayesFractionThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Perbill> {
            if (totalIssuance == Balance.ZERO || tally.support == Balance.ZERO) return Threshold.notPassing(Perbill.ONE)

            val sqrtTurnout = tally.support.intSqrt()
            val sqrtTotalIssuance = totalIssuance.intSqrt()

            val aysFraction = tally.ayeVotes().fraction

            val threshold = sqrtTotalIssuance.divideToDecimal(sqrtTurnout + sqrtTotalIssuance)

            return Threshold.simple(
                value = threshold,
                currentlyPassing = aysFraction > threshold
            )
        }
    },

    SUPER_MAJORITY_AGAINST("SimpleMajority") {

        // https://github.com/paritytech/substrate/blob/5ae005c244295d23586c93da43148b2bc826b137/frame/democracy/src/vote_threshold.rs#L103
        // nays / sqrt(total_issuance) < ayes / sqrt(turnout) =>
        // ayes > nays * sqrt(turnout) / sqrt(total_issuance)  =>
        // ayes / (ayes + nays) > [nays / (ayes + nays)] * [sqrt(turnout) / sqrt(total_issuance)]
        // let a = ayes / (ayes + nays), to = sqrt(total_issuance), tu = sqrt(turnout)
        // a > tu / (tu + to)
        override fun ayesFractionThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Perbill> {
            if (totalIssuance == Balance.ZERO || tally.support == Balance.ZERO) return Threshold.notPassing(Perbill.ONE)

            val sqrtTurnout = tally.support.intSqrt()
            val sqrtTotalIssuance = totalIssuance.intSqrt()

            val aysFraction = tally.ayeVotes().fraction

            val threshold = sqrtTurnout.divideToDecimal(sqrtTurnout + sqrtTotalIssuance)

            return Threshold.simple(
                value = threshold,
                currentlyPassing = aysFraction > threshold
            )
        }
    },

    SIMPLE_MAJORITY("SimpleMajority") {

        // https://github.com/paritytech/substrate/blob/5ae005c244295d23586c93da43148b2bc826b137/frame/democracy/src/vote_threshold.rs#L105
        // ayes > nays =>
        // ayes / (ayes + nays) > 0.5
        override fun ayesFractionThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Perbill> {
            val threshold = 0.5.toBigDecimal()

            val aysFraction = tally.ayeVotes().fraction

            return Threshold.simple(
                value = threshold,
                currentlyPassing = aysFraction > threshold
            )
        }
    };

    override fun supportThreshold(tally: Tally, totalIssuance: Balance, passedSinceDecidingFraction: Perbill): Threshold<Balance> {
        return Threshold.passing(BigInteger.ZERO)
    }
}

fun VotingThreshold.asGovV1VotingThresholdOrNull(): Gov1VotingThreshold? {
    return this as? Gov1VotingThreshold
}
