package io.novafoundation.nova.app.root.presentation.deepLinks

import android.net.Uri
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.GovernanceStateUpdater
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ReferendumDeepLinkHandler(
    private val governanceRouter: GovernanceRouter,
    private val chainRegistry: ChainRegistry,
    private val governanceStateUpdater: GovernanceStateUpdater,
    private val accountRepository: AccountRepository
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val userAccounts = accountRepository.allMetaAccounts()
        val path = data.path ?: return false
        val chainId = data.getChainId() ?: return false
        val referendumId = data.getReferendumId()
        val chainsById = chainRegistry.chainsById()
        val chain = chainsById[chainId]
        val governanceType = data.getGovernanceType()

        return userAccounts.isNotEmpty() &&
            path.startsWith("/open/gov") == true &&
            chain != null &&
            governanceType != null &&
            referendumId != null
    }

    override suspend fun handleDeepLink(data: Uri) {
        val chainId = data.getChainId() ?: return
        val referendumId = data.getReferendumId() ?: return
        val governanceType = data.getGovernanceType() ?: return
        val chain = chainRegistry.getChain(chainId)
        val payload = ReferendumDetailsPayload(referendumId)

        governanceStateUpdater.update(chain.id, chain.utilityAsset.id, governanceType)
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
            govType == null -> supportedGov.getIfExist(Chain.Governance.V2) ?: supportedGov.firstOrNull()
            govType == 0 && supportedGov.contains(Chain.Governance.V2) -> return Chain.Governance.V2
            govType == 1 && supportedGov.contains(Chain.Governance.V1) -> return Chain.Governance.V1
            else -> null
        }
    }

    private fun List<Chain.Governance>.getIfExist(governance: Chain.Governance): Chain.Governance? {
        return this.firstOrNull { it == governance }
    }
}
