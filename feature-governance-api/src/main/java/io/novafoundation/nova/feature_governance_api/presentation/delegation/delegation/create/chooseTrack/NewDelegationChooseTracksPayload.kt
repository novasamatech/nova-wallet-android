package io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.create.chooseTrack

import android.os.Parcelable
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class NewDelegationChooseTracksPayload(
    val delegateId: AccountId,
    val isEditMode: Boolean
) : Parcelable
