package io.novafoundation.nova.feature_nft_impl.data.source.providers.common

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.models.MetadataAttribute

internal fun mapJsonToAttributes(
    gson: Gson,
    attributesJson: String?
): List<NftDetails.Attribute> {
    return if (attributesJson == null) {
        emptyList()
    } else {
         val typeOfMetadataAttribute = TypeToken.getParameterized(List::class.java, MetadataAttribute::class.java).type
        gson.fromJson(attributesJson, typeOfMetadataAttribute) as List<MetadataAttribute>
    }.map {
        NftDetails.Attribute(
            label = it.label,
            value = if (it.value.isString) {
                it.value.asString
            } else {
                it.value.toString()
            }
        )
    }
}
