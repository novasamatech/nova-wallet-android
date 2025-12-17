package io.novafoundation.nova.feature_ahm_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import java.util.Date

class ChainMigrationConfig(
    val originData: ChainData,
    val destinationData: ChainData,
    val blockNumberStartAt: BigInteger,
    val timeStartAt: Date,
    val newTokenNames: List<String>,
    val bannerPath: String,
    val migrationInProgress: Boolean,
    val wikiURL: String
) {

    class ChainData(
        val chainId: String,
        val assetId: Int,
        val minBalance: BigInteger,
        val averageFee: BigInteger
    )
}

class ChainMigrationConfigWithChains(
    val config: ChainMigrationConfig,
    val originChain: Chain,
    val originAsset: Chain.Asset,
    val destinationChain: Chain,
    val destinationAsset: Chain.Asset
)
