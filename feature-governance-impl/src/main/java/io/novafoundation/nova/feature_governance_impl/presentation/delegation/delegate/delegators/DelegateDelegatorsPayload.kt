package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators

import android.os.Parcelable
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class DelegateDelegatorsPayload(val delegateId: AccountId) : Parcelable
