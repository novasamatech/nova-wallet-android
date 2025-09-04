package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalIdentity
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.domain.account.identity.LocalIdentityProvider
import io.novafoundation.nova.feature_account_impl.domain.account.identity.OnChainIdentityProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class IdentityProviderModule {

    @Provides
    @LocalIdentity
    fun provideLocalIdentityProvider(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry
    ): IdentityProvider {
        return LocalIdentityProvider(accountRepository, chainRegistry)
    }

    @Provides
    @OnChainIdentity
    fun provideOnChainIdentityProvider(
        onChainIdentityRepository: OnChainIdentityRepository
    ): IdentityProvider {
        return OnChainIdentityProvider(onChainIdentityRepository)
    }
}
