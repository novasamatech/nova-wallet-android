package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator.Response
import kotlinx.parcelize.Parcelize

interface DAppSearchRequester : InterScreenRequester<SearchPayload, Response>

interface DAppSearchResponder : InterScreenResponder<SearchPayload, Response>

interface DAppSearchCommunicator : DAppSearchRequester, DAppSearchResponder {

    sealed interface Response : Parcelable {

        @Parcelize
        class NewUrl(val url: String) : Response

        @Parcelize
        object Cancel : Response
    }
}
