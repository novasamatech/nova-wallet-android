package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main

import android.os.Parcelable
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.parcelize.Parcelize

@Parcelize
class DelegateDetailsPayload(
    val accountId: AccountId
) : Parcelable
