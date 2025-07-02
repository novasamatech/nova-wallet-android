package io.novafoundation.nova.feature_multisig_operations.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.MultisigFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.ProxyFormatter
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureModule.BindsModule
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters.MultisigActionFormatterDelegate
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters.RealMultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters.TransferMultisigActionFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.SignatoryListFormatter

@Module(includes = [BindsModule::class])
class MultisigOperationsFeatureModule {

    @Module
    internal interface BindsModule {

        @Binds
        fun bindMultisigCallFormatter(real: RealMultisigCallFormatter): MultisigCallFormatter

        @Binds
        @IntoSet
        fun bindTransferCallFormatter(real: TransferMultisigActionFormatter): MultisigActionFormatterDelegate
    }

    @Provides
    @FeatureScope
    fun provideSignatoryListFormatter(
        addressIconGenerator: AddressIconGenerator,
        walletUiUseCase: WalletUiUseCase,
        accountInteractor: AccountInteractor,
        proxyFormatter: ProxyFormatter,
        multisigFormatter: MultisigFormatter
    ): SignatoryListFormatter {
        return SignatoryListFormatter(
            addressIconGenerator,
            walletUiUseCase,
            accountInteractor,
            proxyFormatter,
            multisigFormatter
        )
    }
}
