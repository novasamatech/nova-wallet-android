package io.novafoundation.nova.feature_staking_impl.domain

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.IndividualExposure
import io.novafoundation.nova.feature_staking_impl.domain.bagList.BagListLocator
import io.novafoundation.nova.feature_staking_impl.domain.bagList.BagListScoreConverter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

fun isNominationActive(
    stashId: AccountId,
    exposures: Collection<Exposure>,
    rewardedNominatorsPerValidator: Int
): Boolean {
    val comparator = { accountId: IndividualExposure ->
        accountId.who.contentEquals(stashId)
    }

    val validatorsWithOurStake = exposures.filter { exposure ->
        exposure.others.any(comparator)
    }

    return validatorsWithOurStake.any { it.willAccountBeRewarded(stashId, rewardedNominatorsPerValidator) }
}

fun Exposure.willAccountBeRewarded(
    accountId: AccountId,
    rewardedNominatorsPerValidator: Int
): Boolean {
    val indexInRewardedList = others.sortedByDescending(IndividualExposure::value).indexOfFirst {
        it.who.contentEquals(accountId)
    }

    if (indexInRewardedList == -1) {
        return false
    }

    val numberInRewardedList = indexInRewardedList + 1

    return numberInRewardedList <= rewardedNominatorsPerValidator
}

fun minimumStake(
    exposures: Collection<Exposure>,
    minimumNominatorBond: BigInteger,
    bagListLocator: BagListLocator?,
    bagListScoreConverter: BagListScoreConverter,
): BigInteger {
    val stakeByNominator = exposures
        .fold(mutableMapOf<AccountIdKey, BigInteger>()) { acc, exposure ->
            exposure.others.forEach { individualExposure ->
                val key = individualExposure.who.intoKey()
                val currentExposure = acc.getOrDefault(key, BigInteger.ZERO)
                acc[key] = currentExposure + individualExposure.value
            }

            acc
        }

    val minElectedStake = stakeByNominator.values.minOrNull().orZero().coerceAtLeast(minimumNominatorBond)

    if (bagListLocator == null) return minElectedStake

    val lastElectedBag = bagListLocator.bagBoundaries(bagListScoreConverter.scoreOf(minElectedStake))

    val nextBagThreshold = bagListScoreConverter.balanceOf(lastElectedBag.endInclusive)
    val epsilon = Balance.ONE

    return nextBagThreshold + epsilon
}
