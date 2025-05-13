package io.novafoundation.nova.feature_pay_impl.data.raise.auth.network

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_pay_impl.BuildConfig
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.storage.RaiseAuthStorage
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.storage.RaiseAuthToken
import io.novafoundation.nova.merlin.MerlinCrypto
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import okhttp3.Credentials

interface RaiseAuthRepository {

    fun signChallenge(metaAccount: MetaAccount, challenge: ByteArray): ByteArray

    fun getChallengePublicKey(metaAccount: MetaAccount): ByteArray

    fun getJwtToken(metaAccount: MetaAccount): RaiseAuthToken?

    fun saveJwtToken(metaAccount: MetaAccount, token: RaiseAuthToken)

    fun basicAuthCredentials(): String
}

class RealRaiseAuthRepository(
    private val authStorage: RaiseAuthStorage,
    private val clientId: String = BuildConfig.RAISE_CLIENT_ID,
    private val secret: String = BuildConfig.RAISE_SECRET,
) : RaiseAuthRepository {

    override fun signChallenge(metaAccount: MetaAccount, challenge: ByteArray): ByteArray {
        val keypair = authStorage.getChallengeKeypair(metaAccount)
        return keypair.signRaiseTranscript(challenge)
    }

    override fun getChallengePublicKey(metaAccount: MetaAccount): ByteArray {
        val keypair = authStorage.getChallengeKeypair(metaAccount)
        return keypair.publicKey
    }

    override fun getJwtToken(metaAccount: MetaAccount): RaiseAuthToken? {
        return authStorage.getJwtToken(metaAccount)
    }

    override fun saveJwtToken(metaAccount: MetaAccount, token: RaiseAuthToken) {
        authStorage.saveJwtToken(metaAccount, token)
    }

    override fun basicAuthCredentials(): String {
        return Credentials.basic(clientId, secret)
    }

    private fun Sr25519Keypair.signRaiseTranscript(challenge: ByteArray): ByteArray {
        return MerlinCrypto.generateTranscript(publicKey, privateKey + nonce, challenge)
    }
}
