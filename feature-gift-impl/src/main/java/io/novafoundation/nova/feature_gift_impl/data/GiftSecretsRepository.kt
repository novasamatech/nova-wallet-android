package io.novafoundation.nova.feature_gift_impl.data

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import io.novasama.substrate_sdk_android.scale.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val GIFT_SECRETS = "GIFT_SECRETS"

interface GiftSecretsRepository {

    suspend fun putGiftAccountSecrets(accountId: ByteArray, secrets: EncodableStruct<ChainAccountSecrets>)

    suspend fun getGiftAccountSecrets(accountId: ByteArray): EncodableStruct<ChainAccountSecrets>?
}

class RealGiftSecretsRepository(
    private val encryptedPreferences: EncryptedPreferences,
) : GiftSecretsRepository {

    override suspend fun putGiftAccountSecrets(accountId: ByteArray, secrets: EncodableStruct<ChainAccountSecrets>) = withContext(Dispatchers.IO) {
        encryptedPreferences.putEncryptedString(giftAccountKey(accountId), secrets.toHexString())
    }

    override suspend fun getGiftAccountSecrets(accountId: ByteArray): EncodableStruct<ChainAccountSecrets>? = withContext(Dispatchers.IO) {
        encryptedPreferences.getDecryptedString(giftAccountKey(accountId))?.let(ChainAccountSecrets::read)
    }

    private fun giftAccountKey(accountId: ByteArray) = "${accountId.toHexString()}:$GIFT_SECRETS"
}
