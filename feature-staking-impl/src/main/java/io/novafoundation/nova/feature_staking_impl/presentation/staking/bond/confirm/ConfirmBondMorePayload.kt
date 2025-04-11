package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
class ConfirmBondMorePayload(
    val amount: BigDecimal,
    val fee: FeeParcelModel,
    val stashAddress: String,
) : Parcelable
