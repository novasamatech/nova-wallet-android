package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.network.singular

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.models.MetadataAttribute
import java.math.BigInteger

class SingularV2CollectionRemote(
    val metadata: String?,
    val issuer: String,
    val max: Int?
)

class SingularV2NftRemote(
    val id: String,
    @SerializedName("forsale")
    val price: BigInteger?,
    val collectionId: String,
    @SerializedName("sn")
    val edition: String,
    val image: String?, // prerender, non-null if nft is composable
    val metadata: String?,
    val symbol: String,
)

class SingularV2CollectionMetadata(
    val name: String,
    val description: String?,

    @SerializedName("image", alternate = ["mediaUri"])
    val image: String?,
    val tags: List<String>?,
    val attributes: List<MetadataAttribute>?
)
