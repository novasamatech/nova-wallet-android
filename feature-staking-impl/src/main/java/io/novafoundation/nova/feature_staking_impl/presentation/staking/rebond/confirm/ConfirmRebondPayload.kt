package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
class ConfirmRebondPayload(
    val amount: BigDecimal
) : Parcelable
