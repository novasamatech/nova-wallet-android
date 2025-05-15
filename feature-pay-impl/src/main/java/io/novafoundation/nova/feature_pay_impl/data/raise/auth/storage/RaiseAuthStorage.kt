package io.novafoundation.nova.feature_pay_impl.data.raise.auth.storage

import com.google.gson.Gson
import io.novafoundation.nova.common.data.secrets.v2.KeyPairSchema
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.utils.toStruct
import io.novafoundation.nova.feature_account_api.data.secrets.generateSr25119Keypair
import io.novafoundation.nova.feature_account_api.data.secrets.keypair
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_pay_impl.domain.common.ShopAccountSeedAccessPolicy
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.SubstrateKeypairFactory
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import io.novasama.substrate_sdk_android.scale.toHexString
import kotlinx.coroutines.runBlocking

interface RaiseAuthStorage {

    fun getChallengeKeypair(metaAccount: MetaAccount): Sr25519Keypair

    fun getJwtToken(metaAccount: MetaAccount): RaiseAuthToken?

    fun saveJwtToken(metaAccount: MetaAccount, token: RaiseAuthToken)
}

class RealRaiseAuthStorage(
    private val shopAccountSeedAccessPolicy: ShopAccountSeedAccessPolicy,
    private val encryptedPreferences: EncryptedPreferences,
    private val gson: Gson,
) : RaiseAuthStorage {

    companion object {

        private const val RAISE_AUTH_DERIVATION_PATH = "//raise//auth"

        private const val KEYPAIR_KEY = "RaiseAuthStorage.ChallengeKeypair:"
        private const val JWT_TOKEN_KEY = "RaiseAuthStorage.JwtToken:"
    }

    override fun getChallengeKeypair(metaAccount: MetaAccount): Sr25519Keypair {
        return synchronized(this) {
            if (encryptedPreferences.hasKey(getKeypairKey(metaAccount.id))) {
                getKeypairOrThrow(metaAccount)
            } else {
                generateKeypair(metaAccount).also(::saveKeypair)
            }
        }
    }

    override fun getJwtToken(metaAccount: MetaAccount): RaiseAuthToken? {
        val raw = encryptedPreferences.getDecryptedString(getTokenKey(metaAccount.id)) ?: return null
        return gson.fromJson(raw, RaiseAuthToken::class.java)
    }

    override fun saveJwtToken(metaAccount: MetaAccount, token: RaiseAuthToken) {
        val raw = gson.toJson(token)
        encryptedPreferences.putEncryptedString(getTokenKey(metaAccount.id), raw)
    }

    private fun getKeypairOrThrow(metaAccount: MetaAccount): Sr25519Keypair {
        val raw = requireNotNull(encryptedPreferences.getDecryptedString(getKeypairKey(metaAccount.id)))
        return KeyPairSchema.read(raw).toSr25519Keypair()
    }

    private fun saveKeypair(keypair: Sr25519Keypair) {
        val raw = keypair.toStruct().toHexString()
        encryptedPreferences.putEncryptedString(KEYPAIR_KEY, raw)
    }

    private fun generateKeypair(metaAccount: MetaAccount): Sr25519Keypair {
        return runBlocking {
            val seed = shopAccountSeedAccessPolicy.getSeedFor(metaAccount) ?: error("No seed found for meta account ${metaAccount.name}")

            SubstrateKeypairFactory.generateSr25119Keypair(seed, RAISE_AUTH_DERIVATION_PATH)
        }
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
