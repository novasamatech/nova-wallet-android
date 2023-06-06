package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain

import io.novafoundation.nova.common.utils.enumValueOfOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.skipAliases

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
        return lowestPresentXcmTypeVersion(chainId, "xcm.VersionedMultiLocation")
    }

    override suspend fun lowestPresentMultiAssetsVersion(chainId: ChainId): XcmVersion? {
        return lowestPresentXcmTypeVersion(chainId, "xcm.VersionedMultiAssets")
    }

    override suspend fun lowestPresentMultiAssetVersion(chainId: ChainId): XcmVersion? {
        return lowestPresentXcmTypeVersion(chainId, "xcm.VersionedMultiAsset")
    }

    private suspend fun lowestPresentXcmTypeVersion(chainId: ChainId, typeName: String): XcmVersion? {
        return remoteStorageDataSource.query(chainId) {
            val type = runtime.typeRegistry[typeName]?.skipAliases() as? DictEnum ?: return@query null

            val allSupportedVersions = type.elements.values.map { it.name }
            val leastSupportedVersion = allSupportedVersions.min()

            enumValueOfOrNull<XcmVersion>(leastSupportedVersion)
        }
    }
}
