package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks

import android.os.Parcelable
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class RevokeDelegationChooseTracksPayload(val delegateId: AccountId) : Parcelable
