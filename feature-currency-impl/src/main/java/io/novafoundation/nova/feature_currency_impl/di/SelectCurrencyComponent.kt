package io.novafoundation.nova.feature_currency_impl.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_currency_impl.presentation.currency.SelectCurrencyFragment

@Subcomponent(
    modules = [
        SelectCurrencyModule::class
    ]
)
@ScreenScope
interface SelectCurrencyComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): SelectCurrencyComponent
    }

    fun inject(fragment: SelectCurrencyFragment)
}
