package io.novafoundation.nova.feature_ahm_impl.data.config

import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfig
import java.util.*
import kotlin.time.Duration.Companion.seconds

class ChainMigrationConfigRemote(
    val sourceData: ChainData,
    val destinationData: ChainData,
    val blockNumber: Long,
    val timestamp: Long,
    val newTokenNames: List<String>,
    val bannerPath: String,
    val wikiURL: String
) {

    class ChainData(
        val chainId: String,
        val assetId: Int,
        val minBalance: Long,
        val averageFee: Long
    )
}

fun ChainMigrationConfigRemote.toDomain(): ChainMigrationConfig {
    return ChainMigrationConfig(
        sourceData = sourceData.toDomain(),
        destinationData = destinationData.toDomain(),
        blockNumberStartAt = blockNumber.toBigInteger(),
        timeStartAt = Date(timestamp.seconds.inWholeMilliseconds),
        newTokenNames = newTokenNames,
        bannerPath = bannerPath,
        wikiURL = wikiURL
    )
}

fun ChainMigrationConfigRemote.ChainData.toDomain(): ChainMigrationConfig.ChainData {
    return ChainMigrationConfig.ChainData(
        chainId = chainId,
        assetId = assetId,
        minBalance = minBalance.toBigInteger(),
        averageFee = averageFee.toBigInteger()
    )
}
