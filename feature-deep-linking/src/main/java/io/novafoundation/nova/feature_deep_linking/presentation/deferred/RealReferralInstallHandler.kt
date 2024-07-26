package io.novafoundation.nova.feature_deep_linking.presentation.deferred

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import io.novafoundation.nova.common.data.storage.Preferences
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val PREF_REFERRAL_INSTALL_HANDLED = "referral_install_handled"

class RealReferralInstallHandler(
    private val context: Context,
    private val preferences: Preferences
) : ReferralInstallHandler {

    override suspend fun getResult(): ReferralInstallResult = suspendCoroutine { continuation ->
        if (isHandled()) {
            continuation.resume(ReferralInstallResult.AlreadyHandled)
        } else {
            val referrerClient = InstallReferrerClient.newBuilder(context).build()

            referrerClient.startConnection(
                object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        when (responseCode) {
                            InstallReferrerClient.InstallReferrerResponse.OK -> {
                                val deepLink = referrerClient.installReferrer.installReferrer

                                continuation.resume(ReferralInstallResult.DeeplinkExtracted(deepLink))
                            }

                            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                                continuation.resume(ReferralInstallResult.NotSupported)
                            }

                            InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                                continuation.resume(ReferralInstallResult.ServiceUnavailable)
                            }
                        }

                        referrerClient.endConnection()
                        setHandled()
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        continuation.resume(ReferralInstallResult.ServiceDisconnected)
                    }
                }
            )
        }
    }

    private fun isHandled(): Boolean {
        return preferences.getBoolean(PREF_REFERRAL_INSTALL_HANDLED, false)
    }

    private fun setHandled() {
        return preferences.putBoolean(PREF_REFERRAL_INSTALL_HANDLED, true)
    }
}
