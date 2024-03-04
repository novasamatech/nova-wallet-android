package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.ExportJsonConfirmPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportFragment
import kotlinx.android.synthetic.main.fragment_export_json_confirm.exportJsonConfirmExport
import kotlinx.android.synthetic.main.fragment_export_json_confirm.exportJsonConfirmToolbar
import kotlinx.android.synthetic.main.fragment_export_json_confirm.exportJsonConfirmValue
import javax.inject.Inject

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ExportJsonConfirmFragment : ExportFragment<ExportJsonConfirmViewModel>() {

    @Inject
    lateinit var imageLoader: ImageLoader

    companion object {
        fun getBundle(payload: ExportJsonConfirmPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_export_json_confirm, container, false)
    }

    override fun initViews() {
        exportJsonConfirmToolbar.setHomeButtonListener { viewModel.back() }

        exportJsonConfirmToolbar.setRightActionClickListener { viewModel.optionsClicked() }

        exportJsonConfirmExport.setOnClickListener { viewModel.confirmClicked() }
    }

    override fun inject() {
        val payload = argument<ExportJsonConfirmPayload>(PAYLOAD_KEY)

        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .exportJsonConfirmFactory()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: ExportJsonConfirmViewModel) {
        super.subscribe(viewModel)

        exportJsonConfirmValue.setMessage(viewModel.json)
    }
}
