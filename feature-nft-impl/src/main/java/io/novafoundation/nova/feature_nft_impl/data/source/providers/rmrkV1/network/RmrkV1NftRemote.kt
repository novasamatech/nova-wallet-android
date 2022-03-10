package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.network

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

class RmrkV1NftRemote(
    val id: String,
    @SerializedName("forsale")
    val price: BigInteger?,
    val collectionId: String,
    val instance: String,
    val name: String,
    @SerializedName("sn")
    val edition: String,
    val metadata: String
)

class RmrkV1CollectionRemote(
    val max: Int,
    val name: String,
    val issuer: String,
    val metadata: String?
)

class RmrkV1NftMetadataRemote(
    val image: String,
    val description: String
)
