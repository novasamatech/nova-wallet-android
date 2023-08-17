package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

class NominationPoolSelection : StartMultiStakingSelection {

    override val stakingType = Chain.Asset.StakingType.NOMINATION_POOLS

    override val apy: BigDecimal = BigDecimal.ZERO

    override fun ExtrinsicBuilder.startStaking(amount: Balance, chain: Chain, metaAccount: MetaAccount) {
       // TODO
    }
}
