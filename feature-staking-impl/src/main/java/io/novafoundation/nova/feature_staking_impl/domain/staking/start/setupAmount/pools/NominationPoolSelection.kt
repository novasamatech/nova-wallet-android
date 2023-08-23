package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools

import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.join
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.apy
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

class NominationPoolSelection(
    val pool: NominationPool,
    override val stakingOption: StakingOption,
    override val stake: Balance,
) : StartMultiStakingSelection {

    override val apy: Perbill = pool.apy.orZero()

    override fun ExtrinsicBuilder.startStaking(metaAccount: MetaAccount) {
        nominationPools.join(stake, pool.id)
    }
}
