package io.novafoundation.nova.feature_external_sign_impl.di.modules.sign

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_external_sign_impl.di.modules.sign.PolkadotSignModule.BindsModule
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot.RealSignBytesChainResolver
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot.SignBytesChainResolver

@Module(includes = [BindsModule::class])
class PolkadotSignModule {

    @Module
    interface BindsModule {

        @Binds
        fun bindSignBytesResolver(real: RealSignBytesChainResolver): SignBytesChainResolver
    }
}
