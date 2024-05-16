package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.ExportJsonFragment

@Subcomponent(
    modules = [
        ExportJsonModule::class
    ]
)
@ScreenScope
interface ExportJsonComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ExportPayload,
        ): ExportJsonComponent
    }

    fun inject(fragment: ExportJsonFragment)
}
