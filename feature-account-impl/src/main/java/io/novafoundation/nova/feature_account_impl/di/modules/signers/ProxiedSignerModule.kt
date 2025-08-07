package io.novafoundation.nova.feature_account_impl.di.modules.signers

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxyCallFilterFactory
import io.novafoundation.nova.feature_account_impl.di.modules.signers.ProxiedSignerModule.BindsModule
import io.novafoundation.nova.feature_account_impl.presentation.proxy.sign.RealProxySigningPresenter

@Module(includes = [BindsModule::class])
class ProxiedSignerModule {

    @Module
    interface BindsModule {

        @Binds
        fun bindProxySigningPresenter(real: RealProxySigningPresenter): ProxySigningPresenter
    }

    @Provides
    @FeatureScope
    fun provideProxyExtrinsicValidationRequestBus() = ProxyExtrinsicValidationRequestBus()

    @Provides
    @FeatureScope
    fun provideProxyCallFilterFactory() = ProxyCallFilterFactory()
}
