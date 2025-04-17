package io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize

interface SelectAddressRequester : InterScreenRequester<SelectAddressRequester.Request, SelectAddressResponder.Response> {

    @Parcelize
    class Request(
        val chainId: ChainId,
        val selectedAddress: String?,
        val filter: Filter
    ) : Parcelable {

        sealed interface Filter : Parcelable {
            @Parcelize
            object Everything : Filter

            @Parcelize
            class ExcludeMetaIds(val metaIds: List<Long>) : Filter
        }
    }
}

interface SelectAddressResponder : InterScreenResponder<SelectAddressRequester.Request, SelectAddressResponder.Response> {

    @Parcelize
    class Response(val selectedAddress: String) : Parcelable
}

interface SelectAddressCommunicator : SelectAddressRequester, SelectAddressResponder
