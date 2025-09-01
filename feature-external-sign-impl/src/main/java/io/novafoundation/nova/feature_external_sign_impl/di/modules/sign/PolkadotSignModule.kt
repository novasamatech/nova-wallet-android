package io.novafoundation.nova.feature_external_sign_impl.di.modules.sign

import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_external_sign_impl.di.modules.sign.PolkadotSignModule.BindsModule
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot.PolkadotSignInteractorFactory
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot.RealSignBytesChainResolver
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot.SignBytesChainResolver
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [BindsModule::class])
class PolkadotSignModule {

    @Module
    interface BindsModule {

        @Binds
        fun bindSignBytesResolver(real: RealSignBytesChainResolver): SignBytesChainResolver
    }

    @Provides
    @FeatureScope
    fun provideSignInteractorFactory(
        extrinsicService: ExtrinsicService,
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        @ExtrinsicSerialization extrinsicGson: Gson,
        addressIconGenerator: AddressIconGenerator,
        signerProvider: SignerProvider,
        metadataShortenerService: MetadataShortenerService,
        signingContextFactory: SigningContext.Factory,
        signBytesChainResolver: SignBytesChainResolver
    ) = PolkadotSignInteractorFactory(
        extrinsicService = extrinsicService,
        chainRegistry = chainRegistry,
        accountRepository = accountRepository,
        extrinsicGson = extrinsicGson,
        addressIconGenerator = addressIconGenerator,
        signerProvider = signerProvider,
        metadataShortenerService = metadataShortenerService,
        signingContextFactory = signingContextFactory,
        signBytesChainResolver = signBytesChainResolver
    )
}
