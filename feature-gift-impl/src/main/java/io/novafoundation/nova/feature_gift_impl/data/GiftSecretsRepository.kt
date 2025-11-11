package io.novafoundation.nova.feature_gift_impl.data

import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val GIFT_SECRETS = "GIFT_SECRETS"

interface GiftSecretsRepository {

    suspend fun putGiftAccountSeed(accountId: ByteArray, seed: ByteArray)

    suspend fun getGiftAccountSeed(accountId: ByteArray): ByteArray?
}

class RealGiftSecretsRepository(
    private val encryptedPreferences: EncryptedPreferences,
) : GiftSecretsRepository {

    override suspend fun putGiftAccountSeed(accountId: ByteArray, seed: ByteArray) = withContext(Dispatchers.IO) {
        encryptedPreferences.putEncryptedString(giftAccountKey(accountId), seed.toHexString())
    }

    override suspend fun getGiftAccountSeed(accountId: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        encryptedPreferences.getDecryptedString(giftAccountKey(accountId))?.fromHex()
    }

    private fun giftAccountKey(accountId: ByteArray) = "${accountId.toHexString()}:$GIFT_SECRETS"
}
