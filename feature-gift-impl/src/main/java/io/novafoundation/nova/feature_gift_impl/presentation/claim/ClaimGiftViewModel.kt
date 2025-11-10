package io.novafoundation.nova.feature_gift_impl.presentation.claim

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.domain.filter.selectAddress.SelectAccountFilter
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.isControllableWallet
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_account_api.presenatation.common.mapMetaAccountTypeToNameRes
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletMixin
import io.novafoundation.nova.feature_account_api.view.AccountView
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.domain.ClaimGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.common.claim.ClaimGiftMixinFactory
import io.novafoundation.nova.feature_gift_impl.presentation.common.UnpackingGiftAnimationFactory
import io.novafoundation.nova.feature_gift_impl.presentation.common.claim.ClaimGiftException
import io.novafoundation.nova.feature_gift_impl.presentation.share.model.GiftAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ClaimGiftViewModel(
    private val router: GiftRouter,
    private val payload: ClaimGiftPayload,
    private val claimGiftInteractor: ClaimGiftInteractor,
    private val chainRegistry: ChainRegistry,
    private val unpackingGiftAnimationFactory: UnpackingGiftAnimationFactory,
    private val assetIconProvider: AssetIconProvider,
    private val tokenFormatter: TokenFormatter,
    private val resourceManager: ResourceManager,
    private val walletUiUseCase: WalletUiUseCase,
    private val claimGiftMixinFactory: ClaimGiftMixinFactory,
    private val accountInteractor: AccountInteractor,
    selectSingleWalletMixin: SelectSingleWalletMixin.Factory,
) : BaseViewModel() {

    private val giftFlow = flowOf { claimGiftInteractor.getClaimableGift(payload.secret, payload.chainId, payload.assetId) }
        .shareInBackground()

    private val metaIdToClaimGiftFlow = MutableStateFlow<Long?>(null)

    private val metaAccountToClaimGiftFlow = metaIdToClaimGiftFlow.filterNotNull()
        .map { claimGiftInteractor.getMetaAccount(it) }
        .shareInBackground()

    private val tempMetaAccountFlow = giftFlow.map { claimGiftInteractor.createTempMetaAccount(it) }
        .shareInBackground()

    private val giftAmountWithFee = combine(giftFlow, tempMetaAccountFlow) { gift, metaAccount ->
        claimGiftInteractor.getGiftAmountWithFee(gift, metaAccount, coroutineScope)
    }.shareInBackground()

    val selectedWalletModel = combine(giftFlow, metaAccountToClaimGiftFlow) { gift, metaAccountToClaimGift ->
        val addressModel = walletUiUseCase.walletAddressModelOrNull(
            metaAccountToClaimGift,
            gift.chain,
            AddressIconGenerator.SIZE_MEDIUM
        )

        addressModel.asAccountViewModelOrNoAddress(metaAccountToClaimGift, gift.chain)
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

    private val selectWalletPayloadFlow = giftFlow.map {
        SelectSingleWalletMixin.Payload(
            chain = it.chain,
            filter = SelectAccountFilter.ControllableWallets()
        )
    }
    val selectWalletMixin = selectSingleWalletMixin.create(
        coroutineScope = this,
        payloadFlow = selectWalletPayloadFlow,
        onWalletSelect = ::onWalletSelect
    )

    val alertModelFlow = combine(giftFlow, metaAccountToClaimGiftFlow) { gift, metaAccount ->
        when {
            !metaAccount.type.isControllableWallet() -> {
                val metaAccountTypeName = resourceManager.getString(metaAccount.type.mapMetaAccountTypeToNameRes())
                AlertModel(
                    style = AlertView.Style.fromPreset(AlertView.StylePreset.WARNING),
                    message = resourceManager.getString(
                        R.string.claim_gift_uncontrollable_wallet_title,
                        metaAccountTypeName.lowercase()
                    ),
                    subMessages = listOf(resourceManager.getString(R.string.claim_gift_uncontrollable_wallet_message)),
                    linkAction = AlertModel.ActionModel(
                        text = resourceManager.getString(R.string.common_manage_wallets),
                        listener = ::manageWallets
                    ),
                )
            }

            !metaAccount.hasAccountIn(gift.chain) -> AlertModel(
                style = AlertView.Style.fromPreset(AlertView.StylePreset.WARNING),
                message = resourceManager.getString(R.string.claim_gift_no_account_alert_title, gift.chain.name),
                subMessages = listOf(),
            )

            else -> null
        }
    }

    private val claimGiftMixin = claimGiftMixinFactory.create(this)

    val confirmButtonStateFlow = combine(
        claimGiftMixin.claimingInProgressFlow,
        giftFlow,
        metaAccountToClaimGiftFlow
    ) { claimingInProgress, giftFlow, claimMetaAccount ->
        when {
            claimingInProgress -> DescriptiveButtonState.Loading

            !claimMetaAccount.type.isControllableWallet() -> DescriptiveButtonState.Gone

            !claimMetaAccount.hasAccountIn(giftFlow.chain) -> {
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.account_select_wallet))
            }

            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.claim_gift_button))
        }
    }

    init {
        launch {
            val metaAccount = claimGiftInteractor.getMetaAccountToClaimGift()
            metaIdToClaimGiftFlow.value = metaAccount.id
        }
    }

    fun back() {
        router.back()
    }

    fun claimGift() = launchUnit {
        val gift = giftFlow.first()
        val amountWithFee = giftAmountWithFee.first()
        val tempMetaAccount = tempMetaAccountFlow.first()
        val metaAccountToClaimGift = metaAccountToClaimGiftFlow.first()

        claimGiftMixin.claimGift(
            gift = gift,
            amountWithFee = amountWithFee,
            giftMetaAccount = tempMetaAccount,
            giftRecipient = metaAccountToClaimGift
        )
            .onSuccess {
                val metaAccountToClaimGift = metaAccountToClaimGiftFlow.first()
                accountInteractor.selectMetaAccount(metaAccountToClaimGift.id)
                _giftClaimedEvent.value = Unit.event()
            }
            .onFailure {
                when (it as ClaimGiftException) {
                    is ClaimGiftException.GiftAlreadyClaimed -> showError(
                        resourceManager.getString(R.string.claim_gift_already_claimed_title),
                        resourceManager.getString(R.string.claim_gift_already_claimed_message)
                    )

                    is ClaimGiftException.UnknownError -> showError(
                        resourceManager.getString(R.string.claim_gift_default_error_title),
                        resourceManager.getString(R.string.claim_gift_default_error_message)
                    )
                }
            }
    }

    fun onGiftClaimAnimationFinished() = launchUnit {
        showToast(resourceManager.getString(R.string.claim_gift_success_message))

        router.openMainScreen()
    }

    fun selectWalletToClaim() {
        launch {
            val selectedMetaAccount = metaAccountToClaimGiftFlow.first()
            selectWalletMixin.openSelectWallet(selectedMetaAccount.id)
        }
    }

    private fun onWalletSelect(metaId: Long) {
        metaIdToClaimGiftFlow.value = metaId
    }

    private fun manageWallets() = launchUnit {
        router.openManageWallets()
    }

    private fun AddressModel?.asAccountViewModelOrNoAddress(
        metaAccountToClaimGift: MetaAccount,
        chain: Chain
    ): AccountView.Model {
        return this?.let { AccountView.Model.Address(it) }
            ?: AccountView.Model.NoAddress(
                metaAccountToClaimGift.name,
                resourceManager.getString(R.string.account_chain_not_found, chain.name)
            )
    }
}
