package io.novafoundation.nova.feature_push_notifications.data.repository

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_push_notifications.domain.interactor.AllowingState

interface MultisigPushAlertRepository {

    fun isMultisigsPushAlertWasShown(): Boolean

    fun setMultisigsPushAlertWasShown()

    fun showAlertAtStartAllowingState(): AllowingState

    fun setAlertAtStartAllowingState(state: AllowingState)
}

private const val IS_MULTISIGS_PUSH_ALERT_WAS_SHOWN = "IS_MULTISIGS_PUSH_ALERT_WAS_SHOWN"
private const val MULTISIGS_PUSH_ALERT_ALLOWING_STATE = "MULTISIGS_PUSH_ALERT_ALLOWING_STATE"

class RealMultisigPushAlertRepository(
    private val preferences: Preferences
) : MultisigPushAlertRepository {
    override fun isMultisigsPushAlertWasShown(): Boolean {
        return preferences.getBoolean(IS_MULTISIGS_PUSH_ALERT_WAS_SHOWN, false)
    }

    override fun setMultisigsPushAlertWasShown() {
        preferences.putBoolean(IS_MULTISIGS_PUSH_ALERT_WAS_SHOWN, true)
    }

    override fun showAlertAtStartAllowingState(): AllowingState {
        val state = preferences.getString(MULTISIGS_PUSH_ALERT_ALLOWING_STATE, AllowingState.INITIAL.toString())
        return AllowingState.valueOf(state)
    }

    override fun setAlertAtStartAllowingState(state: AllowingState) {
        preferences.putString(MULTISIGS_PUSH_ALERT_ALLOWING_STATE, state.toString())
    }
}
