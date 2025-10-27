package io.novafoundation.nova.feature_gift_impl.presentation.confirm

import android.view.View
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.presentation.showLoadingState
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.insets.ImeInsetsState
import io.novafoundation.nova.common.utils.insets.applySystemBarInsets
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_account_api.view.showWallet
import io.novafoundation.nova.feature_gift_api.di.GiftFeatureApi
import io.novafoundation.nova.feature_gift_impl.databinding.FragmentCreateGiftConfirmBinding
import io.novafoundation.nova.feature_gift_impl.di.GiftFeatureComponent
import io.novafoundation.nova.feature_gift_impl.presentation.amount.SelectGiftAmountViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class CreateGiftConfirmFragment : BaseFragment<CreateGiftConfirmViewModel, FragmentCreateGiftConfirmBinding>() {

    companion object : PayloadCreator<CreateGiftConfirmPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentCreateGiftConfirmBinding.inflate(layoutInflater)

    override fun applyInsets(rootView: View) {
        binder.root.applySystemBarInsets(imeInsets = ImeInsetsState.ENABLE_IF_SUPPORTED)
    }

    override fun initViews() {
        binder.confirmCreateGiftToolbar.setHomeButtonListener { viewModel.back() }

        binder.confirmCreateGiftButton.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GiftFeatureComponent>(this, GiftFeatureApi::class.java)
            .createGiftConfirmComponentFactory()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: CreateGiftConfirmViewModel) {
        setupExternalActions(viewModel)
        observeValidations(viewModel)
        viewModel.feeMixin.setupFeeLoading(binder.confirmCreateGiftFee)

        viewModel.giftAccountFlow.observe(binder.confirmCreateGiftAccount::showAddress)

        viewModel.confirmButtonStateLiveData.observe(binder.confirmCreateGiftButton::setState)

        viewModel.wallet.observe(binder.confirmCreateGiftWallet::showWallet)

        viewModel.chainModelFlow.observe { binder.confirmCreateGiftNetwork.showChain(it) }

        viewModel.totalAmountModel.observe(binder.confirmCreateGiftTotalAmount::showLoadingState)

        viewModel.giftAmountModel.observe(binder.confirmCreateGiftAmount::showAmount)
    }
}
