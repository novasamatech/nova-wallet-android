package io.novafoundation.nova.feature_ledger_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress.AddEvmGenericLedgerAccountSelectAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectLedger.AddEvmAccountSelectGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectAddress.AddLedgerChainAccountSelectAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.FinishImportGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.PreviewImportGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectLedger.SelectLedgerGenericPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.FillWalletImportLedgerLegacyPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish.FinishImportLedgerPayload

interface LedgerRouter : ReturnableRouter {

    fun openImportFillWallet(payload: FillWalletImportLedgerLegacyPayload)

    fun returnToImportFillWallet()

    fun openSelectImportAddress(payload: SelectLedgerAddressPayload)

    fun openCreatePincode()

    fun openMain()

    fun openFinishImportLedger(payload: FinishImportLedgerPayload)

    fun finishSignFlow()

    fun openAddChainAccountSelectAddress(payload: AddLedgerChainAccountSelectAddressPayload)

    // Generic app flows

    fun openSelectLedgerGeneric(payload: SelectLedgerGenericPayload)

    fun openSelectAddressGenericLedger(payload: SelectLedgerAddressPayload)

    fun openPreviewLedgerAccountsGeneric(payload: PreviewImportGenericLedgerPayload)

    fun openFinishImportLedgerGeneric(payload: FinishImportGenericLedgerPayload)

    fun openAddGenericEvmAddressSelectAddress(payload: AddEvmGenericLedgerAccountSelectAddressPayload)
}
