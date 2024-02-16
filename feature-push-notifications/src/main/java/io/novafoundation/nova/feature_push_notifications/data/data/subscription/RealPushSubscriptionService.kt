package io.novafoundation.nova.feature_push_notifications.data.data.subscription

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.formatting.formatDateISO_8601_NoMs
import io.novafoundation.nova.common.utils.mapOfNotNullValues
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_account_api.domain.model.mainEthereumAddress
import io.novafoundation.nova.feature_push_notifications.data.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.feature_push_notifications.data.data.settings.PushSettings
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import java.util.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.tasks.await

private const val COLLECTION_NAME = "users"
private const val PREFS_FIRESTORE_UUID = "firestore_uuid"

class RealPushSubscriptionService(
    private val prefs: Preferences,
    private val chainRegistry: ChainRegistry,
    private val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider,
    private val accountRepository: AccountRepository
) : PushSubscriptionService {

    override suspend fun handleSubscription(pushEnabled: Boolean, token: String?, pushSettings: PushSettings) {
        if (!googleApiAvailabilityProvider.isAvailable()) return

        val tokenExist = token != null
        if (pushEnabled != tokenExist) throw IllegalStateException("Token should exist to enable push notifications")

        handleTopics(pushEnabled, pushSettings)
        handleFirestore(token, pushSettings)
    }

    private fun getFirestoreUUID(): String {
        var uuid = prefs.getString(PREFS_FIRESTORE_UUID)

        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            prefs.putString(PREFS_FIRESTORE_UUID, uuid)
        }

        return uuid
    }

    private suspend fun handleFirestore(token: String?, pushSettings: PushSettings) {
        if (token == null) {
            Firebase.firestore.collection(COLLECTION_NAME)
                .document(getFirestoreUUID())
                .delete()
                .await()
        } else {
            val model = mapToFirestorePushSettings(
                token,
                Date(),
                pushSettings
            )

            Firebase.firestore.collection(COLLECTION_NAME)
                .document(getFirestoreUUID())
                .set(model)
                .await()
        }
    }

    private suspend fun handleTopics(pushEnabled: Boolean, pushSettings: PushSettings) {
        // TODO unsubscribe from old gov topics
        val deferreds = buildList<Deferred<Void>> {
            this += handleSubscription(pushSettings.announcementsEnabled && pushEnabled, "appUpdates")

            this += pushSettings.governanceState.flatMapChainToTracks()
                .map { (chainId, track) -> handleSubscription(true, "govState:$chainId:$track") }

            this += pushSettings.newReferenda.flatMapChainToTracks()
                .map { (chainId, track) -> handleSubscription(true, "govNewRef:$chainId:$track") }
        }

        deferreds.awaitAll()
    }

    private suspend fun handleSubscription(subscribe: Boolean, topic: String): Deferred<Void> {
        return if (subscribe) {
            subscribeToTopic(topic)
        } else {
            unsubscribeFromTopic(topic)
        }
    }

    private suspend fun subscribeToTopic(topic: String): Deferred<Void> {
        return Firebase.messaging.subscribeToTopic(topic)
            .asDeferred()
    }

    private suspend fun unsubscribeFromTopic(topic: String): Deferred<Void> {
        return Firebase.messaging.unsubscribeFromTopic(topic)
            .asDeferred()
    }

    private suspend fun mapToFirestorePushSettings(
        token: String,
        date: Date,
        settings: PushSettings
    ): Map<String, Any> {
        val chainsById = chainRegistry.chainsById()
        val metaAccountsById = accountRepository
            .getActiveMetaAccounts()
            .associateBy { it.id }

        return mapOf(
            "pushToken" to token,
            "updatedAt" to formatDateISO_8601_NoMs(date),
            "wallets" to settings.subscribedMetaAccounts.mapNotNull { mapToFirestoreWallet(it, metaAccountsById, chainsById) },
            "notifications" to mapOfNotNullValues(
                "stakingReward" to mapToFirestoreChainFeature(settings.stakingReward),
                "tokenSent" to settings.sentTokensEnabled.mapToFirestoreChainFeatureOrNull(),
                "tokenReceived" to settings.receivedTokensEnabled.mapToFirestoreChainFeatureOrNull(),
                "govMyDelegatorVoted" to mapToFirestoreChainFeature(settings.govMyDelegatorVoted),
                "govMyReferendumFinished" to mapToFirestoreChainFeature(settings.govMyReferendumFinished)
            )
        )
    }

    private suspend fun mapToFirestoreWallet(metaId: Long, metaAccountsById: Map<Long, MetaAccount>, chainsById: ChainsById): Map<String, Any>? {
        val metaAccount = metaAccountsById[metaId] ?: return null
        return mapOfNotNullValues(
            "baseEthereum" to metaAccount.mainEthereumAddress(),
            "baseSubstrate" to metaAccount.defaultSubstrateAddress,
            "chainSpecific" to metaAccount.chainAccounts.mapValuesNotNull { (chainId, chainAccount) ->
                val chain = chainsById[chainId] ?: return@mapValuesNotNull null
                chain.addressOf(chainAccount.accountId)
            }.nullIfEmpty()
        )
    }

    private fun mapToFirestoreChainFeature(chainFeature: PushSettings.ChainFeature): Map<String, Any>? {
        return when (chainFeature) {
            is PushSettings.ChainFeature.All -> mapOf("type" to "all")
            is PushSettings.ChainFeature.Concrete -> {
                if (chainFeature.chainIds.isEmpty()) {
                    null
                } else {
                    mapOf("type" to "concrete", "value" to chainFeature.chainIds)
                }
            }
        }
    }

    private fun Boolean.mapToFirestoreChainFeatureOrNull(): Map<String, Any>? {
        return if (true) mapOf("type" to "all") else null
    }

    private fun List<PushSettings.GovernanceFeature>.flatMapChainToTracks(): List<Pair<ChainId, String>> {
        return flatMap { chainGovState -> chainGovState.tracks.map { chainGovState.chainId to it } }
    }

    private fun Map<String, Any>.nullIfEmpty(): Map<String, Any>? {
        return if (isEmpty()) null else this
    }
}
