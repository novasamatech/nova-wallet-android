package io.novafoundation.nova.feature_ledger_impl.di.modules

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.generic.GenericLedgerEvmAlertFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.generic.RealGenericLedgerEvmAlertFormatter

@Module
interface LedgerBindsModule {

    @Binds
    fun bindEvmUpdateFormatter(real: RealGenericLedgerEvmAlertFormatter): GenericLedgerEvmAlertFormatter
}
