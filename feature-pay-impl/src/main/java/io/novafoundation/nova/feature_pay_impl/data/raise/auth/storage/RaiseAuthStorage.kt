package io.novafoundation.nova.feature_pay_impl.data.raise.auth.storage

import com.google.gson.Gson
import io.novafoundation.nova.common.data.secrets.v2.KeyPairSchema
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.data.secrets.v2.substrateKeypair
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.utils.toStruct
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.secrets.AccountSecretsFactory
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import io.novasama.substrate_sdk_android.scale.toHexString
import kotlinx.coroutines.runBlocking

interface RaiseAuthStorage {

    fun getChallengeKeypair(metaId: Long): Sr25519Keypair

    fun getJwtToken(metaId: Long): RaiseAuthToken?

    fun saveJwtToken(metaId: Long, token: RaiseAuthToken)
}

class RealRaiseAuthStorage(
    private val accountSecretsFactory: AccountSecretsFactory,
    private val secretStoreV2: SecretStoreV2,
    private val encryptedPreferences: EncryptedPreferences,
    private val gson: Gson,
) : RaiseAuthStorage {

    companion object {

        private const val RAISE_AUTH_DERIVATION_PATH = "//raise//auth"

        private const val KEYPAIR_KEY = "RaiseAuthStorage.ChallengeKeypair:"
        private const val JWT_TOKEN_KEY = "RaiseAuthStorage.JwtToken:"
    }

    override fun getChallengeKeypair(metaId: Long): Sr25519Keypair {
        return synchronized(this) {
            if (encryptedPreferences.hasKey(getKeypairKey(metaId))) {
                getKeypairOrThrow(metaId)
            } else {
                generateKeypair(metaId).also(::saveKeypair)
            }
        }
    }

    override fun getJwtToken(metaId: Long): RaiseAuthToken? {
        val raw = encryptedPreferences.getDecryptedString(getTokenKey(metaId)) ?: return null
        return gson.fromJson(raw, RaiseAuthToken::class.java)
    }

    override fun saveJwtToken(metaId: Long, token: RaiseAuthToken) {
        val raw = gson.toJson(token)
        encryptedPreferences.putEncryptedString(getTokenKey(metaId), raw)
    }

    private fun getKeypairOrThrow(metaId: Long): Sr25519Keypair {
        val raw = requireNotNull(encryptedPreferences.getDecryptedString(getKeypairKey(metaId)))
        return KeyPairSchema.read(raw).toSr25519Keypair()
    }

    private fun saveKeypair(keypair: Sr25519Keypair) {
        val raw = keypair.toStruct().toHexString()
        encryptedPreferences.putEncryptedString(KEYPAIR_KEY, raw)
    }

    private fun generateKeypair(metaId: Long): Sr25519Keypair {
        return runBlocking {
            val secrets = secretStoreV2.getMetaAccountSecrets(metaId) ?: throw IllegalStateException()

            secrets.toRaiseValidKeypair()
        }
    }

    private suspend fun EncodableStruct<MetaAccountSecrets>.toRaiseValidKeypair(): Sr25519Keypair {
        val seed = seed ?: throw IllegalStateException()

        val raiseValidSecrets = accountSecretsFactory.metaAccountSecrets(
            substrateDerivationPath = RAISE_AUTH_DERIVATION_PATH,
            ethereumDerivationPath = null,
            accountSource = AccountSecretsFactory.AccountSource.Seed(CryptoType.SR25519, seed.toHexString())
        )

        return raiseValidSecrets.secrets.substrateKeypair.toSr25519Keypair()
    }

    private fun getTokenKey(metaId: Long) = JWT_TOKEN_KEY + metaId

    private fun getKeypairKey(metaId: Long) = KEYPAIR_KEY + metaId
}

fun EncodableStruct<KeyPairSchema>.toSr25519Keypair(): Sr25519Keypair {
    return Sr25519Keypair(
        publicKey = get(KeyPairSchema.PublicKey),
        privateKey = get(KeyPairSchema.PrivateKey),
        nonce = requireNotNull(get(KeyPairSchema.Nonce))
    )
}
