package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitReviewFragment
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitReviewPayload

@ScreenScope
@Subcomponent(modules = [BittensorExitReviewModule::class])
interface BittensorExitReviewComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment, @BindsInstance payload: BittensorExitReviewPayload): BittensorExitReviewComponent
    }

    fun inject(fragment: BittensorExitReviewFragment)
}
