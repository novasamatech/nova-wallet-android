package io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

interface SelectTracksRequester : InterScreenRequester<SelectTracksRequester.Request, SelectTracksResponder.Response> {

    @Parcelize
    class Request(
        val chainId: ChainId,
        val governanceType: Chain.Governance,
        val selectedTracks: Set<BigInteger>,
        val minTracks: Int,
    ) : Parcelable
}

interface SelectTracksResponder : InterScreenResponder<SelectTracksRequester.Request, SelectTracksResponder.Response> {

    @Parcelize
    class Response(
        val chainId: ChainId,
        val governanceType: Chain.Governance,
        val selectedTracks: Set<BigInteger>
    ) : Parcelable
}

interface SelectTracksCommunicator : SelectTracksRequester, SelectTracksResponder
