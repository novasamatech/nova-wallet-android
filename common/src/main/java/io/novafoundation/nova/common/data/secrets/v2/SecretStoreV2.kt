package io.novafoundation.nova.common.data.secrets.v2

import io.novafoundation.nova.common.data.secrets.v1.Keypair
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.utils.Union
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.common.utils.fold
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import io.novasama.substrate_sdk_android.scale.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ACCESS_SECRETS = "ACCESS_SECRETS"
private const val ADDITIONAL_KNOWN_KEYS = "ADDITIONAL_KNOWN_KEYS"
private const val ADDITIONAL_KNOWN_KEYS_DELIMITER = ","

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
    suspend fun hasChainSecrets(metaId: Long, accountId: ByteArray) = withContext(Dispatchers.IO) {
        encryptedPreferences.hasKey(chainAccountKey(metaId, accountId, ACCESS_SECRETS))
    }

    suspend fun getAdditionalMetaAccountSecret(metaId: Long, secretName: String) = withContext(Dispatchers.IO) {
        encryptedPreferences.getDecryptedString(metaAccountAdditionalKey(metaId, secretName))
    }

    suspend fun putAdditionalMetaAccountSecret(metaId: Long, secretName: String, value: String) = withContext(Dispatchers.IO) {
        val key = metaAccountAdditionalKey(metaId, secretName)

        encryptedPreferences.putEncryptedString(key, value)
        putAdditionalSecretKeyToKnown(metaId, secretName)
    }

    suspend fun clearMetaAccountSecrets(metaId: Long, chainAccountIds: List<AccountId>) = withContext(Dispatchers.IO) {
        chainAccountIds.map { chainAccountKey(metaId, it, ACCESS_SECRETS) }
            .onEach(encryptedPreferences::removeKey)

        encryptedPreferences.removeKey(metaAccountKey(metaId, ACCESS_SECRETS))
        clearAdditionalSecrets(metaId)
    }

    suspend fun clearChainAccountsSecrets(metaId: Long, chainAccountIds: List<AccountId>) = withContext(Dispatchers.IO) {
        chainAccountIds.map { chainAccountKey(metaId, it, ACCESS_SECRETS) }
            .onEach(encryptedPreferences::removeKey)
    }

    suspend fun allKnownAdditionalSecrets(metaId: Long): Map<String, String> = withContext(Dispatchers.IO) {
        allKnownAdditionalSecretKeys(metaId).associateWith { secretKey ->
            getAdditionalMetaAccountSecret(metaId, secretKey)
        }.filterNotNull()
    }

    suspend fun allKnownAdditionalSecretKeys(metaId: Long): Set<String> = withContext(Dispatchers.IO) {
        val metaAccountAdditionalKnownKey = metaAccountAdditionalKnownKey(metaId)

        encryptedPreferences.getDecryptedString(metaAccountAdditionalKnownKey)
            ?.split(ADDITIONAL_KNOWN_KEYS_DELIMITER)?.toSet()
            ?: emptySet()
    }

    private suspend fun clearAdditionalSecrets(metaId: Long) {
        val allKnown = allKnownAdditionalSecretKeys(metaId)

        allKnown.forEach { secretName ->
            encryptedPreferences.removeKey(metaAccountAdditionalKey(metaId, secretName))
        }

        encryptedPreferences.removeKey(metaAccountAdditionalKnownKey(metaId))
    }

    private suspend fun putAdditionalSecretKeyToKnown(metaId: Long, secretName: String) {
        require(validAdditionalKeyName(secretName))

        val currentKnownKeys = allKnownAdditionalSecretKeys(metaId)

        val updatedKnownKeys = currentKnownKeys + secretName
        val encodedKnownKeys = updatedKnownKeys.joinToString(ADDITIONAL_KNOWN_KEYS_DELIMITER)

        encryptedPreferences.putEncryptedString(metaAccountAdditionalKnownKey(metaId), encodedKnownKeys)
    }

    private fun chainAccountKey(metaId: Long, accountId: ByteArray, secretName: String) = "$metaId:${accountId.toHexString()}:$secretName"

    private fun metaAccountKey(metaId: Long, secretName: String) = "$metaId:$secretName"

    private fun metaAccountAdditionalKnownKey(metaId: Long) = "$metaId:$ADDITIONAL_KNOWN_KEYS"
    private fun metaAccountAdditionalKey(metaId: Long, secretName: String) = "$metaId:$ADDITIONAL_KNOWN_KEYS:$secretName"

    private fun validAdditionalKeyName(secretName: String) = ADDITIONAL_KNOWN_KEYS_DELIMITER !in secretName
}

suspend fun SecretStoreV2.getAccountSecrets(
    metaId: Long,
    accountId: ByteArray,
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
    left = { it[MetaAccountSecrets.SubstrateSeed] },
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

val AccountSecrets.isMetaAccountSecrets
    get() = isLeft

val AccountSecrets.isChainAccountSecrets
    get() = isRight

suspend fun SecretStoreV2.getMetaAccountKeypair(
    metaId: Long,
    isEthereum: Boolean,
): Keypair = withContext(Dispatchers.Default) {
    val secrets = getMetaAccountSecrets(metaId) ?: noMetaSecrets(metaId)

    mapMetaAccountSecretsToKeypair(secrets, isEthereum)
}

fun mapMetaAccountSecretsToKeypair(
    secrets: EncodableStruct<MetaAccountSecrets>,
    ethereum: Boolean,
): Keypair {
    val keypairStruct = if (ethereum) {
        secrets[MetaAccountSecrets.EthereumKeypair] ?: noEthereumSecret()
    } else {
        secrets[MetaAccountSecrets.SubstrateKeypair]
    }

    return mapKeypairStructToKeypair(keypairStruct)
}

fun mapMetaAccountSecretsToDerivationPath(
    secrets: EncodableStruct<MetaAccountSecrets>,
    ethereum: Boolean,
): String? {
    return if (ethereum) {
        secrets[MetaAccountSecrets.EthereumDerivationPath]
    } else {
        secrets[MetaAccountSecrets.SubstrateDerivationPath]
    }
}

fun mapChainAccountSecretsToKeypair(
    secrets: EncodableStruct<ChainAccountSecrets>
) = mapKeypairStructToKeypair(secrets[ChainAccountSecrets.Keypair])

private fun noMetaSecrets(metaId: Long): Nothing = error("No secrets found for meta account $metaId")

private fun noChainSecrets(metaId: Long, accountId: ByteArray): Nothing {
    error("No secrets found for meta account $metaId for account ${accountId.toHexString()}")
}

private fun noEthereumSecret(): Nothing = error("No ethereum keypair found")

fun mapKeypairStructToKeypair(struct: EncodableStruct<KeyPairSchema>): Keypair {
    return Keypair(
        publicKey = struct[KeyPairSchema.PublicKey],
        privateKey = struct[KeyPairSchema.PrivateKey],
        nonce = struct[KeyPairSchema.Nonce]
    )
}
