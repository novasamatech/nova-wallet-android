package io.novafoundation.nova.feature_governance_impl.presentation.common.description

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class DescriptionPayload(
    val description: String,
    val toolbarTitle: String? = null,
    val title: String? = null
) : Parcelable
