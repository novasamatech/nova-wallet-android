package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.isAddressValid
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.watchOnly.change.ChangeWatchAccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangeWatchAccountViewModel(
    private val router: AccountRouter,
    private val addressInputMixinFactory: AddressInputMixinFactory,
    private val chainRegistry: ChainRegistry,
    private val interactor: ChangeWatchAccountInteractor,
    private val payload: AddAccountPayload.ChainAccount,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val chain = flowOf { chainRegistry.getChain(payload.chainId) }
        .shareInBackground()

    val chainAddressMixin = with(addressInputMixinFactory) {
        create(
            inputSpecProvider = singleChainInputSpec(chain),
            errorDisplayer = this@ChangeWatchAccountViewModel::showError,
            showAccountEvent = null,
            coroutineScope = this@ChangeWatchAccountViewModel,
        )
    }

    val inputHint = chain.map { it.name }.map {
        resourceManager.getString(R.string.account_chain_address_format, it)
    }.shareInBackground()

    val buttonState = chainAddressMixin.inputFlow.map { chainAddress ->
        if (chainAddressMixin.isAddressValid(chainAddress)) {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        } else {
            val chainName = chain.first().name
            val reason = resourceManager.getString(R.string.accoount_enter_chain_address_format, chainName)
            DescriptiveButtonState.Disabled(reason)
        }
    }

    fun nextClicked() = launch {
        val result = withContext(Dispatchers.Default) {
            interactor.changeChainAccount(
                metaId = payload.metaId,
                chain = chain.first(),
                address = chainAddressMixin.inputFlow.first()
            )
        }

        result
            .onSuccess { router.openMain() }
            .onFailure(::showError)
    }

    fun backClicked() {
        router.back()
    }
}
