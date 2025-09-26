package io.novafoundation.nova.feature_ahm_api.domain.model

import java.math.BigInteger
import java.util.Date

class ChainMigrationConfig(
    val sourceData: ChainData,
    val destinationData: ChainData,
    val blockNumberStartAt: BigInteger,
    val timeStartAt: Date,
    val newTokenNames: List<String>,
    val bannerPath: String,
    val wikiURL: String
) {

    class ChainData(
        val chainId: String,
        val assetId: Int,
        val minBalance: BigInteger,
        val averageFee: BigInteger
    )
}
