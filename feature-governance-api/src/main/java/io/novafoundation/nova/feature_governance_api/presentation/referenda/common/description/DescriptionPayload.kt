package io.novafoundation.nova.feature_governance_api.presentation.referenda.common.description

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class DescriptionPayload(
    val description: String,
    val toolbarTitle: String? = null,
    val title: String? = null
) : Parcelable
