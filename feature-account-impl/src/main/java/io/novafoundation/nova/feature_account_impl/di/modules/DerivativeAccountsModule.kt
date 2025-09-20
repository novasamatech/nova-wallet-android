package io.novafoundation.nova.feature_account_impl.di.modules;

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.DerivativeFormatter
import io.novafoundation.nova.feature_account_impl.data.derivative.DerivativeAccountRepository
import io.novafoundation.nova.feature_account_impl.data.derivative.RealDerivativeAccountRepository
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated.RealDerivativeFormatter

@Module
interface DerivativeAccountsModule {

   @Binds
   fun bindDerivativeAccountRepository(real: RealDerivativeAccountRepository): DerivativeAccountRepository

   @Binds
   fun bindDerivativeFormatter(real: RealDerivativeFormatter): DerivativeFormatter
}
