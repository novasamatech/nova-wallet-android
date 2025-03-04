package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.asset
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.NominationPoolSelection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

interface StartMultiStakingSelection {

    val stakingOption: StakingOption

    val apy: Fraction?

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
    val properties: SingleStakingProperties
)

fun StartMultiStakingSelection.copyWith(newAmount: BigDecimal) = copyWith(
    stake = stakingOption.asset.planksFromAmount(newAmount)
)

fun RecommendableMultiStakingSelection.copyWith(newAmount: Balance) = copy(
    selection = selection.copyWith(newAmount)
)

fun RecommendableMultiStakingSelection.copyWith(newAmount: BigDecimal) = copy(
    selection = selection.copyWith(newAmount)
)

val SelectionTypeSource.isRecommended: Boolean
    get() = when (this) {
        SelectionTypeSource.Automatic -> true
        is SelectionTypeSource.Manual -> contentRecommended
    }

fun StartMultiStakingSelection.stakeAmount(): BigDecimal = stakingOption.asset.amountFromPlanks(stake)

val RecommendableMultiStakingSelection.isNominationPoolSelection: Boolean
    get() = selection is NominationPoolSelection
