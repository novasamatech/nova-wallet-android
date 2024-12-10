package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator.Response
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.CollatorParcelModel
import kotlinx.parcelize.Parcelize

interface SelectCollatorInterScreenRequester : InterScreenRequester<Request, Response>
interface SelectCollatorInterScreenResponder : InterScreenResponder<Request, Response>

interface SelectCollatorInterScreenCommunicator : SelectCollatorInterScreenRequester, SelectCollatorInterScreenResponder {

    @Parcelize
    class Response(val collator: CollatorParcelModel) : Parcelable

    @kotlinx.android.parcel.Parcelize
    object Request : Parcelable
}

fun SelectCollatorInterScreenRequester.openRequest() = openRequest(Request)
