package io.novafoundation.nova.common.di.modules.shared

import androidx.fragment.app.Fragment
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory

@Module
class PermissionAskerForFragmentModule {

    @Provides
    @ScreenScope
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment
    ) = permissionsAskerFactory.create(fragment)
}
