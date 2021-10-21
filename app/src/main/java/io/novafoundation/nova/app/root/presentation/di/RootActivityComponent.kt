package io.novafoundation.nova.app.root.presentation.di

import androidx.appcompat.app.AppCompatActivity
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.app.root.presentation.RootActivity
import io.novafoundation.nova.common.di.scope.ScreenScope

@Subcomponent(
    modules = [
        RootActivityModule::class
    ]
)
@ScreenScope
interface RootActivityComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance activity: AppCompatActivity
        ): RootActivityComponent
    }

    fun inject(rootActivity: RootActivity)
}
