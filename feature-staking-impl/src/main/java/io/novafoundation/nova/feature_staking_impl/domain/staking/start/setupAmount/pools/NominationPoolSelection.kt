package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.join
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.apy
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.model.MultiStakingType
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

data class NominationPoolSelection(
    val pool: NominationPool,
    override val stakingOption: StakingOption,
    override val stake: Balance,
) : StartMultiStakingSelection {

    override val apy = pool.apy

    override fun ExtrinsicBuilder.startStaking(metaAccount: MetaAccount) {
        nominationPools.join(stake, pool.id)
    }

    override fun isSettingsEquals(other: StartMultiStakingSelection): Boolean {
        if (this === other) return true
        if (other !is NominationPoolSelection) return false

        return pool.id.value == other.pool.id.value
    }

    override fun copyWith(stake: Balance): StartMultiStakingSelection {
        return copy(stake = stake)
    }
}
