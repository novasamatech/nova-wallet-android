package io.novafoundation.nova.feature_account_impl.domain.account.export

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.derivationPath
import io.novafoundation.nova.common.data.secrets.v2.entropy
import io.novafoundation.nova.common.data.secrets.v2.ethereumDerivationPath
import io.novafoundation.nova.common.data.secrets.v2.ethereumKeypair
import io.novafoundation.nova.common.data.secrets.v2.keypair
import io.novafoundation.nova.common.data.secrets.v2.privateKey
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.data.secrets.v2.substrateDerivationPath
import io.novafoundation.nova.common.data.secrets.v2.substrateKeypair
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CommonExportSecretsInteractor {

    suspend fun getMetaAccountMnemonic(metaAccount: MetaAccount): Mnemonic?

    suspend fun getChainAccountMnemonic(metaAccount: MetaAccount, chain: Chain): Mnemonic?

    suspend fun getMetaAccountSeed(metaAccount: MetaAccount): String?

    suspend fun getMetaAccountEthereumPrivateKey(metaAccount: MetaAccount): String?

    suspend fun getChainAccountPrivateKey(metaAccount: MetaAccount, chain: Chain): String?

    suspend fun getChainAccountSeed(metaAccount: MetaAccount, chain: Chain): String?

    suspend fun getDerivationPath(metaAccount: MetaAccount, ethereum: Boolean): String?

    suspend fun getDerivationPath(metaAccount: MetaAccount, chain: Chain): String?

    suspend fun hasEthereumSecrets(metaAccount: MetaAccount): Boolean

    suspend fun hasSubstrateSecrets(metaAccount: MetaAccount): Boolean
}

class RealCommonExportSecretsInteractor(
    private val secretStoreV2: SecretStoreV2
) : CommonExportSecretsInteractor {

    override suspend fun getMetaAccountMnemonic(
        metaAccount: MetaAccount
    ): Mnemonic? = withContext(Dispatchers.Default) {
        val secrets = secretStoreV2.getMetaAccountSecrets(metaAccount.id)

        secrets?.entropy?.let { MnemonicCreator.fromEntropy(it) }
    }

    override suspend fun getChainAccountMnemonic(
        metaAccount: MetaAccount,
        chain: Chain,
    ): Mnemonic? = withContext(Dispatchers.Default) {
        val chainAccountId = metaAccount.requireAccountIdIn(chain)
        val secrets = secretStoreV2.getChainAccountSecrets(metaAccount.id, chainAccountId)

        secrets?.entropy?.let { MnemonicCreator.fromEntropy(it) }
    }

    override suspend fun getMetaAccountSeed(metaAccount: MetaAccount): String? = withContext(Dispatchers.Default) {
        val secrets = secretStoreV2.getMetaAccountSecrets(metaAccount.id)

        secrets?.seed?.toHexString(withPrefix = true)
    }

    override suspend fun getMetaAccountEthereumPrivateKey(metaAccount: MetaAccount): String? = withContext(Dispatchers.Default) {
        val secrets = secretStoreV2.getMetaAccountSecrets(metaAccount.id)

        secrets?.ethereumKeypair?.privateKey?.toHexString(withPrefix = true)
    }

    override suspend fun getChainAccountPrivateKey(metaAccount: MetaAccount, chain: Chain): String? = withContext(Dispatchers.Default) {
        val secrets = secretStoreV2.getChainAccountSecrets(metaAccount.id, metaAccount.requireAccountIdIn(chain))

        secrets?.keypair?.privateKey?.toHexString(withPrefix = true)
    }

    override suspend fun getChainAccountSeed(metaAccount: MetaAccount, chain: Chain): String? = withContext(Dispatchers.Default) {
        val secrets = secretStoreV2.getChainAccountSecrets(metaAccount.id, metaAccount.requireAccountIdIn(chain))

        secrets?.seed?.toHexString(withPrefix = true)
    }

    override suspend fun getDerivationPath(metaAccount: MetaAccount, ethereum: Boolean): String? = withContext(Dispatchers.Default) {
        val secrets = secretStoreV2.getMetaAccountSecrets(metaAccount.id)

        if (ethereum) {
            secrets?.ethereumDerivationPath
        } else {
            secrets?.substrateDerivationPath
        }
    }

    override suspend fun getDerivationPath(metaAccount: MetaAccount, chain: Chain): String? = withContext(Dispatchers.Default) {
        val secrets = secretStoreV2.getChainAccountSecrets(metaAccount.id, metaAccount.requireAccountIdIn(chain))

        secrets?.derivationPath
    }

    override suspend fun hasEthereumSecrets(metaAccount: MetaAccount): Boolean {
        return secretStoreV2.getMetaAccountSecrets(metaAccount.id)?.ethereumKeypair != null
    }

    override suspend fun hasSubstrateSecrets(metaAccount: MetaAccount): Boolean {
        return secretStoreV2.getMetaAccountSecrets(metaAccount.id)?.substrateKeypair != null
    }
}
