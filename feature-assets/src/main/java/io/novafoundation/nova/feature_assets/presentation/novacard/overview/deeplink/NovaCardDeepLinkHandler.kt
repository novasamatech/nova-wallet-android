package io.novafoundation.nova.feature_assets.presentation.novacard.overview.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.common.NovaCardRestrictionCheckMixin
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import kotlinx.coroutines.flow.MutableSharedFlow

private const val PATH = "/open/card"

private const val MERCURYO_PROVIDER = "mercuryo"
private const val DEFAULT_PROVIDER = MERCURYO_PROVIDER

class NovaCardDeepLinkHandler(
    private val router: AssetsRouter,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val novaCardRestrictionCheckMixin: NovaCardRestrictionCheckMixin
) : DeepLinkHandler {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(PATH)
    }

    override suspend fun handleDeepLink(data: Uri): Result<Unit> = runCatching {
        automaticInteractionGate.awaitInteractionAllowed()

        novaCardRestrictionCheckMixin.checkRestrictionAndDo {
            openProvider(data)
        }
    }

    private fun openProvider(data: Uri) {
        val provider = data.getQueryParameter("provider") ?: DEFAULT_PROVIDER

        when (provider) {
            MERCURYO_PROVIDER -> router.openNovaCard()
        }
    }
}
