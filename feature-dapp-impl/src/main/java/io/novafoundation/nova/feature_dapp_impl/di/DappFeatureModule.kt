package io.novafoundation.nova.feature_dapp_impl.di

import dagger.Module
import io.novafoundation.nova.feature_dapp_impl.di.modules.Web3Module

@Module(includes = [Web3Module::class])
class DappFeatureModule
