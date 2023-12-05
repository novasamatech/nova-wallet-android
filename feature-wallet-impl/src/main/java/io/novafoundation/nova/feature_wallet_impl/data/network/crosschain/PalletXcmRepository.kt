package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.utils.enumValueOfOrNull
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.skipAliases
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.callOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.MetadataFunction
import jp.co.soramitsu.fearless_utils.runtime.metadata.moduleOrNull

enum class XcmVersion {
    V0, V1, V2, V3;
}

interface PalletXcmRepository {

    suspend fun lowestPresentMultiLocationVersion(chainId: ChainId): XcmVersion?

    suspend fun lowestPresentMultiAssetsVersion(chainId: ChainId): XcmVersion?

    suspend fun lowestPresentMultiAssetVersion(chainId: ChainId): XcmVersion?
}

class RealPalletXcmRepository(
    private val remoteStorageDataSource: StorageDataSource,
) : PalletXcmRepository {

    override suspend fun lowestPresentMultiLocationVersion(chainId: ChainId): XcmVersion? {
        return lowestPresentXcmTypeVersionFromCallArgument(
            chainId = chainId,
            getCall = { it.moduleOrNull(it.xcmPalletName())?.callOrNull("reserve_transfer_assets") },
            argumentName = "dest"
        )
    }

    override suspend fun lowestPresentMultiAssetsVersion(chainId: ChainId): XcmVersion? {
        return lowestPresentXcmTypeVersionFromCallArgument(
            chainId = chainId,
            getCall = { it.moduleOrNull(it.xcmPalletName())?.callOrNull("reserve_transfer_assets") },
            argumentName = "assets"
        )
    }

    override suspend fun lowestPresentMultiAssetVersion(chainId: ChainId): XcmVersion? {
        return lowestPresentMultiAssetsVersion(chainId)
    }

    private suspend fun lowestPresentXcmTypeVersionFromCallArgument(
        chainId: ChainId,
        getCall: (RuntimeMetadata) -> MetadataFunction?,
        argumentName: String,
    ): XcmVersion? {
        return remoteStorageDataSource.query(chainId) {
            val call = getCall(runtime.metadata) ?: return@query null
            val argument = call.arguments.find { it.name == argumentName } ?: return@query null
            val type = argument.type?.skipAliases() as? DictEnum ?: return@query null

            val allSupportedVersions = type.elements.values.map { it.name }
            val leastSupportedVersion = allSupportedVersions.min()

            enumValueOfOrNull<XcmVersion>(leastSupportedVersion)
        }
    }
}
