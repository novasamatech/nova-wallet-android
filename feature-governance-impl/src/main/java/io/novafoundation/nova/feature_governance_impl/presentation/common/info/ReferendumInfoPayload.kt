package io.novafoundation.nova.feature_governance_impl.presentation.common.info

import android.os.Parcelable
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

@Parcelize
class ReferendumInfoPayload(
    val referendumId: BigInteger
) : Parcelable
