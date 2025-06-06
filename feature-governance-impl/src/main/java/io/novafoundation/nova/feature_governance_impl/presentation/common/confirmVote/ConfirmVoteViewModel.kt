package io.novafoundation.nova.feature_governance_impl.presentation.common.confirmVote

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.model.LocksChangeModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.YourMultiVoteModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

abstract class ConfirmVoteViewModel(
    private val router: GovernanceRouter,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val governanceSharedState: GovernanceSharedState,
    private val hintsMixinFactory: ReferendumVoteHintsMixinFactory,
    private val walletUiUseCase: WalletUiUseCase,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val assetUseCase: AssetUseCase,
    private val validationExecutor: ValidationExecutor
) : BaseViewModel(),
    Validatable by validationExecutor,
    WithFeeLoaderMixin,
    ExternalActions by externalActions {

    abstract val titleFlow: Flow<String>

    abstract val amountModelFlow: Flow<AmountModel>

    abstract val locksChangeUiFlow: Flow<LocksChangeModel>

    abstract val accountVoteUi: Flow<YourMultiVoteModel?>

    protected val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    override val originFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(assetFlow)

    val hintsMixin = hintsMixinFactory.create(scope = this)

    val walletModel: Flow<WalletModel> = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val currentAddressModelFlow = selectedAccountUseCase.selectedMetaAccountFlow().map { metaAccount ->
        val chain = governanceSharedState.chain()

        addressIconGenerator.createAccountAddressModel(chain, metaAccount)
    }.shareInBackground()

    protected val _showNextProgress = MutableStateFlow(false)

    val showNextProgress: Flow<Boolean> = _showNextProgress

    fun accountClicked() = launch {
        val addressModel = currentAddressModelFlow.first()

        externalActions.showAddressActions(addressModel.address, governanceSharedState.chain())
    }

    abstract fun confirmClicked()

    fun backClicked() {
        router.back()
    }
}
