package io.novafoundation.nova.feature_account_impl.data.repository.datasource

import io.novafoundation.nova.common.data.secrets.v1.SecretStoreV1
import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core.model.Language
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.feature_account_api.domain.model.Account
import io.novafoundation.nova.feature_account_api.domain.model.AuthType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountAssetBalance
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountOrdering
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import kotlinx.coroutines.flow.Flow

interface AccountDataSource : SecretStoreV1 {

    fun getAuthTypeFlow(): Flow<AuthType>

    fun saveAuthType(authType: AuthType)

    fun getAuthType(): AuthType

    suspend fun savePinCode(pinCode: String)

    suspend fun getPinCode(): String?

    suspend fun saveSelectedNode(node: Node)

    suspend fun getSelectedNode(): Node?

    suspend fun anyAccountSelected(): Boolean

    suspend fun saveSelectedAccount(account: Account)

    suspend fun getSelectedMetaAccount(): MetaAccount
    fun selectedMetaAccountFlow(): Flow<MetaAccount>

    suspend fun findMetaAccount(accountId: ByteArray, chainId: ChainId): MetaAccount?

    suspend fun accountNameFor(accountId: AccountId, chainId: ChainId): String?

    suspend fun allLightMetaAccounts(): List<LightMetaAccount>

    fun allMetaAccountsFlow(): Flow<List<MetaAccount>>

    fun activeMetaAccountsFlow(): Flow<List<MetaAccount>>

    fun metaAccountsWithBalancesFlow(): Flow<List<MetaAccountAssetBalance>>

    fun metaAccountBalancesFlow(metaId: Long): Flow<List<MetaAccountAssetBalance>>

    suspend fun selectMetaAccount(metaId: Long)
    suspend fun updateAccountPositions(accountOrdering: List<MetaAccountOrdering>)

    suspend fun getSelectedLanguage(): Language
    suspend fun changeSelectedLanguage(language: Language)

    suspend fun accountExists(accountId: AccountId, chainId: ChainId): Boolean
    suspend fun getMetaAccount(metaId: Long): MetaAccount

    suspend fun getMetaAccountType(metaId: Long): LightMetaAccount.Type?

    fun metaAccountFlow(metaId: Long): Flow<MetaAccount>

    suspend fun updateMetaAccountName(metaId: Long, newName: String)
    suspend fun deleteMetaAccount(metaId: Long)

    /**
     * @return id of inserted meta account
     */
    // TODO move it to SecretsAddAccountRepository
    @Deprecated("Use SecretsAddAccountRepository instead")
    suspend fun insertMetaAccountFromSecrets(
        name: String,
        substrateCryptoType: CryptoType,
        secrets: EncodableStruct<MetaAccountSecrets>
    ): Long

    /**
     * @return id of inserted meta account
     */
    // TODO move it to SecretsAddAccountRepository
    @Deprecated("Use SecretsAddAccountRepository instead")
    suspend fun insertChainAccount(
        metaId: Long,
        chain: Chain,
        cryptoType: CryptoType,
        secrets: EncodableStruct<ChainAccountSecrets>
    )

    suspend fun hasActiveMetaAccounts(): Boolean

    fun removeDeactivatedMetaAccounts()

    suspend fun getActiveMetaAccounts(): List<MetaAccount>

    suspend fun getActiveMetaAccountsQuantity(): Int

    suspend fun getMetaAccountIdsByType(type: LightMetaAccount.Type): List<Long>
}

suspend fun AccountDataSource.getMetaAccountTypeOrThrow(metaId: Long): LightMetaAccount.Type {
    return requireNotNull(getMetaAccountType(metaId))
}
