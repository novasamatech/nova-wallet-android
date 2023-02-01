package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.tracks.select

import android.os.Parcelable
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class SelectDelegationTracksPayload(
    val delegateId: AccountId
) : Parcelable
