package io.novafoundation.nova.feature_push_notifications.data.data.settings

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import io.novafoundation.nova.feature_push_notifications.data.data.PushTokenCache
import kotlinx.coroutines.tasks.await

private const val COLLECTION_NAME = "accounts"

class RemotePushSettingsProvider(
    private val pushTokenCache: PushTokenCache
) : PushSettingsProvider {

    private val firestore = Firebase.firestore

    override suspend fun getWalletSettings(): PushWalletSettings? {
        val token = pushTokenCache.getPushToken()
            ?: throw IllegalStateException("No token found")

        return firestore.collection(COLLECTION_NAME)
            .document(token)
            .get()
            .await()
            .toObject(PushWalletSettings::class.java)
    }

    override suspend fun updateWalletSettings(pushWalletSettings: PushWalletSettings) {
        firestore.collection(COLLECTION_NAME)
            .document(pushWalletSettings.pushToken)
            .set(pushWalletSettings)
            .await()
    }
}
