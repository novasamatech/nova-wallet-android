package io.novafoundation.nova.feature_governance_impl.presentation.referenda.description

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ReferendumDescriptionPayload(
    val title: String?,
    val description: String
) : Parcelable
