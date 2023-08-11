package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolPoints
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

@JvmInline
value class NominationPoolsCalls(val extrinsicBuilder: ExtrinsicBuilder)

val ExtrinsicBuilder.nominationPools: NominationPoolsCalls
    get() = NominationPoolsCalls(this)

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

private fun NominationPoolBondExtraSource.prepareForEncoding(): DictEnum.Entry<*> {
    return when (this) {
        is NominationPoolBondExtraSource.FreeBalance -> DictEnum.Entry("FreeBalance", amount)
        NominationPoolBondExtraSource.Rewards -> DictEnum.Entry("Rewards", null)
    }
}
