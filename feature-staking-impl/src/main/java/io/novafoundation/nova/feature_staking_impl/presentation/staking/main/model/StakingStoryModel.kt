package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StakingStoryModel(
    val titleRes: Int,
    val iconSymbol: String,
    val elements: List<Element>
) : Parcelable {

    @Parcelize
    class Element(
        val titleRes: Int,
        val bodyRes: Int,
        val url: String
    ) : Parcelable
}
