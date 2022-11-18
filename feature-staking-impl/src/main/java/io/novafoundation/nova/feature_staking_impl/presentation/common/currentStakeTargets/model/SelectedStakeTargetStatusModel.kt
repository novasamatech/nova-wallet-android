package io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class SelectedStakeTargetModel(
    val addressModel: AddressModel,
    val nominated: AmountModel?,
    val apy: String?,
    val isOversubscribed: Boolean,
    val isSlashed: Boolean
)

data class SelectedStakeTargetStatusModel(
    val titleConfig: TitleConfig?,
    val description: String,
) {

    companion object;

    data class TitleConfig(
        val text: String,
        @DrawableRes val iconRes: Int,
        @ColorRes val iconTintRes: Int,
        @ColorRes val textColorRes: Int
    )
}

fun SelectedStakeTargetStatusModel.Companion.Active(
    resourceManager: ResourceManager,
    groupSize: Int,
    @StringRes description: Int
) = SelectedStakeTargetStatusModel(
    SelectedStakeTargetStatusModel.TitleConfig(
        text = resourceManager.getString(R.string.staking_your_elected_format, groupSize),
        iconRes = R.drawable.ic_checkmark_circle_16,
        iconTintRes = R.color.icon_positive,
        textColorRes = R.color.text_primary,
    ),
    description = resourceManager.getString(description)
)

fun SelectedStakeTargetStatusModel.Companion.Inactive(
    resourceManager: ResourceManager,
    groupSize: Int,
    @StringRes description: Int
) = SelectedStakeTargetStatusModel(
    SelectedStakeTargetStatusModel.TitleConfig(
        text = resourceManager.getString(R.string.staking_your_not_elected_format, groupSize),
        iconRes = R.drawable.ic_time_16,
        iconTintRes = R.color.text_secondary,
        textColorRes = R.color.text_secondary,
    ),
    description = resourceManager.getString(description)
)

fun SelectedStakeTargetStatusModel.Companion.Elected(
    resourceManager: ResourceManager,
    @StringRes description: Int
) = SelectedStakeTargetStatusModel(
    null,
    description = resourceManager.getString(description)
)

fun SelectedStakeTargetStatusModel.Companion.Waiting(
    resourceManager: ResourceManager,
    title: String,
    @StringRes description: Int
) = SelectedStakeTargetStatusModel(
    SelectedStakeTargetStatusModel.TitleConfig(
        text = title,
        iconRes = R.drawable.ic_time_16,
        iconTintRes = R.color.text_secondary,
        textColorRes = R.color.text_secondary,
    ),
    description = resourceManager.getString(description)
)
