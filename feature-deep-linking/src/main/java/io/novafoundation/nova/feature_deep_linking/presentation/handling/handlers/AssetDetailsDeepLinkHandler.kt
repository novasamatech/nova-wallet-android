package io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers

import android.net.Uri
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.findMetaAccountOrThrow
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.buildDeepLink
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

private const val ASSET_PREFIX = "/open/asset"

class AssetDetailsDeepLinkData(
    val accountAddress: String?,
    val chainId: String,
    val assetId: Int
)

private const val PARAM_ADDRESS = "address"
private const val PARAM_CHAIN_ID = "chainId"
private const val PARAM_ASSET_ID = "assetId"

class AssetDetailsDeepLinkHandler(
    private val router: DeepLinkingRouter,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val resourceManager: ResourceManager
) : DeepLinkHandler, DeepLinkConfigurator<AssetDetailsDeepLinkData> {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(ASSET_PREFIX)
    }

    override fun configure(payload: AssetDetailsDeepLinkData): Uri {
        return buildDeepLink(resourceManager, ASSET_PREFIX)
            .appendNullableQueryParameter(PARAM_ADDRESS, payload.accountAddress)
            .appendQueryParameter(PARAM_CHAIN_ID, payload.chainId)
            .appendQueryParameter(PARAM_ASSET_ID, payload.assetId.toString())
            .build()
    }

    override suspend fun handleDeepLink(data: Uri) {
        automaticInteractionGate.awaitInteractionAllowed()

        val address = data.getAddress()
        val chainId = data.getChainId() ?: throw IllegalStateException()
        val assetId = data.getAssetId() ?: throw IllegalStateException()

        val chain = chainRegistry.getChain(chainId)
        require(chain.isEnabled)

        address?.let { selectMetaAccount(chain, address) }

        val payload = AssetPayload(chainId, assetId)
        router.openAssetDetails(payload)
    }

    private suspend fun selectMetaAccount(chain: Chain, address: String) = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.findMetaAccountOrThrow(chain.accountIdOf(address), chain.id)
        accountRepository.selectMetaAccount(metaAccount.id)
    }

    private fun Uri.getAddress(): String? {
        return getQueryParameter(PARAM_ADDRESS)
    }

    private fun Uri.getChainId(): String? {
        return getQueryParameter(PARAM_CHAIN_ID)
    }

    private fun Uri.getAssetId(): Int? {
        return getQueryParameter(PARAM_ASSET_ID)?.toIntOrNull()
    }
}
