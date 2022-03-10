package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.network

import com.google.gson.annotations.SerializedName
import java.math.BigInteger

class RmrkV2NftRemote(
    val id: String,
    @SerializedName("forsale")
    val price: BigInteger?,
    val collectionId: String,
    @SerializedName("metadata_name")
    val name: String,
    @SerializedName("metadata_description")
    val description: String,
    @SerializedName("sn")
    val edition: String,
    val image: String?,
    val metadata: String,
)

class RmrkV2NftMetadataRemote(
    val image: String
)
