package io.novafoundation.nova.feature_staking_impl.presentation.mythos

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.SelectMythosInterScreenCommunicator.Request
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.MythosCollatorParcel
import kotlinx.android.parcel.Parcelize

interface SelectMythosInterScreenRequester : InterScreenRequester<Request, MythosCollatorParcel>
interface SelectMythosInterScreenResponder : InterScreenResponder<Request, MythosCollatorParcel>

interface SelectMythosInterScreenCommunicator : SelectMythosInterScreenRequester, SelectMythosInterScreenResponder {

    @Parcelize
    object Request : Parcelable
}

fun SelectMythosInterScreenRequester.openRequest() = openRequest(Request)
