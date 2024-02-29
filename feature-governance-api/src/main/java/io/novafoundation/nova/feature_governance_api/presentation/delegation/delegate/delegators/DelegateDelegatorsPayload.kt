package io.novafoundation.nova.feature_governance_api.presentation.delegation.delegate.delegators

import android.os.Parcelable
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class DelegateDelegatorsPayload(val delegateId: AccountId) : Parcelable
