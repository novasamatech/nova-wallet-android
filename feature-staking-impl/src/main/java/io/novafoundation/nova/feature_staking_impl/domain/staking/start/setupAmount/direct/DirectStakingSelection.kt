package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct

import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.bond
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.nominate
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.multiAddressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

class DirectStakingSelection(
    val validators: List<Validator>,
    val validatorsLimit: Int,
    override val stakingOption: StakingOption,
): StartMultiStakingSelection {

    override val apy = validators.maxOf { it.electedInfo?.apy.orZero().asPerbill() }

    override fun ExtrinsicBuilder.startStaking(
        amount: Balance,
        chain: Chain,
        metaAccount: MetaAccount
    ) {
        val targets = validators.map { chain.multiAddressOf(it.accountIdHex.fromHex()) }
        val controllerAddress = chain.multiAddressOf(metaAccount.requireAccountIdIn(chain))

        bond(controllerAddress, amount, RewardDestination.Restake)
        nominate(targets)
    }
}
