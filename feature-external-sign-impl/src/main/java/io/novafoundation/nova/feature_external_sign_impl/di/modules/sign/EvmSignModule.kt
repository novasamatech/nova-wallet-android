package io.novafoundation.nova.feature_external_sign_impl.di.modules.sign

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProviderFactory
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_external_sign_impl.data.evmApi.EvmApiFactory
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.evm.EvmSignInteractorFactory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import okhttp3.OkHttpClient

@Module
class EvmSignModule {

    @Provides
    @FeatureScope
    fun provideEthereumApiFactory(
        okHttpClient: OkHttpClient,
        chainRegistry: ChainRegistry,
        gasPriceProviderFactory: GasPriceProviderFactory,
        ): EvmApiFactory {
        return EvmApiFactory(okHttpClient, chainRegistry, gasPriceProviderFactory)
    }

    @Provides
    @FeatureScope
    fun provideSignInteractorFactory(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        tokenRepository: TokenRepository,
        @ExtrinsicSerialization extrinsicGson: Gson,
        addressIconGenerator: AddressIconGenerator,
        evmApiFactory: EvmApiFactory,
        signerProvider: SignerProvider,
        currencyRepository: CurrencyRepository
    ) = EvmSignInteractorFactory(
        chainRegistry = chainRegistry,
        accountRepository = accountRepository,
        signerProvider = signerProvider,
        tokenRepository = tokenRepository,
        currencyRepository = currencyRepository,
        extrinsicGson = extrinsicGson,
        addressIconGenerator = addressIconGenerator,
        evmApiFactory = evmApiFactory
    )
}
