package io.novafoundation.nova.feature_wallet_connect_impl.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.caip.caip2.Caip2Parser
import io.novafoundation.nova.caip.caip2.Caip2Resolver
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.core_db.dao.WalletConnectSessionsDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_external_sign_api.domain.sign.evm.EvmTypedMessageParser
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.feature_wallet_connect_impl.data.repository.RealWalletConnectSessionRepository
import io.novafoundation.nova.feature_wallet_connect_impl.data.repository.WalletConnectSessionRepository
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.RealWalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.CompoundWalletConnectRequestFactory
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.WalletConnectRequest
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.evm.EvmWalletConnectRequestFactory
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.polkadot.PolkadotWalletConnectRequestFactory
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.service.RealWalletConnectServiceFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class WalletConnectFeatureModule {

    @Provides
    @FeatureScope
    fun providePolkadotRequestFactory(gson: Gson): PolkadotWalletConnectRequestFactory {
        return PolkadotWalletConnectRequestFactory(gson)
    }

    @Provides
    @FeatureScope
    fun provideEvmRequestFactory(
        gson: Gson,
        caip2Parser: Caip2Parser,
        typedMessageParser: EvmTypedMessageParser,
    ): EvmWalletConnectRequestFactory {
        return EvmWalletConnectRequestFactory(gson, caip2Parser, typedMessageParser)
    }

    @Provides
    @FeatureScope
    fun provideRequestFactory(
        polkadotFactory: PolkadotWalletConnectRequestFactory,
        evmFactory: EvmWalletConnectRequestFactory,
    ): WalletConnectRequest.Factory {
        return CompoundWalletConnectRequestFactory(polkadotFactory, evmFactory)
    }

    @Provides
    @FeatureScope
    fun provideSessionRepository(dao: WalletConnectSessionsDao): WalletConnectSessionRepository = RealWalletConnectSessionRepository(dao)

    @Provides
    @FeatureScope
    fun provideInteractor(
        chainRegistry: ChainRegistry,
        caip2Resolver: Caip2Resolver,
        requestFactory: WalletConnectRequest.Factory,
        walletConnectSessionRepository: WalletConnectSessionRepository,
        accountRepository: AccountRepository
    ): WalletConnectSessionInteractor = RealWalletConnectSessionInteractor(
        chainRegistry = chainRegistry,
        caip2Resolver = caip2Resolver,
        walletConnectRequestFactory = requestFactory,
        walletConnectSessionRepository = walletConnectSessionRepository,
        accountRepository = accountRepository
    )

    @Provides
    @FeatureScope
    fun provideWalletConnectServiceFactory(
        awaitableMixinFactory: ActionAwaitableMixin.Factory,
        walletUiUseCase: WalletUiUseCase,
        interactor: WalletConnectSessionInteractor,
        dAppSignRequester: ExternalSignCommunicator,
        selectedAccountUseCase: SelectedAccountUseCase
    ): WalletConnectService.Factory {
        return RealWalletConnectServiceFactory(
            awaitableMixinFactory = awaitableMixinFactory,
            walletUiUseCase = walletUiUseCase,
            interactor = interactor,
            dAppSignRequester = dAppSignRequester,
            selectedAccountUseCase = selectedAccountUseCase
        )
    }
}
