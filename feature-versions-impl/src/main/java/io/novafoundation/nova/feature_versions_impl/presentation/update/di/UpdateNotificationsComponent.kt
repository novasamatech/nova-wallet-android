package io.novafoundation.nova.feature_versions_impl.presentation.update.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_versions_impl.presentation.update.UpdateNotificationFragment

@Subcomponent(
    modules = [
        UpdateNotificationsModule::class
    ]
)
@ScreenScope
interface UpdateNotificationsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): UpdateNotificationsComponent
    }

    fun inject(fragment: UpdateNotificationFragment)
}
