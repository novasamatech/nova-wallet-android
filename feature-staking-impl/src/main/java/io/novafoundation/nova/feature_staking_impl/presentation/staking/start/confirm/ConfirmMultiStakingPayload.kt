package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConfirmMultiStakingPayload(val fee: FeeParcelModel) : Parcelable
