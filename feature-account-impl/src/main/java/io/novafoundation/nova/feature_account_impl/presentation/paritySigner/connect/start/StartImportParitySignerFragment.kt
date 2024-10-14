package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.InstructionStepView
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfig.Connect.Instruction
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerStartPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start.view.InstructionImageView

class StartImportParitySignerFragment : BaseFragment<StartImportParitySignerViewModel, FragmentImportParitySignerStartBinding>() {

    companion object {

        private const val PAYLOAD_KEY = "StartImportParitySignerFragment.Payload"

        fun getBundle(payload: ParitySignerStartPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override val binder by viewBinding(FragmentImportParitySignerStartBinding::bind)

    override fun initViews() {
        binder.startImportParitySignerToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.startImportParitySignerToolbar.applyStatusBarInsets()

        binder.startImportParitySignerScanQrCode.setOnClickListener { viewModel.scanQrCodeClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .startImportParitySignerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: StartImportParitySignerViewModel) {
        viewModel.instructions.forEach(::showInstruction)

        binder.startImportParitySignerTitle.text = viewModel.title
        binder.startImportParitySignerConnectOverview.setTargetImage(viewModel.polkadotVaultVariantIcon)
    }

    private fun showInstruction(instruction: Instruction) {
        when (instruction) {
            is Instruction.Image -> showImageInstruction(instruction)
            is Instruction.Step -> showStepInstruction(instruction)
        }
    }

    private fun showStepInstruction(step: Instruction.Step) {
        val view = InstructionStepView(requireContext()).apply {
            layoutParams = MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(0, 24.dp, 0, 0)
            }

            setStepNumber(step.index)
            setStepText(step.content)
        }

        binder.startImportParitySignerInstructionContainer.addView(view)
    }

    private fun showImageInstruction(step: Instruction.Image) {
        val view = InstructionImageView.createWithDefaultLayoutParams(requireContext()).apply {
            setModel(step)
        }

        binder.startImportParitySignerInstructionContainer.addView(view)
    }
}
