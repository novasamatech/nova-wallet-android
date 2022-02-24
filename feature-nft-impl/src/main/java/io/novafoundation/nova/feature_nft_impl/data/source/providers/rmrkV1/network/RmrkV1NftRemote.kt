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
    val metadata: String,
    @SerializedName("metadata_image")
    val metadataImage: String
)
