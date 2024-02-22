package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolPoints
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

@JvmInline
value class NominationPoolsCalls(val extrinsicBuilder: ExtrinsicBuilder)

val ExtrinsicBuilder.nominationPools: NominationPoolsCalls
    get() = NominationPoolsCalls(this)

fun NominationPoolsCalls.join(amount: Balance, poolId: PoolId) {
    extrinsicBuilder.call(
        moduleName = Modules.NOMINATION_POOLS,
        callName = "join",
        arguments = mapOf(
            "amount" to amount,
            "pool_id" to poolId.value
        )
    )
}

fun NominationPoolsCalls.bondExtra(source: NominationPoolBondExtraSource) {
    extrinsicBuilder.call(
        moduleName = Modules.NOMINATION_POOLS,
        callName = "bond_extra",
        arguments = mapOf(
            "extra" to source.prepareForEncoding()
        )
    )
}

fun NominationPoolsCalls.bondExtra(amount: Balance) {
    bondExtra(NominationPoolBondExtraSource.FreeBalance(amount))
}

fun NominationPoolsCalls.unbond(unbondAccount: AccountId, unbondPoints: PoolPoints) {
    extrinsicBuilder.call(
        moduleName = Modules.NOMINATION_POOLS,
        callName = "unbond",
        arguments = mapOf(
            "member_account" to AddressInstanceConstructor.constructInstance(extrinsicBuilder.runtime.typeRegistry, unbondAccount),
            "unbonding_points" to unbondPoints.value
        )
    )
}

fun NominationPoolsCalls.withdrawUnbonded(memberAccount: AccountId, numberOfSlashingSpans: BigInteger) {
    extrinsicBuilder.call(
        moduleName = Modules.NOMINATION_POOLS,
        callName = "withdraw_unbonded",
        arguments = mapOf(
            "member_account" to AddressInstanceConstructor.constructInstance(extrinsicBuilder.runtime.typeRegistry, memberAccount),
            "num_slashing_spans" to numberOfSlashingSpans
        )
    )
}

fun NominationPoolsCalls.claimPayout() {
    extrinsicBuilder.call(
        moduleName = Modules.NOMINATION_POOLS,
        callName = "claim_payout",
        arguments = emptyMap()
    )
}

private fun NominationPoolBondExtraSource.prepareForEncoding(): DictEnum.Entry<*> {
    return when (this) {
        is NominationPoolBondExtraSource.FreeBalance -> DictEnum.Entry("FreeBalance", amount)
        NominationPoolBondExtraSource.Rewards -> DictEnum.Entry("Rewards", null)
    }
}
