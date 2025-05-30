package io.novafoundation.nova.feature_account_api.data.repository.addAccount.secrets

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType

interface MnemonicAddAccountRepository : AddAccountRepository<MnemonicAddAccountRepository.Payload> {

    class Payload(
        val mnemonic: String,
        val advancedEncryption: AdvancedEncryption,
        val addAccountType: AddAccountType
    )
}
