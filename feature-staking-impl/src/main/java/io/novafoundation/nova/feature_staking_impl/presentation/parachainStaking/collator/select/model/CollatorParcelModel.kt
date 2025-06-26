package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorBond
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CandidateMetadata
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CapacityStatus
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorStatus
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapIdentityParcelModelToIdentity
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapIdentityToIdentityParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.CollatorParcelModel.CandidateMetadataParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.CollatorParcelModel.CollatorSnapshotParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.CollatorParcelModel.DelegatorBondParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.IdentityParcelModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger

@Parcelize
class CollatorParcelModel(
    val accountIdHex: String,
    val address: String,
    val identity: IdentityParcelModel?,
    val snapshot: CollatorSnapshotParcelModel?,
    val candidateMetadata: CandidateMetadataParcelModel,
    val minimumStakeToGetRewards: BigInteger,
    val apr: BigDecimal?,
) : Parcelable {

    @Parcelize
    class CollatorSnapshotParcelModel(
        val bond: BigInteger,
        val delegations: List<DelegatorBondParcelModel>,
        val total: BigInteger,
    ) : Parcelable

    @Parcelize
    class DelegatorBondParcelModel(
        val owner: ByteArray,
        val balance: BigInteger,
    ) : Parcelable

    @Parcelize
    class CandidateMetadataParcelModel(
        val totalCounted: BigInteger,
        val delegationCount: BigInteger,
        val lowestBottomDelegationAmount: BigInteger,
        val highestBottomDelegationAmount: BigInteger,
        val lowestTopDelegationAmount: BigInteger,
        val topCapacity: CapacityStatus,
        val bottomCapacity: CapacityStatus,
        val status: CollatorStatus,
    ) : Parcelable
}

fun mapCollatorToCollatorParcelModel(collator: Collator): CollatorParcelModel {
    return with(collator) {
        CollatorParcelModel(
            accountIdHex = accountIdHex,
            identity = identity?.let(::mapIdentityToIdentityParcelModel),
            snapshot = mapCollatorSnapshotToParcelModel(snapshot),
            candidateMetadata = mapCandidateMetadataToParcelModel(candidateMetadata),
            minimumStakeToGetRewards = minimumStakeToGetRewards,
            apr = apr,
            address = address
        )
    }
}

fun mapCollatorParcelModelToCollator(collator: CollatorParcelModel): Collator {
    return with(collator) {
        Collator(
            accountIdHex = accountIdHex,
            identity = identity?.let(::mapIdentityParcelModelToIdentity),
            snapshot = mapCollatorSnapshotFromParcelModel(snapshot),
            candidateMetadata = mapCandidateMetadataFromParcelModel(candidateMetadata),
            minimumStakeToGetRewards = minimumStakeToGetRewards,
            apr = apr,
            address = address
        )
    }
}

private fun mapCandidateMetadataToParcelModel(candidateMetadata: CandidateMetadata): CandidateMetadataParcelModel {
    return with(candidateMetadata) {
        CandidateMetadataParcelModel(
            totalCounted = totalCounted,
            delegationCount = delegationCount,
            lowestBottomDelegationAmount = lowestBottomDelegationAmount,
            highestBottomDelegationAmount = highestBottomDelegationAmount,
            lowestTopDelegationAmount = lowestTopDelegationAmount,
            topCapacity = topCapacity,
            bottomCapacity = bottomCapacity,
            status = status
        )
    }
}

private fun mapCandidateMetadataFromParcelModel(candidateMetadata: CandidateMetadataParcelModel): CandidateMetadata {
    return with(candidateMetadata) {
        CandidateMetadata(
            totalCounted = totalCounted,
            delegationCount = delegationCount,
            lowestBottomDelegationAmount = lowestBottomDelegationAmount,
            highestBottomDelegationAmount = highestBottomDelegationAmount,
            lowestTopDelegationAmount = lowestTopDelegationAmount,
            topCapacity = topCapacity,
            bottomCapacity = bottomCapacity,
            status = status
        )
    }
}

private fun mapCollatorSnapshotToParcelModel(snapshot: CollatorSnapshot?): CollatorSnapshotParcelModel? {
    return snapshot?.run {
        CollatorSnapshotParcelModel(
            bond = bond,
            delegations = delegations.map(::mapDelegatorBondToParcelModel),
            total = total
        )
    }
}

private fun mapDelegatorBondToParcelModel(delegatorBond: DelegatorBond): DelegatorBondParcelModel {
    return with(delegatorBond) {
        DelegatorBondParcelModel(owner, balance)
    }
}

private fun mapCollatorSnapshotFromParcelModel(snapshot: CollatorSnapshotParcelModel?): CollatorSnapshot? {
    return snapshot?.run {
        CollatorSnapshot(
            bond = bond,
            delegations = delegations.map(::mapDelegatorBondFromParcelModel),
            total = total
        )
    }
}

private fun mapDelegatorBondFromParcelModel(delegatorBond: DelegatorBondParcelModel): DelegatorBond {
    return with(delegatorBond) {
        DelegatorBond(owner, balance)
    }
}
