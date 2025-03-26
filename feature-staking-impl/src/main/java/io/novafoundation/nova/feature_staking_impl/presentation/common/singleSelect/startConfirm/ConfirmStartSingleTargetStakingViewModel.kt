package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.StarkingReturnableRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.startConfirm.ConfirmStartSingleTargetStakingViewModel.ConfirmStartSingleTargetStakingState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("LeakingThis")
abstract class ConfirmStartSingleTargetStakingViewModel<S : ConfirmStartSingleTargetStakingState>(
    stateFactory: ConfirmStartSingleTargetStakingState.Factory<S>,
    private val router: StarkingReturnableRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val assetUseCase: AssetUseCase,
    walletUiUseCase: WalletUiUseCase,
    private val payload: ConfirmStartSingleTargetStakingPayload,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    protected val state = stateFactory.create(scope = this)

    private val fee = mapFeeFromParcel(payload.fee)

    abstract val hintsMixin: HintsMixin

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    val currentAccountModelFlow = selectedAccountUseCase.selectedMetaAccountFlow().map {
        addressIconGenerator.createAccountAddressModel(
            chain = selectedAssetState.chain(),
            account = it,
            name = null
        )
    }.shareInBackground()

    val title = state.isStakeMoreFlow().map { isStakeMore ->
        if (isStakeMore) {
            resourceManager.getString(R.string.staking_bond_more_v1_9_0)
        } else {
            resourceManager.getString(R.string.staking_start_title)
        }
    }
        .shareInBackground()

    val amountModel = assetFlow.map { asset ->
        mapAmountToAmountModel(payload.amount, asset)
    }
        .shareInBackground()

    val feeLoaderMixin = feeLoaderMixinV2Factory.createDefault(viewModelScope, selectedAssetState.selectedAssetFlow())

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val collatorAddressModel = flowOf {
        state.collatorAddressModel(selectedAssetState.chain())
    }.shareInBackground()

    protected val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: StateFlow<Boolean> = _showNextProgress

    init {
        setInitialFee()
    }

    protected abstract suspend fun confirmClicked(
        fee: Fee,
        amount: Balance,
        asset: Asset
    )

    protected abstract suspend fun openStakeTargetInfo()

    fun confirmClicked() = launchUnit {
        confirmClicked(fee, payload.amount, assetFlow.first())
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launchUnit {
        val address = currentAccountModelFlow.first().address

        externalActions.showAddressActions(address, selectedAssetState.chain())
    }

    fun stakeTargetClicked() = launchUnit {
        openStakeTargetInfo()
    }

    private fun setInitialFee() = launch {
        feeLoaderMixin.setFee(fee)
    }

    interface ConfirmStartSingleTargetStakingState {

        fun interface Factory<S : ConfirmStartSingleTargetStakingState> {

            fun create(scope: ComputationalScope): S
        }

        fun isStakeMoreFlow(): Flow<Boolean>

        suspend fun collatorAddressModel(chain: Chain): AddressModel
    }
}
