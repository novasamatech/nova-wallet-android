package io.novafoundation.nova.app.root.presentation.deepLinks.handlers

import android.net.Uri
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.buildDeepLink
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.coroutines.flow.MutableSharedFlow

private const val ASSET_PREFIX = "/open/asset"

class AssetDetailsLinkConfigPayload(
    val chainId: String,
    val assetId: Int
)

private const val PARAM_CHAIN_ID = "chainId"
private const val PARAM_ASSET_ID = "assetId"

class AssetDetailsDeepLinkHandler(
    private val router: DeepLinkingRouter,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val resourceManager: ResourceManager
) : DeepLinkHandler, DeepLinkConfigurator<AssetDetailsLinkConfigPayload> {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(ASSET_PREFIX)
    }

    override fun configure(payload: AssetDetailsLinkConfigPayload): Uri {
        return buildDeepLink(resourceManager, ASSET_PREFIX)
            .appendQueryParameter(PARAM_CHAIN_ID, payload.chainId)
            .appendQueryParameter(PARAM_ASSET_ID, payload.assetId.toString())
            .build()
    }

    override suspend fun handleDeepLink(data: Uri) {
        automaticInteractionGate.awaitInteractionAllowed()

        val chainId = data.getChainId() ?: throw IllegalStateException()
        val assetId = data.getAssetId() ?: throw IllegalStateException()
        val payload = AssetPayload(chainId, assetId)
        router.openAssetDetails(payload)
    }

    private fun Uri.getChainId(): String? {
        return getQueryParameter(PARAM_CHAIN_ID)
    }

    private fun Uri.getAssetId(): Int? {
        return getQueryParameter(PARAM_ASSET_ID)?.toIntOrNull()
    }
}
