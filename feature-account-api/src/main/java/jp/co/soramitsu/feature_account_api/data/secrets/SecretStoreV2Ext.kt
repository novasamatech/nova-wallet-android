package jp.co.soramitsu.feature_account_api.data.secrets

import jp.co.soramitsu.common.data.secrets.v2.SecretStoreV2
import jp.co.soramitsu.common.data.secrets.v2.getChainAccountKeypair
import jp.co.soramitsu.common.data.secrets.v2.getMetaAccountKeypair
import jp.co.soramitsu.fearless_utils.encrypt.Signer
import jp.co.soramitsu.fearless_utils.encrypt.keypair.Keypair
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.feature_account_api.domain.model.MetaAccount
import jp.co.soramitsu.feature_account_api.domain.model.accountIdIn
import jp.co.soramitsu.feature_account_api.domain.model.multiChainEncryptionIn
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain

suspend fun SecretStoreV2.sign(
    metaAccount: MetaAccount,
    chain: Chain,
    message: String,
) = sign(metaAccount, chain, message.encodeToByteArray()).toHexString(withPrefix = true)

suspend fun SecretStoreV2.sign(
    metaAccount: MetaAccount,
    chain: Chain,
    message: ByteArray,
) = Signer.sign(
    multiChainEncryption = metaAccount.multiChainEncryptionIn(chain),
    message = message,
    keypair = getKeypair(metaAccount, chain),
).signature

suspend fun SecretStoreV2.getKeypair(
    metaAccount: MetaAccount,
    chain: Chain,
): Keypair {
    val accountId = requireNotNull(metaAccount.accountIdIn(chain)) {
        "No account in ${metaAccount.name} for ${chain.name}"
    }

    return if (hasChainSecrets(metaAccount.id, accountId)) {
        getChainAccountKeypair(metaAccount.id, accountId)
    } else {
        getMetaAccountKeypair(metaAccount.id, chain.isEthereumBased)
    }
}
