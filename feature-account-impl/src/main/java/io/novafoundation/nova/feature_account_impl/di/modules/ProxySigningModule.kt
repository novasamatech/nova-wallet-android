package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_impl.presentation.proxy.sign.RealProxySigningPresenter
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.RealSigningNotSupportedPresentable
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable

@Module
class ProxySigningModule {

    @Provides
    @FeatureScope
    fun provideSigningNotSupportedPresentable(contextManager: ContextManager): SigningNotSupportedPresentable {
        return RealSigningNotSupportedPresentable(contextManager)
    }

    @Provides
    @FeatureScope
    fun provideProxySigningPresenter(
        contextManager: ContextManager,
        resourceManager: ResourceManager,
        signingNotSupportedPresentable: SigningNotSupportedPresentable,
        preferences: Preferences
    ): ProxySigningPresenter = RealProxySigningPresenter(contextManager, resourceManager, signingNotSupportedPresentable, preferences)
}
