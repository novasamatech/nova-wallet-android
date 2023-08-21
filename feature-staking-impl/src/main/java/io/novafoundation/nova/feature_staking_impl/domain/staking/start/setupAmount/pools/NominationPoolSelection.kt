package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools

import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.apy
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

class NominationPoolSelection(
    val pool: NominationPool,
    override val stakingOption: StakingOption,
) : StartMultiStakingSelection {

    override val apy: Perbill = pool.apy.orZero()

    override fun ExtrinsicBuilder.startStaking(amount: Balance, chain: Chain, metaAccount: MetaAccount) {
        // TODO
    }
}
