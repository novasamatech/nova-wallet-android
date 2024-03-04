package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main

import android.os.Parcelable
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize

@Parcelize
class DelegateDetailsPayload(
    val accountId: AccountId
) : Parcelable
