package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam

import com.google.gson.annotations.SerializedName
import java.util.UUID

class MakeSignatureRequest(
    val address: String,
    @SerializedName("previous-total-contribution")
    val previousTotalContribution: String,
    val contribution: String,
    val guid: String = UUID.randomUUID().toString(),
)

class MakeSignatureResponse(
    val signature: String,
)
