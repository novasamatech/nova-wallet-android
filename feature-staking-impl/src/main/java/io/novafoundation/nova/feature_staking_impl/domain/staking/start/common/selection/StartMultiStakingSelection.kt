package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection

import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.model.MultiStakingType
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

interface StartMultiStakingSelection {

    val stakingOption: StakingOption

    val apy: Perbill?

    val stake: Balance

    fun ExtrinsicBuilder.startStaking(metaAccount: MetaAccount)

    fun isSettingsEquals(other: StartMultiStakingSelection): Boolean

    fun copyWith(stake: Balance): StartMultiStakingSelection
}

sealed class SelectionTypeSource {

    object Automatic : SelectionTypeSource()

    data class Manual(val contentRecommended: Boolean) : SelectionTypeSource()
}

data class RecommendableMultiStakingSelection(
    val source: SelectionTypeSource,
    val selection: StartMultiStakingSelection,
)

val SelectionTypeSource.isRecommended: Boolean
    get() = when (this) {
        SelectionTypeSource.Automatic -> true
        is SelectionTypeSource.Manual -> contentRecommended
    }
