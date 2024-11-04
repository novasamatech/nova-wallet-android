package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.databinding.FragmentConfirmMnemonicBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.common.mnemonic.BackupMnemonicAdapter

class ConfirmMnemonicFragment : BaseFragment<ConfirmMnemonicViewModel, FragmentConfirmMnemonicBinding>() {

    companion object {
        private const val KEY_PAYLOAD = "confirm_payload"

        fun getBundle(payload: ConfirmMnemonicPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentConfirmMnemonicBinding.inflate(layoutInflater)

    private val sourceAdapter by lazy(LazyThreadSafetyMode.NONE) {
        BackupMnemonicAdapter(itemHandler = viewModel::sourceWordClicked)
    }

    override fun initViews() {
        binder.confirmMnemonicToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }
        binder.confirmMnemonicToolbar.setRightActionClickListener { viewModel.reset() }

        binder.conformMnemonicSkip.setOnClickListener { viewModel.skipClicked() }
        binder.conformMnemonicContinue.setOnClickListener { viewModel.continueClicked() }

        binder.confirmMnemonicSource.adapter = sourceAdapter
        binder.confirmMnemonicDestination.setWordClickedListener(viewModel::destinationWordClicked)
    }

    override fun inject() {
        val payload = argument<ConfirmMnemonicPayload>(KEY_PAYLOAD)

        FeatureUtils.getFeature<AccountFeatureComponent>(context!!, AccountFeatureApi::class.java)
            .confirmMnemonicComponentFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ConfirmMnemonicViewModel) {
        binder.conformMnemonicSkip.setVisible(viewModel.skipVisible)

        viewModel.sourceWords.observe { sourceAdapter.submitList(it) }
        viewModel.destinationWords.observe { binder.confirmMnemonicDestination.setWords(it) }

        viewModel.nextButtonState.observe {
            binder.conformMnemonicContinue.setState(it)
        }
    }
}
