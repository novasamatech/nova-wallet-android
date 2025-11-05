package io.novafoundation.nova.feature_gift_impl.presentation.claim

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.domain.ClaimGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.common.UnpackingGiftAnimationFactory
import io.novafoundation.nova.feature_gift_impl.presentation.share.model.GiftAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ClaimGiftViewModel(
    private val router: GiftRouter,
    private val payload: ClaimGiftPayload,
    private val claimGiftInteractor: ClaimGiftInteractor,
    private val chainRegistry: ChainRegistry,
    private val unpackingGiftAnimationFactory: UnpackingGiftAnimationFactory,
    private val assetIconProvider: AssetIconProvider,
    private val tokenFormatter: TokenFormatter,
    private val resourceManager: ResourceManager,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val walletUiUseCase: WalletUiUseCase
) : BaseViewModel() {

    private val giftFlow = flowOf { claimGiftInteractor.getClaimableGift(payload.secret, payload.chainId, payload.assetId) }
        .shareInBackground()

    private val selectedMetaAccountFlow = selectedAccountUseCase.selectedMetaAccountFlow()
        .shareInBackground()

    private val tempMetaAccountFlow = giftFlow.map { claimGiftInteractor.createTempMetaAccount(it) }
        .shareInBackground()

    private val giftAmountWithFee = combine(giftFlow, tempMetaAccountFlow) { gift, metaAccount ->
        claimGiftInteractor.getGiftAmountWithFee(gift, metaAccount, coroutineScope)
    }.shareInBackground()

    val selectedWalletModel = combine(giftFlow, selectedMetaAccountFlow) { gift, selectedMetaAccount ->
        walletUiUseCase.walletAddressModel(selectedMetaAccount, gift.chain, AddressIconGenerator.SIZE_MEDIUM)
    }

    val amountModel = combine(giftFlow, giftAmountWithFee) { gift, giftAmountWithFee ->
        val tokenIcon = assetIconProvider.getAssetIconOrFallback(gift.chainAsset)
        val giftAmount = tokenFormatter.formatToken(giftAmountWithFee.amount, gift.chainAsset.symbol)
        GiftAmountModel(tokenIcon, giftAmount)
    }

    private val _giftClaimedEvent = MutableLiveData<Event<Unit>>()
    val giftClaimedEvent: LiveData<Event<Unit>> = _giftClaimedEvent

    val giftAnimationRes = giftFlow.map {
        val chainAsset = chainRegistry.asset(it.chain.id, it.chainAsset.id)
        unpackingGiftAnimationFactory.getAnimationForAsset(chainAsset.symbol)
    }.distinctUntilChanged()
        .shareInBackground()

    private val claimingInProgressFlow = MutableStateFlow(false)
    val confirmButtonStateFlow = claimingInProgressFlow.map { claimingInProgress ->
        if (claimingInProgress) {
            DescriptiveButtonState.Loading
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.claim_gift_button))
        }
    }

    fun back() {
        router.back()
    }

    fun claimGift() = launchUnit {
        claimingInProgressFlow.value = true

        val gift = giftFlow.first()

        if (claimGiftInteractor.isGiftAlreadyClaimed(gift)) {
            showError(
                resourceManager.getString(R.string.claim_gift_already_claimed_title),
                resourceManager.getString(R.string.claim_gift_already_claimed_message)
            )

            claimingInProgressFlow.value = false

            return@launchUnit
        }

        val amountWithFee = giftAmountWithFee.first()
        val tempMetaAccount = tempMetaAccountFlow.first()
        claimGiftInteractor.claimGift(gift, amountWithFee, tempMetaAccount, coroutineScope)
            .onSuccess {
                _giftClaimedEvent.value = Unit.event()
            }
            .onFailure {
                claimingInProgressFlow.value = false

                showError(
                    resourceManager.getString(R.string.claim_gift_default_error_title),
                    resourceManager.getString(R.string.claim_gift_default_error_message)
                )
            }
    }

    fun onGiftClaimAnimationFinished() {
        showToast(resourceManager.getString(R.string.claim_gift_success_message))

        router.openMainScreen()
    }
}
