package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.recommended.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.recommended.RecommendedValidatorsFragment

@Subcomponent(
    modules = [
        RecommendedValidatorsModule::class
    ]
)
@ScreenScope
interface RecommendedValidatorsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): RecommendedValidatorsComponent
    }

    fun inject(fragment: RecommendedValidatorsFragment)
}
