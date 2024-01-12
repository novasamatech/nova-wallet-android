package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConfirmRemoveStakingProxyPayload(
    val proxyAddress: String
) : Parcelable
