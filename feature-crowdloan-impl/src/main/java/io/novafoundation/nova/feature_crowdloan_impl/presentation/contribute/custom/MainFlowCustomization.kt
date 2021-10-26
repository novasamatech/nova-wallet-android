package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom

import android.os.Parcelable
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import kotlinx.coroutines.CoroutineScope

interface MainFlowCustomization {

    interface ViewState {

        suspend fun buildCustomPayload(): Parcelable?
    }

    fun injectViews(into: ViewGroup, state: ViewState, scope: LifecycleCoroutineScope)

    fun createViewState(coroutineScope: CoroutineScope, parachainMetadata: ParachainMetadata?): ViewState
}
