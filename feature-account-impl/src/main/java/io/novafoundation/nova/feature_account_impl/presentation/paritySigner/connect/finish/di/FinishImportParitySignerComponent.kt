package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.finish.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.paritySigner.connect.ParitySignerAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.finish.FinishImportParitySignerFragment

@Subcomponent(
    modules = [
        FinishImportParitySignerModule::class
    ]
)
@ScreenScope
interface FinishImportParitySignerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ParitySignerAccountPayload,
        ): FinishImportParitySignerComponent
    }

    fun inject(fragment: FinishImportParitySignerFragment)
}
