package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.deeplink

import android.net.Uri
import io.novafoundation.nova.feature_deep_linking.presentation.handling.common.DeepLinkHandlingException.ReferendumHandlingException
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigInteger

class ReferendumDeepLinkHandler(
    private val router: GovernanceRouter,
    private val chainRegistry: ChainRegistry,
    private val mutableGovernanceState: MutableGovernanceState,
    private val automaticInteractionGate: AutomaticInteractionGate
) : DeepLinkHandler {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(ReferendumDetailsDeepLinkConfigurator.PREFIX)
    }

    override suspend fun handleDeepLink(data: Uri) = runCatching {
        automaticInteractionGate.awaitInteractionAllowed()

        val chainId = data.getChainIdOrPolkadot()
        val referendumId = data.getReferendumId() ?: throw ReferendumHandlingException.ReferendumIsNotSpecified

        val chain = chainRegistry.getChainOrNull(chainId) ?: throw ReferendumHandlingException.ChainIsNotFound
        require(chain.isEnabled)

        val governanceType = data.getGovernanceType(chain)
        val payload = ReferendumDetailsPayload(referendumId, allowVoting = true, prefilledData = null)

        mutableGovernanceState.update(chain.id, chain.utilityAsset.id, governanceType)
        router.openReferendumFromDeepLink(payload)
    }

    private fun Uri.getChainIdOrPolkadot(): String {
        return getQueryParameter(ReferendumDetailsDeepLinkConfigurator.CHAIN_ID_PARAM) ?: ChainGeneses.POLKADOT
    }

    private fun Uri.getReferendumId(): BigInteger? {
        return getQueryParameter(ReferendumDetailsDeepLinkConfigurator.REFERENDUM_ID_PARAM)
            ?.toBigIntegerOrNull()
    }

    private fun Uri.getGovernanceType(chain: Chain): Chain.Governance {
        val supportedGov = chain.governance
        val govType = getQueryParameter(ReferendumDetailsDeepLinkConfigurator.GOVERNANCE_TYPE_PARAM)
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
