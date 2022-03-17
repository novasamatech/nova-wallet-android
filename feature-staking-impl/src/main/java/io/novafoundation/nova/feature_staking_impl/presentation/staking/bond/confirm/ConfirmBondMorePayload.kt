package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class ConfirmBondMorePayload(
    val amount: BigDecimal,
    val fee: BigDecimal,
    val stashAddress: String,
) : Parcelable
