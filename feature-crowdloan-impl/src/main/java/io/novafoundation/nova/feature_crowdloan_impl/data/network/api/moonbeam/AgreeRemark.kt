package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam

import com.google.gson.annotations.SerializedName

class AgreeRemarkRequest(
    val address: String,
    @SerializedName("signed-message")
    val signedMessage: String,
)

class AgreeRemarkResponse(
    val remark: String,
)
