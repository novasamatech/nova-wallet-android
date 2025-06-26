package io.novafoundation.nova.common.di.modules

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.common.address.format.AddressSchemeFormatter
import io.novafoundation.nova.common.address.format.RealAddressSchemeFormatter

@Module
internal interface CommonBindsModule {

    @Binds
    fun bindAddressSchemeFormatter(real: RealAddressSchemeFormatter): AddressSchemeFormatter
}
