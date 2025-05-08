package io.novafoundation.nova.feature_pay_impl.data.raise.auth.network

import io.novafoundation.nova.feature_pay_impl.BuildConfig
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.storage.RaiseAuthStorage
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.storage.RaiseAuthToken
import io.novafoundation.nova.merlin.MerlinCrypto
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import okhttp3.Credentials

interface RaiseAuthRepository {

    fun signChallenge(metaId: Long, challenge: ByteArray): ByteArray

    fun getChallengePublicKey(metaId: Long): ByteArray

    fun getJwtToken(metaId: Long): RaiseAuthToken?

    fun saveJwtToken(metaId: Long, token: RaiseAuthToken)

    fun basicAuthCredentials(): String
}

class RealRaiseAuthRepository(
    private val authStorage: RaiseAuthStorage,
    private val clientId: String = BuildConfig.RAISE_CLIENT_ID,
    private val secret: String = BuildConfig.RAISE_SECRET,
) : RaiseAuthRepository {

    override fun signChallenge(metaId: Long, challenge: ByteArray): ByteArray {
        val keypair = authStorage.getChallengeKeypair(metaId)
        return keypair.signRaiseTranscript(challenge)
    }

    override fun getChallengePublicKey(metaId: Long): ByteArray {
        val keypair = authStorage.getChallengeKeypair(metaId)
        return keypair.publicKey
    }

    override fun getJwtToken(metaId: Long): RaiseAuthToken? {
        return authStorage.getJwtToken(metaId)
    }

    override fun saveJwtToken(metaId: Long, token: RaiseAuthToken) {
        authStorage.saveJwtToken(metaId, token)
    }

    override fun basicAuthCredentials(): String {
        return Credentials.basic(clientId, secret)
    }

    private fun Sr25519Keypair.signRaiseTranscript(challenge: ByteArray): ByteArray {
        return MerlinCrypto.generateTranscript(publicKey, privateKey + nonce, challenge)
    }
}
