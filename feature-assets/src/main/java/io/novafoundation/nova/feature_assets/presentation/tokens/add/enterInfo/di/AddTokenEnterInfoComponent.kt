package io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoFragment
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoPayload

@Subcomponent(
    modules = [
        AddTokenEnterInfoModule::class
    ]
)
@ScreenScope
interface AddTokenEnterInfoComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AddTokenEnterInfoPayload,
        ): AddTokenEnterInfoComponent
    }

    fun inject(fragment: AddTokenEnterInfoFragment)
}
