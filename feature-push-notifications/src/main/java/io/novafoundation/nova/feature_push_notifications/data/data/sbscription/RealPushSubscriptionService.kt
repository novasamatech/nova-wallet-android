package io.novafoundation.nova.feature_push_notifications.data.data.sbscription

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging
import io.novafoundation.nova.common.utils.formatting.formatDateISO_8601
import io.novafoundation.nova.feature_push_notifications.data.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettings
import java.util.*
import kotlinx.coroutines.tasks.await

private const val COLLECTION_NAME = "accounts"

class RealPushSubscriptionService(
    private val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider
) : PushSubscriptionService {

    override suspend fun handleSubscription(token: String, pushSettings: PushSettings) {
        if (!googleApiAvailabilityProvider.isAvailable()) return

        handleSubscription(pushSettings.appMajorUpdates, "appMajorUpdates")
        handleSubscription(pushSettings.appCriticalUpdates, "appCriticalUpdates")
        handleSubscription(pushSettings.chainReferendums, "chainReferendums")

        sendWaletSettingsToFirestore(token, pushSettings)
    }

    private suspend fun sendWaletSettingsToFirestore(token: String, pushSettings: PushSettings) {
        Firebase.firestore.collection(COLLECTION_NAME)
            .document(token)
            .set(mapToFirestorePushSettings(token, Date(), pushSettings.wallets))
            .await()
    }


    private suspend fun handleSubscription(subscribe: Boolean, topic: String) {
        if (subscribe) {
            subscribeToTopic(topic)
        } else {
            unsubscribeFromTopic(topic)
        }
    }

    private suspend fun subscribeToTopic(topic: String) {
        Firebase.messaging.subscribeToTopic(topic)
            .await()
    }

    private suspend fun unsubscribeFromTopic(topic: String) {
        Firebase.messaging.unsubscribeFromTopic(topic)
            .await()
    }

    private fun mapToFirestorePushSettings(token: String, date: Date, walletsSettings: List<PushSettings.Wallet>): Map<String, Any> {
        return mapOf(
            "pushToken" to token,
            "updatedAt" to formatDateISO_8601(date),
            "wallets" to walletsSettings.map { mapToFirestoreWallet(it) }
        )
    }

    private fun mapToFirestoreWallet(wallet: PushSettings.Wallet): Map<String, Any> {
        return mapOf(
            "baseEthereumAccount" to wallet.baseEthereumAccount,
            "baseSubstrateAccount" to wallet.baseSubstrateAccount,
            "chainAccounts" to wallet.chainAccounts,
            "notifications" to mapOf(
                "stakingReward" to mapOf("type" to wallet.notifications.stakingReward.type),
                "transfer" to mapOf(
                    "type" to wallet.notifications.transfer.type,
                    "value" to wallet.notifications.transfer.value
                )
            )
        )
    }
}
