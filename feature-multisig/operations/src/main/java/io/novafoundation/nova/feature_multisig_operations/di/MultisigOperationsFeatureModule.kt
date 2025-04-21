package io.novafoundation.nova.feature_multisig_operations.di

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureModule.BindsModule
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.common.RealMultisigOperationFormatter

@Module(includes = [BindsModule::class])
class MultisigOperationsFeatureModule {

    @Module
    internal interface BindsModule {
        @Binds
        fun bindOperationsFormatter(real: RealMultisigOperationFormatter): MultisigOperationFormatter
    }
}
