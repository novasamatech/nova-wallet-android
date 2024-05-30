package io.novafoundation.nova.feature_account_impl.presentation.exporting.json

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.switchPasswordInputType
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_export_json_password.exportJsonPasswordConfirmField
import kotlinx.android.synthetic.main.fragment_export_json_password.exportJsonPasswordNewField
import kotlinx.android.synthetic.main.fragment_export_json_password.exportJsonPasswordNext
import kotlinx.android.synthetic.main.fragment_export_json_password.exportJsonPasswordToolbar

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ExportJsonFragment : ExportFragment<ExportJsonViewModel>() {

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

        exportJsonPasswordNext.prepareForProgress(viewLifecycleOwner)

        exportJsonPasswordNewField.setEndIconOnClickListener { viewModel.toggleShowPassword() }
        exportJsonPasswordConfirmField.setEndIconOnClickListener { viewModel.toggleShowPassword() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .exportJsonPasswordFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }

    override fun subscribe(viewModel: ExportJsonViewModel) {
        super.subscribe(viewModel)
        exportJsonPasswordNewField.content.bindTo(viewModel.passwordFlow, lifecycleScope)
        exportJsonPasswordConfirmField.content.bindTo(viewModel.passwordConfirmationFlow, lifecycleScope)

        viewModel.nextButtonState.observe(exportJsonPasswordNext::setState)

        viewModel.showPasswords.observe {
            exportJsonPasswordNewField.content.switchPasswordInputType(it)
            exportJsonPasswordConfirmField.content.switchPasswordInputType(it)
        }

        observeValidations(viewModel)
    }
}
