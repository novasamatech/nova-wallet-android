package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.toHexString
import java.math.BigInteger

typealias HexHash = String

class PreImageRequest(
    val hash: ByteArray,
    val knownSize: BigInteger?,
    val fetchIf: FetchCondition,
) {

    val hashHex: HexHash = hash.toHexString()

    enum class FetchCondition {
        ALWAYS, SMALL_SIZE
    }
}

interface PreImageRepository {

    suspend fun getPreimageFor(request: PreImageRequest, chainId: ChainId): PreImage?

    suspend fun getPreimagesFor(requests: Collection<PreImageRequest>, chainId: ChainId): Map<HexHash, PreImage?>
}
