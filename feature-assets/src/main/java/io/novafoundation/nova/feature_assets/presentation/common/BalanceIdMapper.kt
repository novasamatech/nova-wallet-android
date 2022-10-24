package io.novafoundation.nova.feature_assets.presentation.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.feature_assets.R

fun mapBalanceIdToUi(resourceManager: ResourceManager, id: String): String {
    return when (id.trim()) {
        "staking" -> resourceManager.getString(R.string.assets_balance_details_locks_staking)
        "democrac", "pyconvot" -> resourceManager.getString(R.string.assets_balance_details_locks_democrac)
        "vesting" -> resourceManager.getString(R.string.assets_balance_details_locks_vesting)
        "phrelect" -> resourceManager.getString(R.string.assets_balance_details_locks_phrelect)
        "reserved" -> resourceManager.getString(R.string.wallet_balance_reserved)
        "crowdloan" -> resourceManager.getString(R.string.assets_balance_details_locks_crowdloans)
        else -> id.capitalize()
    }
}
