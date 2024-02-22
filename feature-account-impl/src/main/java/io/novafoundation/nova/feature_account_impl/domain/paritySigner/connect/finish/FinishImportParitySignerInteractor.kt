package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.finish

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.paritySigner.ParitySignerAddAccountRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface FinishImportParitySignerInteractor {

    suspend fun createWallet(
        name: String,
        substrateAccountId: AccountId,
        variant: PolkadotVaultVariant
    ): Result<Unit>
}

class RealFinishImportParitySignerInteractor(
    private val paritySignerAddAccountRepository: ParitySignerAddAccountRepository,
    private val accountRepository: AccountRepository,
) : FinishImportParitySignerInteractor {

    override suspend fun createWallet(
        name: String,
        substrateAccountId: AccountId,
        variant: PolkadotVaultVariant
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val metaId = paritySignerAddAccountRepository.addAccount(
                ParitySignerAddAccountRepository.Payload(
                    name,
                    substrateAccountId,
                    variant
                )
            )

            accountRepository.selectMetaAccount(metaId)
        }
    }
}
