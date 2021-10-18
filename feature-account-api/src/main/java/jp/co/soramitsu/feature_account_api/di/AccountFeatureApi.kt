package jp.co.soramitsu.feature_account_api.di

import jp.co.soramitsu.feature_account_api.data.extrinsic.ExtrinsicService
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountRepository
import jp.co.soramitsu.feature_account_api.domain.interfaces.SelectedAccountUseCase
import jp.co.soramitsu.feature_account_api.domain.updaters.AccountUpdateScope
import jp.co.soramitsu.feature_account_api.presenatation.account.AddressDisplayUseCase
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions

interface AccountFeatureApi {

    fun provideAccountRepository(): AccountRepository

    fun externalAccountActions(): ExternalActions.Presentation

    fun accountUpdateScope(): AccountUpdateScope

    fun addressDisplayUseCase(): AddressDisplayUseCase

    fun accountUseCase(): SelectedAccountUseCase

    fun extrinsicService(): ExtrinsicService
}
