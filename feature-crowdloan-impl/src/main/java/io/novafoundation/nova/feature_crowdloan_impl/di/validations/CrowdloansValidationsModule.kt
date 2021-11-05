package io.novafoundation.nova.feature_crowdloan_impl.di.validations

import dagger.Module

@Module(
    includes = [
        ContributeValidationsModule::class,
        MoonbeamTermsValidationsModule::class
    ]
)
class CrowdloansValidationsModule
