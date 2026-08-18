package io.novafoundation.nova.app.root.presentation.legal.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.app.root.presentation.legal.LegalConsentBottomSheet
import io.novafoundation.nova.common.di.scope.ScreenScope

@Subcomponent(
    modules = [
        LegalConsentModule::class
    ]
)
@ScreenScope
interface LegalConsentComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): LegalConsentComponent
    }

    fun inject(fragment: LegalConsentBottomSheet)
}
