package io.novafoundation.nova.app.root.presentation.deepLinks

import android.net.Uri
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.MutableGovernanceState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val GOV_DEEP_LINK_PREFIX = "/open/gov"

class ReferendumDeepLinkHandler(
    private val governanceRouter: GovernanceRouter,
    private val chainRegistry: ChainRegistry,
    private val mutableGovernanceState: MutableGovernanceState,
    private val accountRepository: AccountRepository,
    private val automaticInteractionGate: AutomaticInteractionGate
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(GOV_DEEP_LINK_PREFIX)
    }

    override suspend fun handleDeepLink(data: Uri) {
        // TODO: check if user has account
        val chainId = data.getChainId() ?: return // TODO: show error message instead
        val referendumId = data.getReferendumId() ?: return // TODO: show error message instead
        val governanceType = data.getGovernanceType() ?: return // TODO: show error message instead
        val chain = chainRegistry.getChain(chainId)
        val payload = ReferendumDetailsPayload(referendumId)

        automaticInteractionGate.awaitInteractionAllowed()
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

    private suspend fun Uri.getGovernanceType(): Chain.Governance? {
        val chainId = this.getChainId() ?: return null
        val chain = chainRegistry.getChain(chainId)
        val supportedGov = chain.governance
        val govType = getQueryParameter("type")
            ?.toIntOrNull()

        return when {
            govType == null -> Chain.Governance.V2.takeIfContainedIn(supportedGov) ?: supportedGov.firstOrNull()
            govType == 0 && supportedGov.contains(Chain.Governance.V2) -> return Chain.Governance.V2
            govType == 1 && supportedGov.contains(Chain.Governance.V1) -> return Chain.Governance.V1
            else -> null
        }
    }

    private fun Chain.Governance.takeIfContainedIn(list: List<Chain.Governance>): Chain.Governance? {
        return list.firstOrNull { it == this }
    }
}
