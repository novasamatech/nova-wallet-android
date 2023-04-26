package io.novafoundation.nova.app.root.navigation

import android.os.Parcelable
import androidx.lifecycle.asFlow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import kotlinx.coroutines.flow.Flow
import java.util.UUID

abstract class BaseInterScreenCommunicator<I : Parcelable, O : Parcelable>(
    private val navigationHolder: NavigationHolder,
) : InterScreenCommunicator<I, O> {

    private val liveDataKey = UUID.randomUUID().toString()

    protected val navController: NavController
        get() = navigationHolder.navController!!

    // from requester - retrieve from current entry
    override val latestResponse: O?
        get() = navController.currentBackStackEntry!!.savedStateHandle
            .get(liveDataKey)

    // from responder - retrieve from previous (requester) entry
    override val lastState: O?
        get() = navController.previousBackStackEntry!!.savedStateHandle
            .get(liveDataKey)

    override val responseFlow: Flow<O>
        get() = createResponseFlow()

    abstract override fun openRequest(request: I)

    fun clearedResponseFlow(): Flow<O> {
        navController.currentBackStackEntry!!.savedStateHandle.remove<O>(liveDataKey)
        return createResponseFlow()
    }

    override fun respond(response: O) {
        // previousBackStackEntry since we want to report to previous screen
        saveResultTo(navController.previousBackStackEntry!!, response)
    }

    protected fun saveResultTo(backStackEntry: NavBackStackEntry, response: O) {
        backStackEntry.savedStateHandle.set(liveDataKey, response)
    }

    private fun createResponseFlow(): Flow<O> {
        return navController.currentBackStackEntry!!.savedStateHandle
            .getLiveData<O>(liveDataKey)
            .asFlow()
    }
}
