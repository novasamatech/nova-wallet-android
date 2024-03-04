package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.ScanSignParitySignerFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.ScanSignParitySignerPayload

@Subcomponent(
    modules = [
        ScanSignParitySignerModule::class
    ]
)
@ScreenScope
interface ScanSignParitySignerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ScanSignParitySignerPayload
        ): ScanSignParitySignerComponent
    }

    fun inject(fragment: ScanSignParitySignerFragment)
}
