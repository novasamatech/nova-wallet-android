package io.novafoundation.nova.feature_ledger_impl.data.repository

import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.MetaAccountLocal
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface LedgerRepository {

    suspend fun insertLedgerMetaAccount(
        name: String,
        ledgerChainAccounts: Map<ChainId, LedgerSubstrateAccount>
    ): Long

    suspend fun getChainAccountDerivationPath(
        metaId: Long,
        chainId: ChainId /* = kotlin.String */
    ): String
}

private const val LEDGER_DERIVATION_PATH_KEY = "LedgerChainAccount.derivationPath"

class RealLedgerRepository(
    private val metaAccountDao: MetaAccountDao,
    private val chainRegistry: ChainRegistry,
    private val secretStoreV2: SecretStoreV2,
) : LedgerRepository {

    override suspend fun insertLedgerMetaAccount(
        name: String,
        ledgerChainAccounts: Map<ChainId, LedgerSubstrateAccount>
    ): Long {
        val metaAccount = MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = null,
            ethereumPublicKey = null,
            ethereumAddress = null,
            name = name,
            isSelected = false,
            position = metaAccountDao.nextAccountPosition(),
            type = MetaAccountLocal.Type.LEDGER
        )

        val metaId = metaAccountDao.insertMetaAndChainAccounts(metaAccount) { metaId ->
            ledgerChainAccounts.map { (chainId, account) ->
                val chain = chainRegistry.getChain(chainId)

                ChainAccountLocal(
                    metaId = metaId,
                    chainId = chainId,
                    publicKey = account.publicKey,
                    accountId = chain.accountIdOf(account.publicKey),
                    cryptoType = mapEncryptionToCryptoType(account.encryptionType)
                )
            }
        }

        ledgerChainAccounts.onEach { (chainId, ledgerAccount) ->
            val derivationPathKey = derivationPathSecretKey(chainId)
            secretStoreV2.putAdditionalMetaAccountSecret(metaId, derivationPathKey, ledgerAccount.derivationPath)
        }

        return metaId
    }

    override suspend fun getChainAccountDerivationPath(metaId: Long, chainId: ChainId): String {
        val key = derivationPathSecretKey(chainId)

        return secretStoreV2.getAdditionalMetaAccountSecret(metaId, key)
            ?: throw IllegalStateException("Cannot find Ledger derivation path for chain $chainId in meta account $metaId")
    }

    private fun derivationPathSecretKey(chainId: ChainId): String {
        return "$LEDGER_DERIVATION_PATH_KEY.$chainId"
    }
}
