package io.novafoundation.nova.feature_account_impl.presentation.exporting.seed

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.FragmentExportSeedBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ExportSeedFragment : ExportFragment<ExportSeedViewModel, FragmentExportSeedBinding>() {

    companion object {
        fun getBundle(exportPayload: ExportPayload.ChainAccount): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, exportPayload)
            }
        }
    }

    override val binder by viewBinding(FragmentExportSeedBinding::bind)

    override fun initViews() {
        binder.exportSeedToolbar.setHomeButtonListener { viewModel.back() }

        binder.exportSeedToolbar.setRightActionClickListener { viewModel.optionsClicked() }

        binder.exportSeedContentContainer.background = requireContext().getRoundedCornerDrawable(fillColorRes = R.color.input_background)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .exportSeedFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: ExportSeedViewModel) {
        super.subscribe(viewModel)

        viewModel.secretFlow.observe(binder.exportSeedValue::setText)

        viewModel.secretTypeNameFlow.observe(binder.exportSeedTitle::setText)
    }
}
