package io.novafoundation.nova.runtime.multiNetwork.multiLocation


import android.util.Log
import io.novafoundation.nova.common.utils.enumValueOfOrNull
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.withRuntime
import io.novasama.substrate_sdk_android.runtime.definitions.types.RuntimeType
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.skipAliases
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.callOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.module.MetadataFunction
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull


interface XcmVersionDetector {

    suspend fun lowestPresentMultiLocationVersion(chainId: ChainId): XcmVersion?

    suspend fun lowestPresentMultiAssetsVersion(chainId: ChainId): XcmVersion?

    suspend fun lowestPresentMultiAssetVersion(chainId: ChainId): XcmVersion?

    suspend fun detectMultiLocationVersion(chainId: ChainId, multiLocationType: RuntimeType<*, *>?): XcmVersion?
}

class RealXcmVersionDetector(
    private val chainRegistry: ChainRegistry
) : XcmVersionDetector {

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

    override suspend fun detectMultiLocationVersion(chainId: ChainId, multiLocationType: RuntimeType<*, *>?): XcmVersion? {
        val actualCheckedType = multiLocationType?.skipAliases() ?: return null
        val versionedType = getVersionedType(
            chainId = chainId,
            getCall = { moduleOrNull(xcmPalletName())?.callOrNull("reserve_transfer_assets") },
            argumentName = "dest"
        ) ?: return null

        val matchingEnumEntry = versionedType.elements.values.find { enumEntry -> enumEntry.value.skipAliases().value === actualCheckedType }
            ?: run {
                Log.w("RealPalletXcmRepository", "Failed to find matching variant in versioned multiplication for type ${actualCheckedType.name}")

                return null
            }


        return enumValueOfOrNull<XcmVersion>(matchingEnumEntry.name)?.also {
            Log.d("RealPalletXcmRepository", "Identified xcm version for ${actualCheckedType.name} to be ${it.name}")
        }
    }

    private suspend fun lowestPresentXcmTypeVersionFromCallArgument(
        chainId: ChainId,
        getCall: (RuntimeMetadata) -> MetadataFunction?,
        argumentName: String,
    ): XcmVersion? {
        val type = getVersionedType(chainId, getCall, argumentName) ?: return null

        val allSupportedVersions = type.elements.values.map { it.name }
        val leastSupportedVersion = allSupportedVersions.min()

        return enumValueOfOrNull<XcmVersion>(leastSupportedVersion)
    }

    private suspend fun getVersionedType(
        chainId: ChainId,
        getCall: RuntimeMetadata.() -> MetadataFunction?,
        argumentName: String,
    ): DictEnum? {
        return chainRegistry.withRuntime(chainId) {
            val call = getCall(metadata) ?: return@withRuntime null
            val argument = call.arguments.find { it.name == argumentName } ?: return@withRuntime null
            argument.type?.skipAliases() as? DictEnum
        }
    }
}
