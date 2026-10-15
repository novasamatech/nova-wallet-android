package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindCollectionEnum
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

class CandidateMetadata(
    val totalCounted: Balance,
    val delegationCount: BigInteger,
    val lowestBottomDelegationAmount: Balance,
    val highestBottomDelegationAmount: Balance,
    val lowestTopDelegationAmount: Balance,
    val topCapacity: CapacityStatus,
    val bottomCapacity: CapacityStatus,
    val status: CollatorStatus,
)

enum class CapacityStatus {
    Full, Empty, Partial
}

enum class CollatorStatus {
    Active, Idle, Leaving
}

fun CandidateMetadata.isFull(): Boolean {
    return bottomCapacity == CapacityStatus.Full
}
fun CandidateMetadata.isRewardedListFull(): Boolean {
    return topCapacity == CapacityStatus.Full
}

val CandidateMetadata.isActive
    get() = status == CollatorStatus.Active

fun CandidateMetadata.isBottomDelegationsNotEmpty(): Boolean {
    return bottomCapacity != CapacityStatus.Empty
}

fun CandidateMetadata.isStakeEnoughToEarnRewards(stake: BigInteger): Boolean {
    return if (isRewardedListFull()) {
        stake > lowestTopDelegationAmount
    } else {
        true
    }
}

fun CandidateMetadata.minimumStakeToGetRewards(techMinimumStake: Balance): Balance {
    return if (topCapacity == CapacityStatus.Full) {
        lowestTopDelegationAmount
    } else {
        techMinimumStake
    }
}

fun bindCandidateMetadata(decoded: Any?): CandidateMetadata {
    return decoded.castToStruct().let { struct ->
        val nominationCount: Any? = struct.mapping["delegationCount"] ?: struct.mapping["nominationCount"]
        val lowestBottom: Any? = struct.mapping["lowestBottomDelegationAmount"] ?: struct.mapping["lowestBottomNominationAmount"]
        val lowestTop: Any? = struct.mapping["lowestTopDelegationAmount"] ?: struct.mapping["lowestTopNominationAmount"]
        val highestBottom: Any? = struct.mapping["highestBottomDelegationAmount"] ?: struct.mapping["highestBottomNominationAmount"]

        CandidateMetadata(
            totalCounted = bindNumber(struct["totalCounted"]),
            delegationCount = bindNumber(nominationCount),
            lowestBottomDelegationAmount = bindNumber(lowestBottom),
            lowestTopDelegationAmount = bindNumber(lowestTop),
            highestBottomDelegationAmount = bindNumber(highestBottom),
            topCapacity = bindCollectionEnum(struct["topCapacity"]),
            bottomCapacity = bindCollectionEnum(struct["bottomCapacity"]),
            status = bindCollectionEnum(struct["status"])
        )
    }
}
