package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.network.singular

import com.google.gson.annotations.SerializedName

class SingularV2CollectionRemote(
    val metadata: String?,
    val issuer: String,
)

class SingularV2CollectionMetadata(
    val name: String,

    @SerializedName("image", alternate = ["mediaUri"])
    val image: String
)

