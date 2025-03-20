package io.novafoundation.nova.common.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooserFactory
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAskerFactory
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.utils.systemCall.WebViewFilePickerSystemCallFactory

@Module
class WebViewModule {

    @Provides
    @ApplicationScope
    fun provideWebViewPermissionAskerFactory(
        permissionsAsker: PermissionsAskerFactory
    ) = WebViewPermissionAskerFactory(permissionsAsker)

    @Provides
    @ApplicationScope
    fun provideWebViewFilePickerSystemCallFactory() = WebViewFilePickerSystemCallFactory()

    @Provides
    @ApplicationScope
    fun provideWebViewFileChooserFactory(
        systemCallExecutor: SystemCallExecutor,
        webViewFilePickerSystemCallFactory: WebViewFilePickerSystemCallFactory
    ) = WebViewFileChooserFactory(systemCallExecutor, webViewFilePickerSystemCallFactory)
}
