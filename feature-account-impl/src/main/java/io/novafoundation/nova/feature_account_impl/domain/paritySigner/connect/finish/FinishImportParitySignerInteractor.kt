package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.finish

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.addAccountWithSingleChange
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.substrate
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.paritySigner.ParitySignerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SubstrateKeypairAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SeedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.domain.utils.ScanSecret
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.String

interface FinishImportParitySignerInteractor {

    suspend fun createPolkadotVaultWallet(
        name: String,
        substrateAccountId: AccountId,
        variant: PolkadotVaultVariant
    ): Result<Unit>

    suspend fun createSecretWallet(
        name: String,
        secret: ScanSecret,
        variant: PolkadotVaultVariant
    ): Result<Unit>
}

class RealFinishImportParitySignerInteractor(
    private val paritySignerAddAccountRepository: ParitySignerAddAccountRepository,
    private val substrateKeypairAddAccountRepository: SubstrateKeypairAddAccountRepository,
    private val seedAddAccountRepository: SeedAddAccountRepository,
    private val accountRepository: AccountRepository
) : FinishImportParitySignerInteractor {

    override suspend fun createPolkadotVaultWallet(
        name: String,
        substrateAccountId: AccountId,
        variant: PolkadotVaultVariant
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val addAccountResult = paritySignerAddAccountRepository.addAccountWithSingleChange(
                ParitySignerAddAccountRepository.Payload(
                    name,
                    substrateAccountId,
                    variant
                )
            )

            accountRepository.selectMetaAccount(addAccountResult.metaId)
        }
    }

    override suspend fun createSecretWallet(
        name: String,
        secret: ScanSecret,
        variant: PolkadotVaultVariant
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val addAccountResult = when (secret) {
                is ScanSecret.Seed -> createBySeed(secret.data, name)
                is ScanSecret.EncryptedKeypair -> createByRawKey(secret.data, name)
            }

            accountRepository.selectMetaAccount(addAccountResult.metaId)
        }
    }

    private suspend fun createBySeed(
        secret: ByteArray,
        name: String
    ): AddAccountResult.SingleAccountChange = seedAddAccountRepository.addAccountWithSingleChange(
        SeedAddAccountRepository.Payload(
            seed = secret.toHexString(),
            advancedEncryption = getAdvancedEncryption(),
            addAccountType = AddAccountType.MetaAccount(name),
        )
    )

    private suspend fun createByRawKey(
        secret: ByteArray,
        name: String
    ): AddAccountResult.SingleAccountChange = substrateKeypairAddAccountRepository.addAccountWithSingleChange(
        SubstrateKeypairAddAccountRepository.Payload(
            substrateKeypair = secret,
            advancedEncryption = getAdvancedEncryption(),
            addAccountType = AddAccountType.MetaAccount(name),
        )
    )

    private fun getAdvancedEncryption() = AdvancedEncryption.substrate(CryptoType.SR25519, substrateDerivationPaths = null)
}
