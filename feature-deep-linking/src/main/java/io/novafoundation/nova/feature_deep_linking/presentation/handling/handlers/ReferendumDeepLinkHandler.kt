package io.novafoundation.nova.app.root.presentation.deepLinks.handlers

import android.net.Uri
import io.novafoundation.nova.app.root.presentation.deepLinks.common.DeepLinkHandlingException.ReferendumHandlingException
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.buildDeepLink
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
import java.math.BigInteger
import kotlinx.coroutines.flow.MutableSharedFlow

private const val GOV_DEEP_LINK_PREFIX = "/open/gov"

private const val PARAM_CHAIN_ID = "chainId"
private const val PARAM_REF_ID = "id"
private const val PARAM_GOV_TYPE = "type"

class ReferendumDeepLinkConfigPayload(
    val chainId: String,
    val referendumId: BigInteger,
    val governanceType: Chain.Governance
)

class ReferendumDeepLinkHandler(
    private val router: DeepLinkingRouter,
    private val chainRegistry: ChainRegistry,
    private val mutableGovernanceState: MutableGovernanceState,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val resourceManager: ResourceManager
) : DeepLinkHandler, DeepLinkConfigurator<ReferendumDeepLinkConfigPayload> {

    override val callbackFlow = MutableSharedFlow<CallbackEvent>()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(GOV_DEEP_LINK_PREFIX)
    }

    override fun configure(payload: ReferendumDeepLinkConfigPayload): Uri {
        return buildDeepLink(resourceManager, GOV_DEEP_LINK_PREFIX)
            .appendQueryParameter(PARAM_CHAIN_ID, payload.chainId)
            .appendQueryParameter(PARAM_REF_ID, payload.referendumId.toString())
            .appendQueryParameter(PARAM_GOV_TYPE, mapGovTypeToParams(payload.governanceType).toString())
            .build()
    }

    override suspend fun handleDeepLink(data: Uri) {
        automaticInteractionGate.awaitInteractionAllowed()

        val chainId = data.getChainId() ?: throw ReferendumHandlingException.ChainIsNotFound
        val referendumId = data.getReferendumId() ?: throw ReferendumHandlingException.ReferendumIsNotSpecified
        val chain = chainRegistry.getChainOrNull(chainId) ?: throw ReferendumHandlingException.ChainIsNotFound
        val governanceType = data.getGovernanceType(chain)
        val payload =
            io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload(
                referendumId
            )

        mutableGovernanceState.update(chain.id, chain.utilityAsset.id, governanceType)
        router.openReferendum(payload)
    }

    private fun Uri.getChainId(): String? {
        return getQueryParameter(PARAM_CHAIN_ID)
    }

    private fun Uri.getReferendumId(): BigInteger? {
        return getQueryParameter(PARAM_REF_ID)
            ?.toBigIntegerOrNull()
    }

    private suspend fun Uri.getGovernanceType(chain: Chain): Chain.Governance {
        val supportedGov = chain.governance
        val govType = getQueryParameter(PARAM_GOV_TYPE)
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

    private fun Chain.Governance.takeIfContainedIn(list: List<Chain.Governance>): Chain.Governance? {
        return list.firstOrNull { it == this }
    }

    private fun mapGovTypeToParams(govType: Chain.Governance): String {
        return when (govType) {
            Chain.Governance.V1 -> 1.toString()
            Chain.Governance.V2 -> 0.toString()
        }
    }
}
