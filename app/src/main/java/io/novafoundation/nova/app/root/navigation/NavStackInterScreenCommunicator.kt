package io.novafoundation.nova.app.root.navigation

import android.os.Parcelable
import androidx.annotation.CallSuper
import androidx.lifecycle.asFlow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import kotlinx.coroutines.flow.Flow
import java.util.UUID

abstract class NavStackInterScreenCommunicator<I : Parcelable, O : Parcelable>(
    private val navigationHolder: NavigationHolder,
) : InterScreenCommunicator<I, O> {

    private val responseKey = UUID.randomUUID().toString()
    private val requestKey = UUID.randomUUID().toString()

    protected val navController: NavController
        get() = navigationHolder.navController!!

    // from requester - retrieve from current entry
    override val latestResponse: O?
        get() = navController.currentBackStackEntry!!.savedStateHandle
            .get(responseKey)

    // from responder - retrieve from previous (requester) entry
    override val lastState: O?
        get() = navController.previousBackStackEntry!!.savedStateHandle
            .get(responseKey)

    override val responseFlow: Flow<O>
        get() = createResponseFlow()

    // from responder - retrieve from previous (requester) entry
    override val lastInput: I?
        get() = navController.previousBackStackEntry!!.savedStateHandle
            .get(requestKey)

    @CallSuper
    override fun openRequest(request: I) {
        saveRequest(request)
    }

    fun clearedResponseFlow(): Flow<O> {
        navController.currentBackStackEntry!!.savedStateHandle.remove<O>(requestKey)
        return createResponseFlow()
    }

    override fun respond(response: O) {
        // previousBackStackEntry since we want to report to previous screen
        saveResultTo(navController.previousBackStackEntry!!, response)
    }

    protected fun saveResultTo(backStackEntry: NavBackStackEntry, response: O) {
        backStackEntry.savedStateHandle.set(responseKey, response)
    }

    private fun saveRequest(request: I) {
        navController.currentBackStackEntry!!.savedStateHandle.set(requestKey, request)
    }

    private fun createResponseFlow(): Flow<O> {
        return navController.currentBackStackEntry!!.savedStateHandle
            .getLiveData<O>(responseKey)
            .asFlow()
    }
}
