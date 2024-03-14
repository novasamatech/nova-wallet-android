package io.novafoundation.nova.feature_push_notifications.data.data.subscription

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.formatting.formatDateISO_8601_NoMs
import io.novafoundation.nova.common.utils.mapOfNotNullValues
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.common.utils.useIf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_account_api.domain.model.mainEthereumAddress
import io.novafoundation.nova.feature_push_notifications.BuildConfig
import io.novafoundation.nova.feature_push_notifications.data.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.feature_push_notifications.data.data.PUSH_LOG_TAG
import io.novafoundation.nova.feature_push_notifications.data.domain.model.PushSettings
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.chainIdHexPrefix16
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import java.math.BigInteger
import java.util.UUID
import java.util.Date
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

private const val COLLECTION_NAME = "users"
private const val PREFS_FIRESTORE_UUID = "firestore_uuid"

private const val GOV_STATE_TOPIC_NAME = "govState"
private const val NEW_REFERENDA_TOPIC_NAME = "govNewRef"

class TrackIdentifiable(val chainId: ChainId, val track: BigInteger) : Identifiable {
    override val identifier: String = "$chainId:$track"
}

class RealPushSubscriptionService(
    private val prefs: Preferences,
    private val chainRegistry: ChainRegistry,
    private val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider,
    private val accountRepository: AccountRepository
) : PushSubscriptionService {

    private val generateIdMutex = Mutex()

    override suspend fun handleSubscription(pushEnabled: Boolean, token: String?, oldSettings: PushSettings, newSettings: PushSettings?) {
        if (!googleApiAvailabilityProvider.isAvailable()) return

        val tokenExist = token != null
        if (pushEnabled != tokenExist) throw IllegalStateException("Token should exist to enable push notifications")

        handleTopics(pushEnabled, oldSettings, newSettings)
        handleFirestore(token, newSettings)

        if (BuildConfig.DEBUG) {
            Log.d(PUSH_LOG_TAG, "Firestore user updated: ${getFirestoreUUID()}")
        }
    }

    private suspend fun getFirestoreUUID(): String {
        return generateIdMutex.withLock {
            var uuid = prefs.getString(PREFS_FIRESTORE_UUID)

            if (uuid == null) {
                uuid = UUID.randomUUID().toString()
                prefs.putString(PREFS_FIRESTORE_UUID, uuid)
            }

            uuid
        }
    }

    private suspend fun handleFirestore(token: String?, pushSettings: PushSettings?) {
        val hasAccounts = pushSettings?.subscribedMetaAccounts?.any() ?: false
        if (token == null || pushSettings == null || !hasAccounts) {
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

    private suspend fun handleTopics(pushEnabled: Boolean, oldSettings: PushSettings, newSettings: PushSettings?) {
        val referendumUpdateTracks = newSettings?.getGovernanceTracksFor { it.referendumUpdateEnabled }
            ?.takeIf { pushEnabled }
            .orEmpty()

        val newReferendaTracks = newSettings?.getGovernanceTracksFor { it.newReferendaEnabled }
            ?.takeIf { pushEnabled }
            .orEmpty()

        val govStateTracksDiff = CollectionDiffer.findDiff(
            oldItems = oldSettings.getGovernanceTracksFor { it.referendumUpdateEnabled },
            newItems = referendumUpdateTracks,
            forceUseNewItems = false
        )
        val newReferendaDiff = CollectionDiffer.findDiff(
            oldItems = oldSettings.getGovernanceTracksFor { it.newReferendaEnabled },
            newItems = newReferendaTracks,
            forceUseNewItems = false
        )

        val deferreds = buildList<Deferred<Void>> {
            val announcementsEnabled = newSettings?.announcementsEnabled ?: false
            this += handleSubscription(announcementsEnabled && pushEnabled, "appUpdates")

            this += govStateTracksDiff.added
                .map { subscribeToTopic("${GOV_STATE_TOPIC_NAME}_${it.chainId}_${it.track}") }
            this += govStateTracksDiff.removed
                .map { unsubscribeFromTopic("${GOV_STATE_TOPIC_NAME}_${it.chainId}_${it.track}") }

            this += newReferendaDiff.added
                .map { subscribeToTopic("${NEW_REFERENDA_TOPIC_NAME}_${it.chainId}_${it.track}") }
            this += newReferendaDiff.removed
                .map { unsubscribeFromTopic("${NEW_REFERENDA_TOPIC_NAME}_${it.chainId}_${it.track}") }
        }

        deferreds.awaitAll()
    }

    private fun handleSubscription(subscribe: Boolean, topic: String): Deferred<Void> {
        return if (subscribe) {
            subscribeToTopic(topic)
        } else {
            unsubscribeFromTopic(topic)
        }
    }

    private fun subscribeToTopic(topic: String): Deferred<Void> {
        return Firebase.messaging.subscribeToTopic(topic)
            .asDeferred()
    }

    private fun unsubscribeFromTopic(topic: String): Deferred<Void> {
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
                "tokenReceived" to settings.receivedTokensEnabled.mapToFirestoreChainFeatureOrNull()
            )
        )
    }

    private fun mapToFirestoreWallet(metaId: Long, metaAccountsById: Map<Long, MetaAccount>, chainsById: ChainsById): Map<String, Any>? {
        val metaAccount = metaAccountsById[metaId] ?: return null
        return mapOfNotNullValues(
            "baseEthereum" to metaAccount.mainEthereumAddress(),
            "baseSubstrate" to metaAccount.defaultSubstrateAddress,
            "chainSpecific" to metaAccount.chainAccounts.mapValuesNotNull { (chainId, chainAccount) ->
                val chain = chainsById[chainId] ?: return@mapValuesNotNull null
                chain.addressOf(chainAccount.accountId)
            }.transfromChainIdsTo16Hex()
                .nullIfEmpty()
        )
    }

    private fun mapToFirestoreChainFeature(chainFeature: PushSettings.ChainFeature): Map<String, Any>? {
        return when (chainFeature) {
            is PushSettings.ChainFeature.All -> mapOf("type" to "all")
            is PushSettings.ChainFeature.Concrete -> {
                if (chainFeature.chainIds.isEmpty()) {
                    null
                } else {
                    mapOf("type" to "concrete", "value" to chainFeature.chainIds.transfromChainIdsTo16Hex())
                }
            }
        }
    }

    private fun Boolean.mapToFirestoreChainFeatureOrNull(): Map<String, Any>? {
        return if (this) mapOf("type" to "all") else null
    }

    private fun PushSettings.getGovernanceTracksFor(filter: (PushSettings.GovernanceState) -> Boolean): List<TrackIdentifiable> {
        return governance.filter { (_, state) -> filter(state) }
            .flatMap { (chainId, state) -> state.tracks.map { TrackIdentifiable(chainId.chainIdHexPrefix16(), it.value) } }
    }

    private fun Map<String, Any>.nullIfEmpty(): Map<String, Any>? {
        return if (isEmpty()) null else this
    }

    private fun <T> Map<ChainId, T>.transfromChainIdsTo16Hex(): Map<String, T> {
        return mapKeys { (chainId, _) -> chainId.chainIdHexPrefix16() }
    }

    private fun List<ChainId>.transfromChainIdsTo16Hex(): List<String> {
        return map { chainId -> chainId.chainIdHexPrefix16() }
    }
}
