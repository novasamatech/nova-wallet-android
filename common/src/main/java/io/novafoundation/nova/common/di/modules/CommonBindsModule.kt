package io.novafoundation.nova.common.di.modules

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.common.address.format.AddressSchemeFormatter
import io.novafoundation.nova.common.address.format.RealAddressSchemeFormatter
import io.novafoundation.nova.common.data.memory.RealScopedValueStoreFactory
import io.novafoundation.nova.common.data.memory.ScopedValueStore

@Module
internal interface CommonBindsModule {

    @Binds
    fun bindAddressSchemeFormatter(real: RealAddressSchemeFormatter): AddressSchemeFormatter

    @Binds
    fun bindScopedValueStoreFactory(real: RealScopedValueStoreFactory): ScopedValueStore.Factory
}
