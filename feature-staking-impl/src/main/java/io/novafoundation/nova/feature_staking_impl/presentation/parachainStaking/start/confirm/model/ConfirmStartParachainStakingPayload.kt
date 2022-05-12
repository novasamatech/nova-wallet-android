package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model

import android.os.Parcelable
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class ConfirmStartParachainStakingPayload(
    val collatorId: AccountId,
    val amount: BigDecimal,
    val fee: BigDecimal
) : Parcelable
