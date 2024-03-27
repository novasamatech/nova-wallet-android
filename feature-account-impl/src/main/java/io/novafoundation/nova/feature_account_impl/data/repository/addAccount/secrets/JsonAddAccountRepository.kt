package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets

import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.ImportJsonMetaData
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedDecoder
import io.novasama.substrate_sdk_android.encrypt.model.NetworkTypeIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JsonAddAccountRepository(
    private val accountDataSource: AccountDataSource,
    private val accountSecretsFactory: AccountSecretsFactory,
    private val jsonSeedDecoder: JsonSeedDecoder,
    private val chainRegistry: ChainRegistry,
    proxySyncService: ProxySyncService,
    metaAccountChangesEventBus: MetaAccountChangesEventBus
) : SecretsAddAccountRepository<JsonAddAccountRepository.Payload>(
    accountDataSource,
    accountSecretsFactory,
    chainRegistry,
    proxySyncService,
    metaAccountChangesEventBus
) {

    class Payload(
        val json: String,
        val password: String,
        val addAccountType: AddAccountType
    )

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        return addSecretsAccount(
            derivationPaths = AdvancedEncryption.DerivationPaths.empty(),
            addAccountType = payload.addAccountType,
            accountSource = AccountSecretsFactory.AccountSource.Json(payload.json, payload.password)
        )
    }

    suspend fun extractJsonMetadata(importJson: String): ImportJsonMetaData = withContext(Dispatchers.Default) {
        val importAccountMeta = jsonSeedDecoder.extractImportMetaData(importJson)

        with(importAccountMeta) {
            val chainId = (networkTypeIdentifier as? NetworkTypeIdentifier.Genesis)?.genesis?.removeHexPrefix()
            val cryptoType = mapEncryptionToCryptoType(encryption.encryptionType)

            ImportJsonMetaData(name, chainId, cryptoType)
        }
    }
}
