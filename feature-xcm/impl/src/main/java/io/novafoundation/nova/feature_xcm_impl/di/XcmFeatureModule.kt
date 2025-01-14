package io.novafoundation.nova.feature_xcm_impl.di

import dagger.Module
import io.novafoundation.nova.feature_xcm_impl.di.modules.BindsModule

@Module(
    includes = [
       BindsModule::class
    ]
)
class XcmFeatureModule
