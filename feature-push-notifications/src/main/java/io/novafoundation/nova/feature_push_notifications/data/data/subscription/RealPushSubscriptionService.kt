package io.novafoundation.nova.feature_push_notifications.data.data.subscription

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.formatting.formatDateISO_8601_NoMs
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.feature_push_notifications.data.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettings
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import java.util.*
import kotlinx.coroutines.tasks.await

private const val COLLECTION_NAME = "users"
private const val PREFS_FIRESTORE_UUID = "firestore_uuid"

class RealPushSubscriptionService(
    private val prefs: Preferences,
    private val chainRegistry: ChainRegistry,
    private val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider
) : PushSubscriptionService {

    override suspend fun handleSubscription(token: String, pushSettings: PushSettings) {
        if (!googleApiAvailabilityProvider.isAvailable()) return

        handleSubscription(pushSettings.appMajorUpdates, "appMajorUpdates")
        handleSubscription(pushSettings.appCriticalUpdates, "appCriticalUpdates")
        handleSubscription(pushSettings.chainReferendums, "chainReferendums")

        sendWaletSettingsToFirestore(token, pushSettings)
    }

    private fun getFirestoreUUID(): String {
        var uuid = prefs.getString(PREFS_FIRESTORE_UUID)

        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            prefs.putString(PREFS_FIRESTORE_UUID, uuid)
        }

        return uuid
    }

    private suspend fun sendWaletSettingsToFirestore(token: String, pushSettings: PushSettings) {
        val model = mapToFirestorePushSettings(
            token,
            Date(),
            chainRegistry.chainsById(),
            pushSettings.wallets
        )
        Firebase.firestore.collection(COLLECTION_NAME)
            .document(getFirestoreUUID())
            .set(model)
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

    private fun mapToFirestorePushSettings(
        token: String,
        date: Date,
        chainsById: ChainsById,
        walletsSettings: List<PushSettings.Wallet>
    ): Map<String, Any> {
        return mapOf(
            "pushToken" to token,
            "updatedAt" to formatDateISO_8601_NoMs(date),
            "wallets" to walletsSettings.map { mapToFirestoreWallet(it, chainsById) }
        )
    }

    private fun mapToFirestoreWallet(wallet: PushSettings.Wallet, chainsById: ChainsById): Map<String, Any> {
        return mapOf(
            "baseEthereumAccount" to wallet.baseEthereumAccount,
            "baseSubstrateAccount" to wallet.baseSubstrateAccount,
            "chainAccounts" to wallet.chainAccounts.mapValuesNotNull { chainsById.lovercaseNameOf(it.key) },
            "notifications" to mapOf(
                "stakingReward" to mapToFirestoreChainFeature(wallet.notifications.stakingReward, chainsById),
                "transfer" to mapToFirestoreChainFeature(wallet.notifications.transfer, chainsById)
            )
        )
    }

    private fun mapToFirestoreChainFeature(chainFeature: PushSettings.ChainFeature, chainsById: ChainsById): Map<String, Any> {
        return when (chainFeature) {
            is PushSettings.ChainFeature.All -> mapOf("type" to "all")
            is PushSettings.ChainFeature.Concrete -> mapOf(
                "type" to "concrete",
                "value" to chainFeature.chainIds.mapNotNull { chainsById.lovercaseNameOf(it) }
            )
        }
    }

    private fun Map<ChainId, Chain>.lovercaseNameOf(key: String): String? {
        return this[key]?.name?.lowercase()
    }
}
