package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

@JvmInline
value class NominationPoolsCalls(val extrinsicBuilder: ExtrinsicBuilder)

val ExtrinsicBuilder.nominationPools: NominationPoolsCalls
    get() = NominationPoolsCalls(this)


fun NominationPoolsCalls.bondExtra(source: NominationPoolBondExtraSource) {
    extrinsicBuilder.call(
        Modules.NOMINATION_POOLS,
        "bond_extra",
        mapOf(
            "extra" to source.prepareForEncoding()
        )
    )
}

fun NominationPoolsCalls.bondExtra(amount: Balance) {
    bondExtra(NominationPoolBondExtraSource.FreeBalance(amount))
}

private fun NominationPoolBondExtraSource.prepareForEncoding(): DictEnum.Entry<*> {
    return when (this) {
        is NominationPoolBondExtraSource.FreeBalance -> DictEnum.Entry("FreeBalance", amount)
        NominationPoolBondExtraSource.Rewards -> DictEnum.Entry("Rewards", null)
    }
}
