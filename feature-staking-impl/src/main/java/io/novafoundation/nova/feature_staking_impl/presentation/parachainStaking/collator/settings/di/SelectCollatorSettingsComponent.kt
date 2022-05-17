package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.CollatorRecommendationConfigParcelModel

@Subcomponent(
    modules = [
        SelectCollatorSettingsModule::class
    ]
)
@ScreenScope
interface SelectCollatorSettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: CollatorRecommendationConfigParcelModel,
        ): SelectCollatorSettingsComponent
    }

    fun inject(fragment: SelectCollatorSettingsFragment)
}
