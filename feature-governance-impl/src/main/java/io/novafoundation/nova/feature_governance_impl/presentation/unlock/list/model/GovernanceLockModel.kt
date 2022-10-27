package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId

class GovernanceLockModel(
    val referendumId: ReferendumId,
    val amount: String,
    val status: String,
    @ColorRes val statusColorRes: Int,
    @DrawableRes val statusIconRes: Int?,
    @ColorRes val statusIconColorRes: Int?
)
