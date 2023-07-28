package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parachain

import java.math.BigInteger

class ParachainMetadataRemote(
    val description: String,
    val icon: String,
    val name: String,
    val paraid: BigInteger,
    val token: String,
    val rewardRate: Double?,
    val customFlow: String?,
    val website: String,
    val extras: Map<String, String>?,
    val movedToParaId: BigInteger?,
)
