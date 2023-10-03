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

enum class NominationStatus {
    NOT_PRESENT, OVERSUBSCRIBED, ACTIVE
}

val NominationStatus.isActive: Boolean
    get() = this == NominationStatus.ACTIVE

val NominationStatus.isOversubscribed: Boolean
    get() = this == NominationStatus.OVERSUBSCRIBED

fun nominationStatus(
    stashId: AccountId,
    exposures: Collection<Exposure>,
    rewardedNominatorsPerValidator: Int
): NominationStatus {
    val comparator = { accountId: IndividualExposure ->
        accountId.who.contentEquals(stashId)
    }

    val validatorsWithOurStake = exposures.filter { exposure ->
        exposure.others.any(comparator)
    }
    if (validatorsWithOurStake.isEmpty()) {
        return NominationStatus.NOT_PRESENT
    }

    val willBeRewarded = validatorsWithOurStake.any { it.willAccountBeRewarded(stashId, rewardedNominatorsPerValidator) }

    return if (willBeRewarded) NominationStatus.ACTIVE else NominationStatus.OVERSUBSCRIBED
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
    maxNominatorsInValidator: Int,
    bagListScoreConverter: BagListScoreConverter,
    bagListSize: BigInteger?,
    maxElectingVoters: BigInteger?
): BigInteger {
    if (bagListSize == null || maxElectingVoters == null || bagListSize < maxElectingVoters) return minimumNominatorBond

    val stakeByNominator = exposures
        .fold(mutableMapOf<AccountIdKey, ActiveStakingEntry>()) { acc, exposure ->
            exposure.others.forEachIndexed { index, individualExposure ->
                val key = individualExposure.who.intoKey()
                val currentEntry = acc.getOrPut(key) { ActiveStakingEntry() }

                val isStakeActive = index < maxNominatorsInValidator
                currentEntry.addStake(individualExposure.value, isStakeActive)
            }

            acc
        }

    val minElectedStake = stakeByNominator.values
        .asSequence()
        .mapNotNull { it.totalStakedIfAnyActive() }
        .minOrNull()
        .orZero()
        .coerceAtLeast(minimumNominatorBond)

    if (bagListLocator == null) return minElectedStake

    val lastElectedBag = bagListLocator.bagBoundaries(bagListScoreConverter.scoreOf(minElectedStake))

    val nextBagThreshold = bagListScoreConverter.balanceOf(lastElectedBag.endInclusive ?: lastElectedBag.start)
    val epsilon = Balance.ONE

    val nextBagRequiredAmount = nextBagThreshold + epsilon

    return nextBagRequiredAmount.coerceAtLeast(minElectedStake)
}


private class ActiveStakingEntry {

    private var totalStaked: Balance = Balance.ZERO
    private var allStakeInactive: Boolean = true

    fun addStake(stake: Balance, isStakeActive: Boolean) {
        totalStaked += stake
        allStakeInactive = allStakeInactive && !isStakeActive
    }

    fun totalStakedIfAnyActive(): Balance? {
        return totalStaked.takeUnless { allStakeInactive }
    }
}
