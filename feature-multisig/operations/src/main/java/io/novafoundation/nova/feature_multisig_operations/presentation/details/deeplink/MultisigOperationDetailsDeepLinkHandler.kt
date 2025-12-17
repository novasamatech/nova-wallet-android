package io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.DialogMessageManager
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperationId
import io.novafoundation.nova.feature_account_api.data.multisig.model.create
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdKeyIn
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.common.fromOperationId
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.MultisigOperationDetailsPayload
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.toAccountIdKey
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class MultisigOperationDetailsDeepLinkHandler(
    private val router: MultisigOperationsRouter,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val dialogMessageManager: DialogMessageManager,
    private val multisigCallFormatter: MultisigCallFormatter,
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false

        return path.startsWith(MultisigOperationDeepLinkConfigurator.PREFIX)
    }

    override suspend fun handleDeepLink(data: Uri): Result<Unit> = runCatching {
        automaticInteractionGate.awaitInteractionAllowed()

        val chainId = data.getChainId()
        val chain = chainRegistry.getChain(chainId)

        val multisigAccount = data.getMultisigAddress()?.toAccountIdKey(chain) ?: error("Multisig address not found")
        val signatoryAccount = data.getSignatoryAddress()?.toAccountIdKey(chain) ?: error("Signatory address not found")
        val callHash = data.getCallHash() ?: error("Call hash not found")
        val callData = data.getCallData(chainId)
        val operationState = data.getOperationState()

        val multisigMetaAccount = accountRepository.getActiveMetaAccounts()
            .filterIsInstance<MultisigMetaAccount>()
            .firstOrNull { it.accountIdKeyIn(chain) == multisigAccount && it.signatoryAccountId == signatoryAccount }
            ?: error("Multisig account not found")

        accountRepository.selectMetaAccount(multisigMetaAccount.id)

        when (operationState) {
            null,
            MultisigOperationDeepLinkData.State.Active -> {
                val operationIdentifier = PendingMultisigOperationId.create(multisigMetaAccount, chain, callHash.removeHexPrefix())
                val operationPayload = MultisigOperationPayload.fromOperationId(operationIdentifier)
                router.openMultisigOperationDetails(
                    MultisigOperationDetailsPayload(
                        operationPayload,
                        navigationButtonMode = MultisigOperationDetailsPayload.NavigationButtonMode.CLOSE
                    )
                )
            }

            is MultisigOperationDeepLinkData.State.Executed -> showDialog(
                R.string.multisig_transaction_executed_dialog_title,
                multisigCallFormatter.formatExecutedOperationMessage(callData, signatoryAccount, chain)
            )

            is MultisigOperationDeepLinkData.State.Rejected -> showDialog(
                R.string.multisig_transaction_rejected_dialog_title,
                multisigCallFormatter.formatRejectedOperationMessage(callData, signatoryAccount, operationState.actorIdentity, chain)
            )
        }
    }

    private fun showDialog(titleRes: Int, messageText: String) {
        dialogMessageManager.showDialog {
            setTitle(titleRes)
            setMessage(messageText)
            setPositiveButton(R.string.common_got_it, null)
        }
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

    private suspend fun Uri.getCallData(chainId: String): GenericCall.Instance? {
        val callDataString = getQueryParameter(MultisigOperationDeepLinkConfigurator.CALL_DATA_PARAM) ?: return null
        val runtime = chainRegistry.getRuntime(chainId)
        return GenericCall.fromHex(runtime, callDataString)
    }
}
