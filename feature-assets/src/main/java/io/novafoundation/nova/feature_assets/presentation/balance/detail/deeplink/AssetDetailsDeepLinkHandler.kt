package io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.findMetaAccountOrThrow
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

class AssetDetailsDeepLinkHandler(
    private val router: AssetsRouter,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val assetDetailsDeepLinkConfigurator: AssetDetailsDeepLinkConfigurator
) : DeepLinkHandler {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(assetDetailsDeepLinkConfigurator.deepLinkPrefix)
    }

    override suspend fun handleDeepLink(data: Uri) {
        automaticInteractionGate.awaitInteractionAllowed()

        val address = data.getAddress()
        val chainId = data.getChainIdOrPolkadot()
        val assetId = data.getAssetId() ?: throw IllegalStateException()

        val chain = chainRegistry.getChain(chainId)
        require(chain.isEnabled)

        address?.let { selectMetaAccount(chain, address) }

        val payload = AssetPayload(chainId, assetId)
        router.openAssetDetailsFromDeepLink(payload)
    }

    private suspend fun selectMetaAccount(chain: Chain, address: String) = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.findMetaAccountOrThrow(chain.accountIdOf(address), chain.id)
        accountRepository.selectMetaAccount(metaAccount.id)
    }

    private fun Uri.getAddress(): String? {
        return getQueryParameter(assetDetailsDeepLinkConfigurator.addressParam)
    }

    private fun Uri.getChainIdOrPolkadot(): String {
        return getQueryParameter(assetDetailsDeepLinkConfigurator.chainIdParam) ?: ChainGeneses.POLKADOT
    }

    private fun Uri.getAssetId(): Int? {
        return getQueryParameter(assetDetailsDeepLinkConfigurator.assetIdParam)?.toIntOrNull()
    }
}
