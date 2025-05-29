package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_account_api.presenatation.addressActions.AddressActionsMixin
import io.novafoundation.nova.feature_account_impl.presentation.account.addressActions.AddressActionsMixinFactory

@Module
interface AccountBindsModule {

    @Binds
    fun bindAddressActionsMixinFactory(real: AddressActionsMixinFactory): AddressActionsMixin.Factory
}
