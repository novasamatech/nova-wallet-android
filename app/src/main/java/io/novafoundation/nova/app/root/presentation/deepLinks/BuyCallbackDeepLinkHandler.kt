package io.novafoundation.nova.app.root.presentation.deepLinks

import android.content.Intent
import android.net.Uri
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class BuyCallbackDeepLinkHandler(
    private val interactor: RootInteractor,
    private val resourceManager: ResourceManager
) : DeepLinkHandler {

    override val callbackFlow: MutableSharedFlow<CallbackEvent> = singleReplaySharedFlow()

    override suspend fun matches(data: Uri): Boolean {
        return interactor.isBuyProviderRedirectLink(data.toString())
    }

    override suspend fun handleDeepLink(data: Uri) {
        val message = resourceManager.getString(R.string.buy_completed)
        callbackFlow.emit(CallbackEvent.Message(message))
    }
}
