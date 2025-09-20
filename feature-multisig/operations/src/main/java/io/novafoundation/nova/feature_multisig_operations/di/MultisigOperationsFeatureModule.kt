package io.novafoundation.nova.feature_multisig_operations.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureModule.BindsModule
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters.MultisigActionFormatterDelegate
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters.RealMultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters.TransferMultisigActionFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters.UtilityBatchesActionFormatter

@Module(includes = [BindsModule::class])
class MultisigOperationsFeatureModule {

    @Module
    internal interface BindsModule {

        @Binds
        fun bindMultisigCallFormatter(real: RealMultisigCallFormatter): MultisigCallFormatter

        @Binds
        @IntoSet
        fun bindTransferCallFormatter(real: TransferMultisigActionFormatter): MultisigActionFormatterDelegate

        @Binds
        @IntoSet
        fun bindUtilityBatchCallFormatter(real: UtilityBatchesActionFormatter): MultisigActionFormatterDelegate
    }
}
