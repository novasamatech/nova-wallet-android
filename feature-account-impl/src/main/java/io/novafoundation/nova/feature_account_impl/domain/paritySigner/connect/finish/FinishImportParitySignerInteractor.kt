package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.finish

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.addAccountWithSingleChange
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.recommended
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.paritySigner.ParitySignerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.RawKeyAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SeedAddAccountRepository
import io.novasama.substrate_sdk_android.encrypt.qr.ScanSecret
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.String

interface FinishImportParitySignerInteractor {

    suspend fun createWalletPolkadotVaultWallet(
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
    private val rawKeyAddAccountRepository: RawKeyAddAccountRepository,
    private val seedAddAccountRepository: SeedAddAccountRepository,
    private val accountRepository: AccountRepository,
    private val encryptionDefaults: EncryptionDefaults
) : FinishImportParitySignerInteractor {

    override suspend fun createWalletPolkadotVaultWallet(
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
                is ScanSecret.RawKey -> createByRawKey(secret.data, name)
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
            advancedEncryption = encryptionDefaults.recommended(),
            addAccountType = AddAccountType.MetaAccount(name),
        )
    )

    private suspend fun createByRawKey(
        secret: ByteArray,
        name: String
    ): AddAccountResult.SingleAccountChange = rawKeyAddAccountRepository.addAccountWithSingleChange(
        RawKeyAddAccountRepository.Payload(
            rawKey = secret,
            advancedEncryption = encryptionDefaults.recommended(),
            addAccountType = AddAccountType.MetaAccount(name),
        )
    )
}
