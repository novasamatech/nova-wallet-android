package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigDecimal

interface StartMultiStakingSelection {

    val stakingType: Chain.Asset.StakingType

    val apy: BigDecimal

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
