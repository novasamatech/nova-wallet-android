package jp.co.soramitsu.feature_account_api.data.secrets

import jp.co.soramitsu.common.data.secrets.v2.AccountSecrets
import jp.co.soramitsu.common.data.secrets.v2.ChainAccountSecrets
import jp.co.soramitsu.common.data.secrets.v2.SecretStoreV2
import jp.co.soramitsu.common.data.secrets.v2.getAccountSecrets
import jp.co.soramitsu.common.data.secrets.v2.mapChainAccountSecretsToKeypair
import jp.co.soramitsu.common.data.secrets.v2.mapMetaAccountSecretsToDerivationPath
import jp.co.soramitsu.common.data.secrets.v2.mapMetaAccountSecretsToKeypair
import jp.co.soramitsu.common.utils.fold
import jp.co.soramitsu.fearless_utils.encrypt.keypair.Keypair
import jp.co.soramitsu.feature_account_api.domain.model.MetaAccount
import jp.co.soramitsu.feature_account_api.domain.model.accountIdIn
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain

suspend fun SecretStoreV2.getAccountSecrets(
    metaAccount: MetaAccount,
    chain: Chain
) : AccountSecrets {
    val accountId = metaAccount.accountIdIn(chain) ?: error("No account for chain $chain in meta account ${metaAccount.name}")

    return getAccountSecrets(metaAccount.id, accountId)
}

fun AccountSecrets.keypair(chain: Chain): Keypair {
    return fold(
        left = { mapMetaAccountSecretsToKeypair(it, ethereum = chain.isEthereumBased) },
        right = { mapChainAccountSecretsToKeypair(it) }
    )
}

fun AccountSecrets.derivationPath(chain: Chain): String? {
    return fold(
        left = { mapMetaAccountSecretsToDerivationPath(it, ethereum = chain.isEthereumBased) },
        right = { it[ChainAccountSecrets.DerivationPath] }
    )
}
