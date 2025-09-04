package io.novafoundation.nova.splash.presentation

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.splash.SplashRouter
import kotlinx.coroutines.launch

class SplashViewModel(
    private val router: SplashRouter,
    private val repository: AccountRepository
) : BaseViewModel() {

    fun openInitialDestination() {
        viewModelScope.launch {
            if (repository.isAccountSelected()) {
                openPinCode()
            } else {
                openWelcomeScreen()
            }
        }
    }

    private suspend fun openPinCode() {
        if (repository.isCodeSet()) {
            router.openInitialCheckPincode()
        } else {
            router.openCreatePincode()
        }
    }

    private fun openWelcomeScreen() {
        router.openWelcomeScreen()
    }
}
