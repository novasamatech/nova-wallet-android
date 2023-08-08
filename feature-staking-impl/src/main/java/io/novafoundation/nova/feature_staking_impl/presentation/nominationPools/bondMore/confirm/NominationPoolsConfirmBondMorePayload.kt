package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class NominationPoolsConfirmBondMorePayload(
    val amount: BigDecimal,
    val fee: BigDecimal,
) : Parcelable
