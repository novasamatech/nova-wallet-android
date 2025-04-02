package io.novafoundation.nova.feature_buy_impl.di

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface BuyFeatureDependencies {

    val chainRegistry: ChainRegistry

    val accountUseCase: SelectedAccountUseCase

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
}
