package io.novafoundation.nova.feature_multisig_operations.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureModule.BindsModule
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters.DefaultLeafActionFormatter
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
    }

    @Provides
    @FeatureScope
    fun provideDelegatesList(
        transfers: TransferMultisigActionFormatter,
        batches: UtilityBatchesActionFormatter,
        defaultLeafs: DefaultLeafActionFormatter
    ): List<MultisigActionFormatterDelegate> {
        // Important!
        // Order here is important for those formatters that are not mutually exclusive and can both format the same node
        // In particular, make sure `defaultLeafs` is always the last one. Otherwise it will catch nodes before any more specific formatter
        return listOf(
            batches,
            transfers,
            defaultLeafs
        )
    }
}
