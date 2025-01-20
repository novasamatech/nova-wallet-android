package io.novafoundation.nova.feature_deep_linking.presentation.handling.handlers

import android.net.Uri
import io.novafoundation.nova.app.root.presentation.deepLinks.common.DeepLinkHandlingException.ReferendumHandlingException
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_link_building.presentation.ReferendumDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigInteger

class ReferendumDeepLinkHandler(
    private val router: DeepLinkingRouter,
    private val chainRegistry: ChainRegistry,
    private val mutableGovernanceState: MutableGovernanceState,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val govDeepLinkConfigurator: ReferendumDetailsDeepLinkConfigurator
) : DeepLinkHandler {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(govDeepLinkConfigurator.deepLinkPrefix)
    }

    override suspend fun handleDeepLink(data: Uri) {
        automaticInteractionGate.awaitInteractionAllowed()

        val chainId = data.getChainIdOrPolkadot()
        val referendumId = data.getReferendumId() ?: throw ReferendumHandlingException.ReferendumIsNotSpecified

        val chain = chainRegistry.getChainOrNull(chainId) ?: throw ReferendumHandlingException.ChainIsNotFound
        require(chain.isEnabled)

        val governanceType = data.getGovernanceType(chain)
        val payload = ReferendumDetailsPayload(referendumId, allowVoting = true, prefilledData = null)

        mutableGovernanceState.update(chain.id, chain.utilityAsset.id, governanceType)
        router.openReferendum(payload)
    }

    private fun Uri.getChainIdOrPolkadot(): String {
        return getQueryParameter(govDeepLinkConfigurator.chainIdParam) ?: ChainGeneses.POLKADOT
    }

    private fun Uri.getReferendumId(): BigInteger? {
        return getQueryParameter(govDeepLinkConfigurator.referendumIdParam)
            ?.toBigIntegerOrNull()
    }

    private fun Uri.getGovernanceType(chain: Chain): Chain.Governance {
        val supportedGov = chain.governance
        val govType = getQueryParameter(govDeepLinkConfigurator.governanceTypeParam)
            ?.toIntOrNull()

        val cantSelectGovType = govType == null && supportedGov.size > 1 && !supportedGov.contains(Chain.Governance.V2)

        return when {
            cantSelectGovType -> throw ReferendumHandlingException.GovernanceTypeIsNotSpecified
            govType == null && supportedGov.contains(Chain.Governance.V2) -> Chain.Governance.V2
            govType == null && supportedGov.size == 1 -> supportedGov.first()
            govType == 0 && supportedGov.contains(Chain.Governance.V2) -> Chain.Governance.V2
            govType == 1 && supportedGov.contains(Chain.Governance.V1) -> Chain.Governance.V1
            govType !in 0..1 -> throw ReferendumHandlingException.GovernanceTypeIsNotSupported
            else -> throw ReferendumHandlingException.GovernanceTypeIsNotSupported
        }
    }
}
