package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.paritySigner.connect.ParitySignerStartPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan.ScanImportParitySignerFragment

@Subcomponent(
    modules = [
        ScanImportParitySignerModule::class
    ]
)
@ScreenScope
interface ScanImportParitySignerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ParitySignerStartPayload,
        ): ScanImportParitySignerComponent
    }

    fun inject(fragment: ScanImportParitySignerFragment)
}
