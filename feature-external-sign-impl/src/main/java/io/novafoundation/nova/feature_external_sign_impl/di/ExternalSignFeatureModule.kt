package io.novafoundation.nova.feature_external_sign_impl.di

import dagger.Module
import io.novafoundation.nova.feature_external_sign_impl.di.modules.sign.EvmSignModule
import io.novafoundation.nova.feature_external_sign_impl.di.modules.sign.PolkadotSignModule

@Module(includes = [EvmSignModule::class, PolkadotSignModule::class])
class ExternalSignFeatureModule
