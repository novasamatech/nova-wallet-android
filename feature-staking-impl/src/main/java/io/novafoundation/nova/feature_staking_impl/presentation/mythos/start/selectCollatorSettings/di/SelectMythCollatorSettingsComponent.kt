package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.SelectMythCollatorSettingsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model.MythCollatorRecommendationConfigParcel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.CollatorRecommendationConfigParcelModel

@Subcomponent(
    modules = [
        SelectMythCollatorSettingsModule::class
    ]
)
@ScreenScope
interface SelectMythCollatorSettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: MythCollatorRecommendationConfigParcel,
        ): SelectMythCollatorSettingsComponent
    }

    fun inject(fragment: SelectMythCollatorSettingsFragment)
}
