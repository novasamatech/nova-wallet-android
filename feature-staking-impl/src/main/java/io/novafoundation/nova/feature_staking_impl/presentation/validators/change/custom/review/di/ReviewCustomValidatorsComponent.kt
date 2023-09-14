package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.ReviewCustomValidatorsFragment

@Subcomponent(
    modules = [
        ReviewCustomValidatorsModule::class
    ]
)
@ScreenScope
interface ReviewCustomValidatorsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance argument: CustomValidatorsPayload
        ): ReviewCustomValidatorsComponent
    }

    fun inject(fragment: ReviewCustomValidatorsFragment)
}
