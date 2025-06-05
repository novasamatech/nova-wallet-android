package io.novafoundation.nova.feature_account_api.presenatation.account.chain.model

import androidx.annotation.DrawableRes

data class ChainAccountGroupUi(
    val id: String,
    val title: String,
    val action: Action?
) {

    data class Action(
        val name: String,
        @DrawableRes val icon: Int
    )
}
