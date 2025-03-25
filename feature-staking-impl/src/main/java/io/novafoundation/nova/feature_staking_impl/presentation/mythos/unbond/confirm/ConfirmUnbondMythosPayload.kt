package io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.MythosCollatorParcel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConfirmUnbondMythosPayload(
    val collator: MythosCollatorParcel,
    val amount: Balance,
    val fee: FeeParcelModel
) : Parcelable
