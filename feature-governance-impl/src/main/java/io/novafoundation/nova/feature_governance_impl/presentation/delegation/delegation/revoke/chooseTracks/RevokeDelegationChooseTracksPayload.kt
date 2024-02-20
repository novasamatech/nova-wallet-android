package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks

import android.os.Parcelable
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class RevokeDelegationChooseTracksPayload(val delegateId: AccountId) : Parcelable
