package io.novafoundation.nova.feature_account_api.di

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions

interface AccountFeatureApi {

    fun provideAccountRepository(): AccountRepository

    fun externalAccountActions(): ExternalActions.Presentation

    fun accountUpdateScope(): AccountUpdateScope

    fun addressDisplayUseCase(): AddressDisplayUseCase

    fun accountUseCase(): SelectedAccountUseCase

    fun extrinsicService(): ExtrinsicService
}
