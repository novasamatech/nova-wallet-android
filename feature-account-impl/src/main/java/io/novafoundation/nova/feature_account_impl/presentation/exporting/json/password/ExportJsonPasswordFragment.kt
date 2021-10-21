package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.password

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import kotlinx.android.synthetic.main.fragment_export_json_password.exportJsonPasswordConfirmField
import kotlinx.android.synthetic.main.fragment_export_json_password.exportJsonPasswordMatchingError
import kotlinx.android.synthetic.main.fragment_export_json_password.exportJsonPasswordNetworkInput
import kotlinx.android.synthetic.main.fragment_export_json_password.exportJsonPasswordNewField
import kotlinx.android.synthetic.main.fragment_export_json_password.exportJsonPasswordNext
import kotlinx.android.synthetic.main.fragment_export_json_password.exportJsonPasswordToolbar
import javax.inject.Inject

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ExportJsonPasswordFragment : BaseFragment<ExportJsonPasswordViewModel>() {

    @Inject
    lateinit var imageLoader: ImageLoader

    companion object {
        fun getBundle(exportPayload: ExportPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, exportPayload)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_export_json_password, container, false)
    }

    override fun initViews() {
        exportJsonPasswordToolbar.setHomeButtonListener { viewModel.back() }

        exportJsonPasswordNext.setOnClickListener { viewModel.nextClicked() }

        exportJsonPasswordMatchingError.setDrawableStart(R.drawable.ic_red_cross, widthInDp = 24, paddingInDp = 8)

        exportJsonPasswordNetworkInput.isEnabled = false
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .exportJsonPasswordFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: ExportJsonPasswordViewModel) {
        exportJsonPasswordNewField.content.bindTo(viewModel.passwordLiveData)
        exportJsonPasswordConfirmField.content.bindTo(viewModel.passwordConfirmationLiveData)

        viewModel.nextEnabled.observe(exportJsonPasswordNext::setEnabled)

        viewModel.showDoNotMatchingErrorLiveData.observe {
            exportJsonPasswordMatchingError.setVisible(it, falseState = View.INVISIBLE)
        }

        viewModel.chainFlow.observe {
            exportJsonPasswordNetworkInput.textIconView.load(it.icon, imageLoader)
            exportJsonPasswordNetworkInput.setMessage(it.name)
        }
    }
}
