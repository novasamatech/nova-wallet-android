package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class ConfirmSetControllerPayload(
    val fee: FeeParcelModel,
    val stashAddress: String,
    val controllerAddress: String,
    val transferable: BigDecimal
) : Parcelable
