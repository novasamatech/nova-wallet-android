package io.novafoundation.nova.feature_external_sign_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_external_sign_api.domain.sign.evm.EvmTypedMessageParser
import io.novafoundation.nova.feature_external_sign_impl.di.modules.sign.EvmSignModule
import io.novafoundation.nova.feature_external_sign_impl.di.modules.sign.PolkadotSignModule
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.evm.RealEvmTypedMessageParser

@Module(includes = [EvmSignModule::class, PolkadotSignModule::class])
class ExternalSignFeatureModule {

    @Provides
    @FeatureScope
    fun provideEvmTypedMessageParser(): EvmTypedMessageParser = RealEvmTypedMessageParser()
}
