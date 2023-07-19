package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.metadata

import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.metadata.response.DelegateMetadataRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import retrofit2.http.GET
import retrofit2.http.Path

interface DelegateMetadataApi {

    companion object {
        const val BASE_URL = "https://raw.githubusercontent.com/novasamatech/opengov-delegate-registry/master/registry/"
    }

    @GET("{fileName}")
    suspend fun getDelegatesMetadata(
        @Path("fileName") fileName: String,
    ): List<DelegateMetadataRemote>
}

suspend fun DelegateMetadataApi.getDelegatesMetadata(chain: Chain): List<DelegateMetadataRemote> {
    return getDelegatesMetadata(fileNameFor(chain))
}

private fun fileNameFor(chain: Chain): String {
    val withoutExtension = chain.name.lowercase()

    return "$withoutExtension.json"
}
