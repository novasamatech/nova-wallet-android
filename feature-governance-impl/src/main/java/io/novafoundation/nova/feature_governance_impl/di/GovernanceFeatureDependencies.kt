package io.novafoundation.nova.feature_governance_impl.di

import coil.ImageLoader
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface GovernanceFeatureDependencies {

    val chainRegistry: ChainRegistry

    val imageLoader: ImageLoader

    val addressIconGenerator: AddressIconGenerator

    val resourceManager: ResourceManager

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val tokenRepository: TokenRepository

    val accountRepository: AccountRepository

    val selectedAccountUseCase: SelectedAccountUseCase
}
