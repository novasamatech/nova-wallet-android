package io.novafoundation.nova.feature_onboarding_impl.presentation.consentUpgrade.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_onboarding_impl.presentation.consentUpgrade.ConsentBannerUpgradeFragment

@Subcomponent(
    modules = [
        ConsentBannerUpgradeModule::class
    ]
)
@ScreenScope
interface ConsentBannerUpgradeComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): ConsentBannerUpgradeComponent
    }

    fun inject(fragment: ConsentBannerUpgradeFragment)
}
