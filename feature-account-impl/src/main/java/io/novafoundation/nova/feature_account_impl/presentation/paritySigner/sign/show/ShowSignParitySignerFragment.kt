package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show

import android.os.Bundle

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setSequence
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentSignParitySignerShowBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common.setupQrCodeExpiration

class ShowSignParitySignerFragment : BaseFragment<ShowSignParitySignerViewModel, FragmentSignParitySignerShowBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "ShowSignParitySignerFragment.Payload"

        fun getBundle(payload: ShowSignParitySignerPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun createBinding() = FragmentSignParitySignerShowBinding.inflate(layoutInflater)

    override fun initViews() {
        setupExternalActions(viewModel)

        onBackPressed { viewModel.backClicked() }

        binder.signParitySignerShowToolbar.applyStatusBarInsets()
        binder.signParitySignerShowToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.signParitySignerShowQr.background = requireContext().getRoundedCornerDrawable(fillColorRes = R.color.qr_code_background)
        binder.signParitySignerShowQr.clipToOutline = true // for round corners

        binder.signParitySignerShowAddress.setWholeClickListener { viewModel.addressClicked() }

        binder.signParitySignerShowHaveError.setOnClickListener { viewModel.troublesClicked() }
        binder.signParitySignerShowContinue.setOnClickListener { viewModel.continueClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .showSignParitySignerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: ShowSignParitySignerViewModel) {
        setupQrCodeExpiration(
            validityPeriodFlow = viewModel.validityPeriod,
            qrCodeExpiredPresentable = viewModel.qrCodeExpiredPresentable,
            timerView = binder.signParitySignerShowTimer,
            onTimerFinished = viewModel::timerFinished
        )

        viewModel.qrCodeSequence.observe(binder.signParitySignerShowQr::setSequence)

        viewModel.addressModel.observe {
            binder.signParitySignerShowAddress.setLabel(it.nameOrAddress)
            binder.signParitySignerShowAddress.setMessage(it.address)
            binder.signParitySignerShowAddress.setPrimaryIcon(it.image)
        }

        binder.signParitySignerShowToolbar.setTitle(viewModel.title)
        binder.signParitySignerShowHaveError.text = viewModel.errorButtonLabel

        setupModeSwitcher(viewModel)
    }

    private fun setupModeSwitcher(viewModel: ShowSignParitySignerViewModel) {
        binder.signParitySignerShowMode.setVisible(viewModel.supportsMultipleSigningModes)

        if (!viewModel.supportsMultipleSigningModes) return

        initTabs()

        binder.signParitySignerShowMode bindTo viewModel.selectedSigningModeIndex
    }

    private fun initTabs() = with(binder.signParitySignerShowMode) {
        addTab(newTab().setText(R.string.account_parity_signer_show_mode_new))
        addTab(newTab().setText(R.string.account_parity_signer_show_mode_legacy))
    }
}
