package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.dialog.errorDialog
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowAddress
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowContinue
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowHaveError
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowQr
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowTimer
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowToolbar

class ShowSignParitySignerFragment : BaseFragment<ShowSignParitySignerViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "ShowSignParitySignerFragment.Payload"

        fun getBundle(payload: ParitySignerSignInterScreenCommunicator.Request): Bundle {
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

        signParitySignerShowQr.background = requireContext().getRoundedCornerDrawable(fillColorRes = R.color.white)
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
        viewModel.qrCode.observe(signParitySignerShowQr::setImageBitmap)

        viewModel.addressModel.observe {
            signParitySignerShowAddress.setLabel(it.nameOrAddress)
            signParitySignerShowAddress.setMessage(it.address)
            signParitySignerShowAddress.setPrimaryIcon(it.image)
        }

        viewModel.validityPeriod.observe { validityPeriod ->
            signParitySignerShowTimer.startTimer(
                value = validityPeriod.period,
                customMessageFormat = R.string.account_parity_signer_sign_qr_code_valid_format,
                onTick = { view, _ ->
                    val textColorRes = if (validityPeriod.closeToExpire()) R.color.red else R.color.white_64

                    view.setTextColorRes(textColorRes)
                },
                onFinish = { view ->
                    viewModel.timerFinished()

                    view.setText(R.string.account_parity_signer_sign_qr_code_expired)
                }
            )
        }

        viewModel.acknowledgeExpired.awaitableActionLiveData.observeEvent {
            errorDialog(
                context = requireContext(),
                onConfirm = { it.onSuccess(Unit) }
            ) {
                setTitle(R.string.account_parity_signer_sign_qr_code_expired)
                setMessage(it.payload)
            }
        }
    }
}
