package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.password.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.password.ExportJsonPasswordFragment

@Subcomponent(
    modules = [
        ExportJsonPasswordModule::class
    ]
)
@ScreenScope
interface ExportJsonPasswordComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ExportPayload,
        ): ExportJsonPasswordComponent
    }

    fun inject(fragment: ExportJsonPasswordFragment)
}
