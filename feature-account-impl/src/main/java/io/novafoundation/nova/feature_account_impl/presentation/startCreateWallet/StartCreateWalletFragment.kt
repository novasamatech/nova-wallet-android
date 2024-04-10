package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import kotlinx.android.synthetic.main.fragment_start_create_wallet.startCreateWalletCloudBackupButton
import kotlinx.android.synthetic.main.fragment_start_create_wallet.startCreateWalletConfirmName
import kotlinx.android.synthetic.main.fragment_start_create_wallet.startCreateWalletExplanation
import kotlinx.android.synthetic.main.fragment_start_create_wallet.startCreateWalletManualBackupButton
import kotlinx.android.synthetic.main.fragment_start_create_wallet.startCreateWalletNameInput
import kotlinx.android.synthetic.main.fragment_start_create_wallet.startCreateWalletNameInputLayout
import kotlinx.android.synthetic.main.fragment_start_create_wallet.startCreateWalletTitle
import kotlinx.android.synthetic.main.fragment_start_create_wallet.startCreateWalletToolbar

class StartCreateWalletFragment : BaseFragment<StartCreateWalletViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_start_create_wallet, container, false)
    }

    override fun initViews() {
        startCreateWalletToolbar.applyStatusBarInsets()
        startCreateWalletToolbar.setHomeButtonListener { viewModel.backClicked() }

        startCreateWalletConfirmName.setOnClickListener { viewModel.confirmNameClicked() }

        onBackPressed { viewModel.backClicked() }

        startCreateWalletCloudBackupButton.setOnClickListener { viewModel.cloudBackupClicked() }
        startCreateWalletManualBackupButton.setOnClickListener { viewModel.manualBackupClicked() }

        startCreateWalletCloudBackupButton.prepareForProgress(viewLifecycleOwner)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(context!!, AccountFeatureApi::class.java)
            .startCreateWallet()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: StartCreateWalletViewModel) {
        observeActionBottomSheet(viewModel)
        startCreateWalletNameInput.bindTo(viewModel.nameInput, viewLifecycleOwner.lifecycleScope)

        viewModel.confirmNameButtonState.observe { state ->
            startCreateWalletConfirmName.setState(state)
        }

        viewModel.createWalletState.observe {
            startCreateWalletNameInputLayout.isEndIconVisible = it == CreateWalletState.SETUP_NAME
            startCreateWalletNameInput.isFocusable = it == CreateWalletState.SETUP_NAME
            startCreateWalletNameInput.isFocusableInTouchMode = it == CreateWalletState.SETUP_NAME

            startCreateWalletConfirmName.isVisible = it == CreateWalletState.SETUP_NAME
            startCreateWalletCloudBackupButton.isVisible = it == CreateWalletState.CHOOSE_BACKUP_WAY
            startCreateWalletManualBackupButton.isVisible = it == CreateWalletState.CHOOSE_BACKUP_WAY
        }

        viewModel.titleText.observe {
            startCreateWalletTitle.text = it
        }

        viewModel.explanationText.observe {
            startCreateWalletExplanation.text = it
        }

        viewModel.cloudBackupSyncProgressFlow.observe {
            startCreateWalletCloudBackupButton.showProgress(it)
            startCreateWalletManualBackupButton.isEnabled = !it
        }
    }
}
