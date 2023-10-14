package io.novafoundation.nova.feature_nft_impl.data.source.providers.common

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.models.MetadataAttribute

private const val YesValue = "Yes"
private const val NoValue = "No"

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
            value = when {
                it.value.isString -> {
                    val value = it.value.asString
                    when (value) {
                        "true" -> YesValue
                        "false" -> NoValue
                        else -> value
                    }
                }
                it.value.isBoolean -> {
                    if (it.value.asBoolean) {
                        YesValue
                    } else {
                        NoValue
                    }
                }
                else -> it.value.toString()
            }
        )
    }
}
