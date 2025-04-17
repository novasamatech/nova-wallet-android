package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
class ConfirmUnbondPayload(
    val amount: BigDecimal,
    val fee: FeeParcelModel
) : Parcelable
