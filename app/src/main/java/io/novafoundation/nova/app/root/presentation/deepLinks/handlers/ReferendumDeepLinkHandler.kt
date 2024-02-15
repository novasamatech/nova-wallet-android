package io.novafoundation.nova.app.root.presentation.deepLinks.handlers

import android.net.Uri
import io.novafoundation.nova.app.root.presentation.deepLinks.CallbackEvent
import io.novafoundation.nova.app.root.presentation.deepLinks.DeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.common.DeepLinkHandlingException.ReferendumHandlingException
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigInteger

private const val GOV_DEEP_LINK_PREFIX = "/open/gov"

class ReferendumDeepLinkHandler(
    private val governanceRouter: GovernanceRouter,
    private val chainRegistry: ChainRegistry,
    private val mutableGovernanceState: MutableGovernanceState,
    private val automaticInteractionGate: AutomaticInteractionGate
) : DeepLinkHandler {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(GOV_DEEP_LINK_PREFIX)
    }

    override suspend fun handleDeepLink(data: Uri) {
        automaticInteractionGate.awaitInteractionAllowed()

        val chainId = data.getChainId() ?: throw ReferendumHandlingException.ChainIsNotFound
        val referendumId = data.getReferendumId() ?: throw ReferendumHandlingException.ReferendumIsNotSpecified
        val chain = chainRegistry.getChainOrNull(chainId) ?: throw ReferendumHandlingException.ChainIsNotFound
        val governanceType = data.getGovernanceType(chain)
        val payload = ReferendumDetailsPayload(referendumId)

        mutableGovernanceState.update(chain.id, chain.utilityAsset.id, governanceType)
        governanceRouter.openReferendum(payload)
    }

    private fun Uri.getChainId(): String? {
        return getQueryParameter("chainId")
    }

    private fun Uri.getReferendumId(): BigInteger? {
        return getQueryParameter("id")
            ?.toBigIntegerOrNull()
    }

    private fun Uri.getGovernanceType(chain: Chain): Chain.Governance {
        val supportedGov = chain.governance
        val govType = getQueryParameter("type")
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
