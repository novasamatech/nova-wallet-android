package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show.ShowSignParitySignerPayload

@Subcomponent(
    modules = [
        ShowSignParitySignerModule::class
    ]
)
@ScreenScope
interface ShowSignParitySignerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ShowSignParitySignerPayload
        ): ShowSignParitySignerComponent
    }

    fun inject(fragment: ShowSignParitySignerFragment)
}
