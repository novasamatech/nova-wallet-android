package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection

import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

interface StartMultiStakingSelection {

    val stakingOption: StakingOption

    val apy: Perbill

    fun ExtrinsicBuilder.startStaking(amount: Balance, chain: Chain, metaAccount: MetaAccount)
}

sealed class SelectionTypeSource {

    object Automatic : SelectionTypeSource()

    data class Manual(val contentRecommended: Boolean) : SelectionTypeSource()
}

class RecommendableMultiStakingSelection(
    val source: SelectionTypeSource,
    val selection: StartMultiStakingSelection,
)

val SelectionTypeSource.isRecommended: Boolean
    get() = when (this) {
        SelectionTypeSource.Automatic -> true
        is SelectionTypeSource.Manual -> contentRecommended
    }
