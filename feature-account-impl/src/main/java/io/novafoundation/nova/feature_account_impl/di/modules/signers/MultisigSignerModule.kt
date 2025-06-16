package io.novafoundation.nova.feature_account_impl.di.modules.signers

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigValidationsRepository
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_impl.data.multisig.repository.RealMultisigValidationsRepository
import io.novafoundation.nova.feature_account_impl.presentation.multisig.MultisigSigningPresenter
import io.novafoundation.nova.feature_account_impl.presentation.multisig.RealMultisigSigningPresenter

@Module(includes = [MultisigSignerModule.BindsModule::class])
class MultisigSignerModule {

    @Module
    interface BindsModule {

        @Binds
        fun bindMultisigDepositRepository(real: RealMultisigValidationsRepository): MultisigValidationsRepository

        @Binds
        fun bindMultisigPresenter(real: RealMultisigSigningPresenter): MultisigSigningPresenter
    }

    @Provides
    @FeatureScope
    fun provideRequestBus() = MultisigExtrinsicValidationRequestBus()
}
