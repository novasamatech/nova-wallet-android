package io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.data.multisig.model.createIdentifier
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.common.MultisigOperationDetailsPayload
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class MultisigOperationDetailsDeepLinkHandler(
    private val router: MultisigOperationsRouter,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val automaticInteractionGate: AutomaticInteractionGate
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(MultisigOperationDeepLinkConfigurator.PREFIX)
    }

    override suspend fun handleDeepLink(data: Uri): Result<Unit> = runCatching {
        automaticInteractionGate.awaitInteractionAllowed()

        val chainId = data.getChainId()
        val multisigAddress = data.getMultisigAddress() ?: error("Multisig address not found")
        val signatoryAddress = data.getSignatoryAddress() ?: error("Signatory address not found")
        val callHash = data.getCallHash() ?: error("Call hash not found")

        val chain = chainRegistry.getChain(chainId)
        val multisigMetaAccount = accountRepository.getActiveMetaAccounts()
            .filterIsInstance<MultisigMetaAccount>()
            .firstOrNull { it.addressIn(chain) == multisigAddress && chain.addressOf(it.signatoryAccountId) == signatoryAddress }
            ?: error("Multisig account not found")

        accountRepository.selectMetaAccount(multisigMetaAccount.id)

        val operationIdentifier = PendingMultisigOperation.createIdentifier(multisigMetaAccount.id, chain, callHash.removeHexPrefix())
        router.openMultisigOperationDetails(MultisigOperationDetailsPayload(operationIdentifier))
    }

    private fun Uri.getChainId(): String {
        return getQueryParameter(MultisigOperationDeepLinkConfigurator.CHAIN_ID_PARAM) ?: ChainGeneses.POLKADOT
    }

    private fun Uri.getMultisigAddress(): String? {
        return getQueryParameter(MultisigOperationDeepLinkConfigurator.MULTISIG_ADDRESS_PARAM)
    }

    private fun Uri.getSignatoryAddress(): String? {
        return getQueryParameter(MultisigOperationDeepLinkConfigurator.SIGNATORY_ADDRESS_PARAM)
    }

    private fun Uri.getCallHash(): String? {
        return getQueryParameter(MultisigOperationDeepLinkConfigurator.CALL_HASH_PARAM)
    }
}
