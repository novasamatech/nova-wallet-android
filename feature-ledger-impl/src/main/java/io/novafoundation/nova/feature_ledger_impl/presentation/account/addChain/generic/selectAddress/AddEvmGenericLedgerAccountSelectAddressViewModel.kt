package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.generic.AddEvmAccountToGenericLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.legacy.AddLedgerChainAccountInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.LedgerAccount
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.generic.GenericLedgerEvmAlertFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.generic.RealGenericLedgerEvmAlertFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.model.AddressVerificationMode
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEvmGenericLedgerAccountSelectAddressViewModel(
    private val router: LedgerRouter,
    private val payload: AddEvmGenericLedgerAccountSelectAddressPayload,
    private val addAccountInteractor: AddEvmAccountToGenericLedgerInteractor,
    private val selectAddressLedgerInteractor: SelectAddressLedgerInteractor,
    private val evmAlertFormatter: GenericLedgerEvmAlertFormatter,
    addressIconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
    chainRegistry: ChainRegistry,
    selectLedgerAddressPayload: SelectLedgerAddressPayload,
    messageCommandFormatter: MessageCommandFormatter
) : SelectAddressLedgerViewModel(
    router = router,
    interactor = selectAddressLedgerInteractor,
    addressIconGenerator = addressIconGenerator,
    resourceManager = resourceManager,
    payload = selectLedgerAddressPayload,
    chainRegistry = chainRegistry,
    messageCommandFormatter = messageCommandFormatter
) {

    override val ledgerVariant: LedgerVariant = LedgerVariant.GENERIC

    override val addressVerificationMode = AddressVerificationMode.Enabled(addressSchemesToVerify = listOf(AddressScheme.EVM))

    override suspend fun loadLedgerAccount(
        substratePreviewChain: Chain,
        deviceId: String,
        accountIndex: Int,
        ledgerVariant: LedgerVariant
    ): Result<LedgerAccount?> {
        return selectAddressLedgerInteractor.loadLedgerAccount(substratePreviewChain, deviceId, accountIndex, ledgerVariant).map { ledgerAccount ->
            if (ledgerAccount.evm != null) {
                ledgerAccount
            } else {
                _alertFlow.emit(evmAlertFormatter.createUpdateAppToGetEvmAddressAlert())

                null
            }
        }
    }

    override fun onAccountVerified(account: LedgerAccount) {
        launch {
            val result = addAccountInteractor.addEvmAccount(payload.metaId, account.evm!!)

            result
                .onSuccess { router.openMain() }
                .onFailure(::showError)
        }
    }
}
