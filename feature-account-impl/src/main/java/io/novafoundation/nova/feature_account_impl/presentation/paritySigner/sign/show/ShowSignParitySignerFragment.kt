package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common.setupQrCodeExpiration
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowAddress
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowContinue
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowHaveError
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowQr
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowTimer
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowToolbar
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerSignLabel

class ShowSignParitySignerFragment : BaseFragment<ShowSignParitySignerViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "ShowSignParitySignerFragment.Payload"

        fun getBundle(payload: ShowSignParitySignerPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_parity_signer_show, container, false)
    }

    override fun initViews() {
        setupExternalActions(viewModel)

        onBackPressed { viewModel.backClicked() }

        signParitySignerShowToolbar.applyStatusBarInsets()
        signParitySignerShowToolbar.setHomeButtonListener { viewModel.backClicked() }

        signParitySignerShowQr.background = requireContext().getRoundedCornerDrawable(fillColorRes = R.color.qr_code_background)
        signParitySignerShowQr.clipToOutline = true // for round corners

        signParitySignerShowAddress.setWholeClickListener { viewModel.addressClicked() }

        signParitySignerShowHaveError.setOnClickListener { viewModel.troublesClicked() }
        signParitySignerShowContinue.setOnClickListener { viewModel.continueClicked() }
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
            timerView = signParitySignerShowTimer,
            onTimerFinished = viewModel::timerFinished
        )

        viewModel.qrCode.observe(signParitySignerShowQr::setImageBitmap)

        viewModel.addressModel.observe {
            signParitySignerShowAddress.setLabel(it.nameOrAddress)
            signParitySignerShowAddress.setMessage(it.address)
            signParitySignerShowAddress.setPrimaryIcon(it.image)
        }

        signParitySignerShowToolbar.setTitle(viewModel.title)
        signParitySignerSignLabel.text = viewModel.signLabel
        signParitySignerShowHaveError.text = viewModel.errorButtonLabel
    }
}
