package io.novafoundation.nova.web3names.data.serviceEndpoint

import com.google.gson.Gson
import io.novafoundation.nova.web3names.data.endpoints.TransferRecipientsApi

private const val TRANSFER_ASSETS_PROVIDER_DID_SERVICE_TYPE_V1 = "KiltTransferAssetRecipientV1"
private const val TRANSFER_ASSETS_PROVIDER_DID_SERVICE_TYPE_V2 = "KiltTransferAssetRecipientV2"

class W3NServiceEndpointHandlerFactory(
    private val transferRecipientsApi: TransferRecipientsApi,
    private val gson: Gson
) {

    fun getHandler(serviceEndpoints: List<ServiceEndpoint>): W3NServiceEndpointHandler? {
        val v2ServiceEndpoint = serviceEndpoints.firstOrNull { TRANSFER_ASSETS_PROVIDER_DID_SERVICE_TYPE_V2 in it.serviceTypes }
        if (v2ServiceEndpoint != null) {
            return W3NServiceEndpointHandlerV2(v2ServiceEndpoint, transferRecipientsApi, gson)
        }

        val v1ServiceEndpoint = serviceEndpoints.firstOrNull { TRANSFER_ASSETS_PROVIDER_DID_SERVICE_TYPE_V1 in it.serviceTypes }
        if (v1ServiceEndpoint != null) {
            return W3NServiceEndpointHandlerV1(v1ServiceEndpoint, transferRecipientsApi, gson)
        }

        return null
    }
}
