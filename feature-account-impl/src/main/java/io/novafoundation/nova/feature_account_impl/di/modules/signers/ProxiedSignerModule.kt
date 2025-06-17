package io.novafoundation.nova.feature_account_impl.di.modules.signers

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxiedSignerFactory
import io.novafoundation.nova.feature_account_impl.data.signer.proxy.ProxyCallFilterFactory
import io.novafoundation.nova.feature_account_impl.di.modules.signers.ProxiedSignerModule.BindsModule
import io.novafoundation.nova.feature_account_impl.presentation.proxy.sign.RealProxySigningPresenter
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

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

    @Provides
    @FeatureScope
    fun provideProxiedSignerFactory(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        proxySigningPresenter: ProxySigningPresenter,
        proxyRepository: GetProxyRepository,
        proxyExtrinsicValidationRequestBus: ProxyExtrinsicValidationRequestBus,
        proxyCallFilterFactory: ProxyCallFilterFactory
    ) = ProxiedSignerFactory(
        chainRegistry = chainRegistry,
        accountRepository = accountRepository,
        proxySigningPresenter = proxySigningPresenter,
        getProxyRepository = proxyRepository,
        proxyExtrinsicValidationEventBus = proxyExtrinsicValidationRequestBus,
        proxyCallFilterFactory = proxyCallFilterFactory
    )
}
