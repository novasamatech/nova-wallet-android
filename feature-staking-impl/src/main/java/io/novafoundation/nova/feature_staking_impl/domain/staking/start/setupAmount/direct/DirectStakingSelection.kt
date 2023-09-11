package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct

import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.bond
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.nominate
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.multiAddressOf
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

data class DirectStakingSelection(
    val validators: List<Validator>,
    val validatorsLimit: Int,
    override val stakingOption: StakingOption,
    override val stake: Balance,
) : StartMultiStakingSelection {

    override val apy = validators.mapNotNull { it.electedInfo?.apy?.asPerbill() }
        .maxOrNull()

    override fun ExtrinsicBuilder.startStaking(metaAccount: MetaAccount) {
        val chain = stakingOption.chain

        val targets = validators.map { chain.multiAddressOf(it.accountIdHex.fromHex()) }
        val controllerAddress = chain.multiAddressOf(metaAccount.requireAccountIdIn(chain))

        bond(controllerAddress, stake, RewardDestination.Restake)
        nominate(targets)
    }

    override fun copyWith(stake: Balance): StartMultiStakingSelection {
        return copy(stake = stake)
    }

    override fun isSettingsEquals(other: StartMultiStakingSelection): Boolean {
        if (other === this) return true
        if (other !is DirectStakingSelection) return false

        val otherAddresses = other.validators.map { it.address }.toSet()
        val thisAddresses = validators.map { it.address }.toSet()
        return thisAddresses == otherAddresses &&
            validatorsLimit == other.validatorsLimit
    }
}

fun StartMultiStakingSelection.asDirectSelection(): DirectStakingSelection? {
    return this as? DirectStakingSelection
}
