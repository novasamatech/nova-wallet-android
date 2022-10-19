package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters

import android.os.Parcelable
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

@Parcelize
class ReferendumVotersPayload(val title: String, val referendumId: BigInteger) : Parcelable
