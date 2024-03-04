package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.ExportJsonConfirmFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.ExportJsonConfirmPayload

@Subcomponent(
    modules = [
        ExportJsonConfirmModule::class
    ]
)
@ScreenScope
interface ExportJsonConfirmComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ExportJsonConfirmPayload
        ): ExportJsonConfirmComponent
    }

    fun inject(fragment: ExportJsonConfirmFragment)
}
