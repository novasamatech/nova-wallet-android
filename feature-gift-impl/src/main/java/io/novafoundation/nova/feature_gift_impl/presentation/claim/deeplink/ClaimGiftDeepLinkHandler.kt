package io.novafoundation.nova.feature_gift_impl.presentation.claim.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.DialogMessageManager
import io.novafoundation.nova.common.utils.normalizeSeed
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.MutableSharedFlow
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.domain.ClaimGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.claim.ClaimGiftPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChain
import io.novasama.substrate_sdk_android.extensions.fromHex
import java.security.InvalidParameterException

class ClaimGiftDeepLinkHandler(
    private val router: GiftRouter,
    private val chainRegistry: ChainRegistry,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val dialogMessageManager: DialogMessageManager,
    private val claimGiftDeepLinkConfigurator: ClaimGiftDeepLinkConfigurator,
    private val claimGiftInteractor: ClaimGiftInteractor
) : DeepLinkHandler {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(claimGiftDeepLinkConfigurator.deepLinkPrefix)
    }

    override suspend fun handleDeepLink(uri: Uri) = runCatching {
        automaticInteractionGate.awaitInteractionAllowed()

        val data = uri.getQueryParameter(claimGiftDeepLinkConfigurator.payloadParam) ?: throw InvalidParameterException()
        val (secret, chainIdPrefix, symbol) = data.split("_")
        val secretBytes = secret.fromHex()
        val chain = findChain(chainIdPrefix) ?: throw InvalidParameterException()
        val chainAsset = chain.assets.first { it.symbol.value == symbol }
        require(chain.isEnabled)

        val claimableGift = claimGiftInteractor.getClaimableGift(secretBytes, chain.id, chainAsset.id)
        val isGiftAlreadyClaimed = claimGiftInteractor.isGiftAlreadyClaimed(claimableGift)
        if (isGiftAlreadyClaimed) {
            dialogMessageManager.showDialog {
                setTitle(R.string.claim_gift_already_claimed_title)
                setMessage(R.string.claim_gift_already_claimed_message)
                setPositiveButton(R.string.common_got_it, null)
            }

            return@runCatching
        }

        router.openClaimGift(ClaimGiftPayload(secretBytes, chain.id, chainAsset.id))
    }

    private suspend fun findChain(chainIdPrefix: String): Chain? {
        return chainRegistry.findChain {
            val normalisedChainId = claimGiftDeepLinkConfigurator.normaliseChainId(it.id)
            if (normalisedChainId.length >= claimGiftDeepLinkConfigurator.chainIdLength) {
                it.id.contains(chainIdPrefix)
            } else {
                it.id == chainIdPrefix
            }
        }
    }
}
