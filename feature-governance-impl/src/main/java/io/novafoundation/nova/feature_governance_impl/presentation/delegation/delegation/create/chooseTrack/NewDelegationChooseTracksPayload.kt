package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseTrack

import android.os.Parcelable
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.parcelize.Parcelize

@Parcelize
class NewDelegationChooseTracksPayload(
    val delegateId: AccountId,
    val isEditMode: Boolean
) : Parcelable
