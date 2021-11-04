package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom

import android.os.Parcelable
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface SelectContributeCustomization {

    interface ViewState {

        val customizationPayloadFlow: Flow<Parcelable?>
    }

    fun injectViews(into: ViewGroup, state: ViewState, scope: LifecycleCoroutineScope)

    fun createViewState(
        features: CrowdloanMainFlowFeatures,
        parachainMetadata: ParachainMetadata,
    ): ViewState
}

interface ConfirmContributeCustomization {

    interface ViewState

    fun injectViews(into: ViewGroup, state: ViewState, scope: LifecycleCoroutineScope)

    fun createViewState(
        coroutineScope: CoroutineScope,
        parachainMetadata: ParachainMetadata,
        customPayload: Parcelable?,
    ): ViewState
}

class CrowdloanMainFlowFeatures(
    val coroutineScope: CoroutineScope,
    val browserable: Browserable.Presentation,
)
