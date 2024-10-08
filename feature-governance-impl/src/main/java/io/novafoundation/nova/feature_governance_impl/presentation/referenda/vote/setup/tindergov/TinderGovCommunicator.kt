package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

interface TinderGovVoteRequester : InterScreenRequester<TinderGovVoteRequester.Request, TinderGovVoteResponder.Response> {

    @Parcelize
    class Request(val referendumId: BigInteger) : Parcelable
}

interface TinderGovVoteResponder : InterScreenResponder<TinderGovVoteRequester.Request, TinderGovVoteResponder.Response> {

    @Parcelize
    class Response(val success: Boolean) : Parcelable
}

interface TinderGovVoteCommunicator : TinderGovVoteRequester, TinderGovVoteResponder
