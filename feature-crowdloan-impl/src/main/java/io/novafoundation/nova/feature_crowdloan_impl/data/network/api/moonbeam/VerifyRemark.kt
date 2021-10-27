package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam

import com.google.gson.annotations.SerializedName

class VerifyRemarkRequest(
    val address: String,
    @SerializedName("extrinsic-hash")
    val extrinsicHash: String,
    @SerializedName("block-hash")
    val blockHash: String,
)

class VerifyRemarkResponse(
    val verified: Boolean,
)
