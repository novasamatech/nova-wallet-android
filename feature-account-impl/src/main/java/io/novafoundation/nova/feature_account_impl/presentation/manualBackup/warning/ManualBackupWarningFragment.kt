package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.warning

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.condition.setupConditions
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload

class ManualBackupWarningFragment : BaseFragment<ManualBackupWarningViewModel>() {

    companion object {

        private const val KEY_PAYLOAD = "payload"

        fun bundle(payload: ManualBackupCommonPayload) = Bundle().apply {
            putParcelable(KEY_PAYLOAD, payload)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_manual_backup_warning, container, false)
    }

    override fun initViews() {
        manualBackupWarningToolbar.applyStatusBarInsets()
        manualBackupWarningToolbar.setHomeButtonListener { viewModel.backClicked() }

        manualBackupWarningButtonContinue.setOnClickListener { viewModel.continueClicked() }

        buildConditions()
    }

    private fun buildConditions() {
        manualBackupWarningCondition1.setText(
            buildCondition(R.string.backup_secrets_warning_condition_1, R.string.backup_secrets_warning_condition_1_highlight)
        )
        manualBackupWarningCondition2.setText(
            buildCondition(R.string.backup_secrets_warning_condition_2, R.string.backup_secrets_warning_condition_2_highlight)
        )
        manualBackupWarningCondition3.setText(
            buildCondition(R.string.backup_secrets_warning_condition_3, R.string.backup_secrets_warning_condition_3_highlight)
        )

        viewModel.conditionMixin.setupConditions(
            manualBackupWarningCondition1,
            manualBackupWarningCondition2,
            manualBackupWarningCondition3
        )
    }

    private fun buildCondition(termBaseResId: Int, termHighlightResId: Int): CharSequence {
        return SpannableFormatter.format(
            getString(termBaseResId),
            getString(termHighlightResId)
                .toSpannable(colorSpan(requireContext().getColor(R.color.text_primary)))
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .manualBackupWarning()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ManualBackupWarningViewModel) {
        viewModel.buttonState.observe {
            manualBackupWarningButtonContinue.setState(it)
        }
    }
}
