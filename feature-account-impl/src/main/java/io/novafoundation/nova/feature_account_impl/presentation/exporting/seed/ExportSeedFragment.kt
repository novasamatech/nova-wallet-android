package io.novafoundation.nova.feature_account_impl.presentation.exporting.seed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ExportSeedFragment : ExportFragment<ExportSeedViewModel>() {

    companion object {
        fun getBundle(exportPayload: ExportPayload.ChainAccount): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, exportPayload)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_export_seed, container, false)
    }

    override fun initViews() {
        exportSeedToolbar.setHomeButtonListener { viewModel.back() }

        exportSeedToolbar.setRightActionClickListener { viewModel.optionsClicked() }

        exportSeedContentContainer.background = requireContext().getRoundedCornerDrawable(fillColorRes = R.color.input_background)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .exportSeedFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: ExportSeedViewModel) {
        super.subscribe(viewModel)

        viewModel.secretFlow.observe(exportSeedValue::setText)

        viewModel.secretTypeNameFlow.observe(exportSeedTitle::setText)
    }
}
