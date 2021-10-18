package jp.co.soramitsu.common.data.secrets.v2

import jp.co.soramitsu.common.data.secrets.v1.Keypair
import jp.co.soramitsu.common.data.storage.encrypt.EncryptedPreferences
import jp.co.soramitsu.common.utils.Union
import jp.co.soramitsu.common.utils.fold
import jp.co.soramitsu.fearless_utils.encrypt.keypair.Keypair
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.scale.EncodableStruct
import jp.co.soramitsu.fearless_utils.scale.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ACCESS_SECRETS = "ACCESS_SECRETS"

typealias AccountSecrets = Union<EncodableStruct<MetaAccountSecrets>, EncodableStruct<ChainAccountSecrets>>

class SecretStoreV2(
    private val encryptedPreferences: EncryptedPreferences,
) {

    suspend fun putMetaAccountSecrets(metaId: Long, secrets: EncodableStruct<MetaAccountSecrets>) = withContext(Dispatchers.IO) {
        encryptedPreferences.putEncryptedString(metaAccountKey(metaId, ACCESS_SECRETS), secrets.toHexString())
    }

    suspend fun getMetaAccountSecrets(metaId: Long): EncodableStruct<MetaAccountSecrets>? = withContext(Dispatchers.IO) {
        encryptedPreferences.getDecryptedString(metaAccountKey(metaId, ACCESS_SECRETS))?.let(MetaAccountSecrets::read)
    }

    suspend fun putChainAccountSecrets(metaId: Long, accountId: ByteArray, secrets: EncodableStruct<ChainAccountSecrets>) = withContext(Dispatchers.IO) {
        encryptedPreferences.putEncryptedString(chainAccountKey(metaId, accountId, ACCESS_SECRETS), secrets.toHexString())
    }

    suspend fun getChainAccountSecrets(metaId: Long, accountId: ByteArray): EncodableStruct<ChainAccountSecrets>? = withContext(Dispatchers.IO) {
        encryptedPreferences.getDecryptedString(chainAccountKey(metaId, accountId, ACCESS_SECRETS))?.let(ChainAccountSecrets::read)
    }

    suspend fun hasChainSecrets(metaId: Long, accountId: ByteArray) = withContext(Dispatchers.Default) {
        encryptedPreferences.hasKey(chainAccountKey(metaId, accountId, ACCESS_SECRETS))
    }

    suspend fun clearSecrets(metaId: Long, chainAccountIds: List<AccountId>) = withContext(Dispatchers.Default) {
        chainAccountIds.map { chainAccountKey(metaId, it, ACCESS_SECRETS) }
            .onEach(encryptedPreferences::removeKey)

        encryptedPreferences.removeKey(metaAccountKey(metaId, ACCESS_SECRETS))
    }

    private fun chainAccountKey(metaId: Long, accountId: ByteArray, secretName: String) = "$metaId:${accountId.toHexString()}:$secretName"

    private fun metaAccountKey(metaId: Long, secretName: String) = "$metaId:$secretName"
}

suspend fun SecretStoreV2.getAccountSecrets(
    metaId: Long,
    accountId: ByteArray
): AccountSecrets {
    return if (hasChainSecrets(metaId, accountId)) {
        Union.right(
            getChainAccountSecrets(metaId, accountId) ?: noChainSecrets(metaId, accountId)
        )
    } else {
        Union.left(
            getMetaAccountSecrets(metaId) ?: noMetaSecrets(metaId)
        )
    }
}

fun AccountSecrets.seed(): ByteArray? = fold(
    left = { it[MetaAccountSecrets.Seed] },
    right = { it[ChainAccountSecrets.Seed] }
)

fun AccountSecrets.entropy(): ByteArray? = fold(
    left = { it[MetaAccountSecrets.Entropy] },
    right = { it[ChainAccountSecrets.Entropy] }
)

suspend fun SecretStoreV2.getChainAccountKeypair(
    metaId: Long,
    accountId: ByteArray,
): Keypair = withContext(Dispatchers.Default) {
    val secrets = getChainAccountSecrets(metaId, accountId) ?: noChainSecrets(metaId, accountId)

    val keypairStruct = secrets[ChainAccountSecrets.Keypair]

    mapKeypairStructToKeypair(keypairStruct)
}

suspend fun SecretStoreV2.getMetaAccountKeypair(
    metaId: Long,
    isEthereum: Boolean
): Keypair = withContext(Dispatchers.Default) {
    val secrets = getMetaAccountSecrets(metaId) ?: noMetaSecrets(metaId)

    val keypairStruct = if (isEthereum) {
        secrets[MetaAccountSecrets.EthereumKeypair] ?: error("No ethereum keypair found for meta account $metaId")
    } else {
        secrets[MetaAccountSecrets.SubstrateKeypair]
    }

    mapKeypairStructToKeypair(keypairStruct)
}

private fun noMetaSecrets(metaId: Long): Nothing = error("No secrets found for meta account $metaId")

private fun noChainSecrets(metaId: Long, accountId: ByteArray): Nothing {
    error("No secrets found for meta account $metaId for account ${accountId.toHexString()}")
}

fun mapKeypairStructToKeypair(struct: EncodableStruct<KeyPairSchema>): Keypair {
    return Keypair(
        publicKey = struct[KeyPairSchema.PublicKey],
        privateKey = struct[KeyPairSchema.PrivateKey],
        nonce = struct[KeyPairSchema.Nonce]
    )
}
