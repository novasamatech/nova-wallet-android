package io.novafoundation.nova.feature_account_api.data.secrets

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.getChainAccountKeypair
import io.novafoundation.nova.common.data.secrets.v2.getMetaAccountKeypair
import jp.co.soramitsu.fearless_utils.encrypt.Signer
import jp.co.soramitsu.fearless_utils.encrypt.keypair.Keypair
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.multiChainEncryptionIn
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

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

/**
 * @return secrets for the given [accountId] in [metaAccount] respecting configuration of [chain] (is ethereum or not).
 */
suspend fun SecretStoreV2.getKeypair(
    metaAccount: MetaAccount,
    chain: Chain,
    accountId: AccountId
): Keypair {
    return if (hasChainSecrets(metaAccount.id, accountId)) {
        getChainAccountKeypair(metaAccount.id, accountId)
    } else {
        getMetaAccountKeypair(metaAccount.id, chain.isEthereumBased)
    }
}

/**
 * @return chain account secrets if there is a chain account in  [chain]. Meta account keypair otherwise.
 */
suspend fun SecretStoreV2.getKeypair(
    metaAccount: MetaAccount,
    chain: Chain,
): Keypair {
    val accountId = requireNotNull(metaAccount.accountIdIn(chain)) {
        "No account in ${metaAccount.name} for ${chain.name}"
    }

    return getKeypair(metaAccount, chain, accountId)
}
