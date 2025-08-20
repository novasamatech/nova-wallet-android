package io.novafoundation.nova.feature_push_notifications.domain.interactor

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.isMultisig
import io.novafoundation.nova.feature_push_notifications.data.repository.MultisigPushAlertRepository
import io.novafoundation.nova.feature_push_notifications.data.settings.PushSettingsProvider
import kotlinx.coroutines.flow.Flow

interface MultisigPushAlertInteractor {

    fun isPushNotificationsEnabled(): Boolean

    fun isAlertWasAlreadyShown(): Boolean

    fun setAlertWasAlreadyShown()

    suspend fun allowedToShowAlertAtStart(): Boolean

    suspend fun hasMultisigWallets(consumedMetaIdsUpdates: List<Long>): Boolean
}

enum class AllowingState {
    INITIAL, ALLOWED, NOT_ALLOWED
}

class RealMultisigPushAlertInteractor(
    private val pushSettingsProvider: PushSettingsProvider,
    private val accountRepository: AccountRepository,
    private val multisigPushAlertRepository: MultisigPushAlertRepository
) : MultisigPushAlertInteractor {

    override fun isPushNotificationsEnabled(): Boolean {
        return pushSettingsProvider.isPushNotificationsEnabled()
    }

    override fun isAlertWasAlreadyShown(): Boolean {
        return multisigPushAlertRepository.isMultisigsPushAlertWasShown()
    }

    override fun setAlertWasAlreadyShown() {
        multisigPushAlertRepository.setMultisigsPushAlertWasShown()
    }

    /**
     * We have to check if we can show alert right after user update the app.
     * Showing is allowed when user have multisig accounts in first app start after update
     */
    override suspend fun allowedToShowAlertAtStart(): Boolean {
        val allowingState = multisigPushAlertRepository.showAlertAtStartAllowingState()

        if (allowingState == AllowingState.INITIAL) {
            val userHasMultisigs = accountRepository.hasMetaAccountsByType(LightMetaAccount.Type.MULTISIG)
            if (userHasMultisigs) {
                multisigPushAlertRepository.setAlertAtStartAllowingState(AllowingState.ALLOWED)
                return true
            } else {
                multisigPushAlertRepository.setAlertAtStartAllowingState(AllowingState.NOT_ALLOWED)
                return false
            }
        }

        return allowingState == AllowingState.ALLOWED
    }

    override suspend fun hasMultisigWallets(consumedMetaIdsUpdates: List<Long>): Boolean {
        val consumedMetaAccountUpdates = accountRepository.getMetaAccountsByIds(consumedMetaIdsUpdates)
        return consumedMetaAccountUpdates.any { it.isMultisig() }
    }
}
