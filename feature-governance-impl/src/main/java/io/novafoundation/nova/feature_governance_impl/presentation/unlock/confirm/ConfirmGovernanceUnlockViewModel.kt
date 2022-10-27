package io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class ConfirmGovernanceUnlockViewModel(
    private val router: GovernanceRouter,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val governanceSharedState: GovernanceSharedState,
    private val validationExecutor: ValidationExecutor
) : BaseViewModel(),
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions {

    // TODO val hintsMixin = hintsMixinFactory.create(coroutineScope = this, payload)

    // TODO()
    val walletModel: Flow<WalletModel> = flow { }

    // TODO()
    val addressModel: Flow<AddressModel> = flow { }

    // TODO()
    val showNextProgress: Flow<Boolean> = flow { }

    fun accountClicked() = launch {
        /*
        TODO
        val chain = governanceSharedState.chain()
        val address = chain.addressOf(accountId)
        val type = ExternalActions.Type.Address(address)
        externalActions.showExternalActions(type, chain)*/
    }

    fun confirmClicked() {
        TODO()
    }

    fun backClicked() {
        router.back()
    }
}
