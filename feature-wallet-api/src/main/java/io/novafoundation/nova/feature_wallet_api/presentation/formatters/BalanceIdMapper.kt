package io.novafoundation.nova.feature_wallet_api.presentation.formatters

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceBreakdownIds
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance

fun mapBalanceIdToUi(resourceManager: ResourceManager, id: String): String {
    return when (id.trim()) {
        "staking" -> resourceManager.getString(R.string.assets_balance_details_locks_staking)
        "democrac" -> resourceManager.getString(R.string.assets_balance_details_locks_democrac_v1)
        "pyconvot" -> resourceManager.getString(R.string.assets_balance_details_locks_democrac_v2)
        "vesting" -> resourceManager.getString(R.string.assets_balance_details_locks_vesting)
        "phrelect" -> resourceManager.getString(R.string.assets_balance_details_locks_phrelect)
        BalanceBreakdownIds.RESERVED -> resourceManager.getString(R.string.wallet_balance_reserved)
        BalanceBreakdownIds.CROWDLOAN -> resourceManager.getString(R.string.assets_balance_details_locks_crowdloans)
        BalanceBreakdownIds.NOMINATION_POOL,
        BalanceBreakdownIds.NOMINATION_POOL_DELEGATED -> resourceManager.getString(R.string.setup_staking_type_pool_staking)
        else -> id.capitalize()
    }
}

val ExternalBalance.Type.balanceId: String
    get() = when (this) {
        ExternalBalance.Type.CROWDLOAN -> BalanceBreakdownIds.CROWDLOAN
        ExternalBalance.Type.NOMINATION_POOL -> BalanceBreakdownIds.NOMINATION_POOL
    }
