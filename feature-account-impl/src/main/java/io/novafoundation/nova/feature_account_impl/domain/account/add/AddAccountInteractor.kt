package io.novafoundation.nova.feature_account_impl.domain.account.add

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.addAccountWithSingleChange
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.ImportJsonMetaData
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SubstrateKeypairAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.JsonAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SeedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.TrustWalletAddAccountRepository
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.utils.isSubstrateKeypair
import io.novafoundation.nova.feature_account_impl.domain.utils.isSubstrateSeed
import io.novasama.substrate_sdk_android.extensions.fromHex
import java.util.InvalidPropertiesFormatException

class AddAccountInteractor(
    private val mnemonicAddAccountRepository: MnemonicAddAccountRepository,
    private val jsonAddAccountRepository: JsonAddAccountRepository,
    private val seedAddAccountRepository: SeedAddAccountRepository,
    private val substrateKeypairAddAccountRepository: SubstrateKeypairAddAccountRepository,
    private val trustWalletAddAccountRepository: TrustWalletAddAccountRepository,
    private val accountRepository: AccountRepository,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor
) {

    suspend fun createMetaAccountWithRecommendedSettings(addAccountType: AddAccountType): Result<Unit> {
        val mnemonic = accountRepository.generateMnemonic()
        val advancedEncryption = advancedEncryptionInteractor.getRecommendedAdvancedEncryption()
        return createAccount(mnemonic.words, advancedEncryption, addAccountType)
    }

    suspend fun createAccount(
        mnemonic: String,
        advancedEncryption: AdvancedEncryption,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(
            addAccountType,
            mnemonicAddAccountRepository,
            MnemonicAddAccountRepository.Payload(
                mnemonic,
                advancedEncryption,
                addAccountType
            )
        )
    }

    suspend fun importFromMnemonic(
        mnemonic: String,
        advancedEncryption: AdvancedEncryption,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return createAccount(mnemonic, advancedEncryption, addAccountType)
    }

    suspend fun importFromSecret(
        secret: String,
        advancedEncryption: AdvancedEncryption,
        addAccountType: AddAccountType
    ): Result<Unit> = runCatching {
        when {
            secret.isSubstrateSeed() -> importFromSeed(secret, advancedEncryption, addAccountType).getOrThrow()

            secret.isSubstrateKeypair() -> importFromSubstrateKeypair(secret, advancedEncryption, addAccountType).getOrThrow()

            else -> throw InvalidPropertiesFormatException("Invalid secret length: Expected 32 or 64 bytes.")
        }
    }

    suspend fun importFromSeed(
        seed: String,
        advancedEncryption: AdvancedEncryption,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(
            addAccountType,
            seedAddAccountRepository,
            SeedAddAccountRepository.Payload(
                seed,
                advancedEncryption,
                addAccountType
            )
        )
    }

    suspend fun importFromSubstrateKeypair(
        keypair: String,
        advancedEncryption: AdvancedEncryption,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(
            addAccountType,
            substrateKeypairAddAccountRepository,
            SubstrateKeypairAddAccountRepository.Payload(
                keypair.fromHex(),
                advancedEncryption,
                addAccountType
            )
        )
    }

    suspend fun importFromJson(
        json: String,
        password: String,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(
            addAccountType,
            jsonAddAccountRepository,
            JsonAddAccountRepository.Payload(
                json = json,
                password = password,
                addAccountType = addAccountType
            )
        )
    }

    suspend fun importFromTrustWallet(mnemonic: String, addAccountType: AddAccountType.MetaAccount): Result<Unit> {
        return addAccount(
            addAccountType = addAccountType,
            addAccountRepository = trustWalletAddAccountRepository,
            payload = TrustWalletAddAccountRepository.Payload(mnemonic, addAccountType)
        )
    }

    suspend fun extractJsonMetadata(json: String): Result<ImportJsonMetaData> {
        return runCatching {
            jsonAddAccountRepository.extractJsonMetadata(json)
        }
    }

    private suspend inline fun <T> addAccount(
        addAccountType: AddAccountType,
        addAccountRepository: AddAccountRepository<T>,
        payload: T
    ): Result<Unit> {
        return runCatching {
            val result = addAccountRepository.addAccountWithSingleChange(payload)

            if (addAccountType is AddAccountType.MetaAccount) {
                accountRepository.selectMetaAccount(result.metaId)
            }
        }
    }
}
