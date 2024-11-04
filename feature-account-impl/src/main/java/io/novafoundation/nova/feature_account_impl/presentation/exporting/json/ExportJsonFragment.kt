package io.novafoundation.nova.feature_account_impl.presentation.exporting.json

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.switchPasswordInputType
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.databinding.FragmentExportJsonPasswordBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import javax.inject.Inject

private const val PAYLOAD_KEY = "PAYLOAD_KEY"

class ExportJsonFragment : ExportFragment<ExportJsonViewModel, FragmentExportJsonPasswordBinding>() {

    override val binder by viewBinding(FragmentExportJsonPasswordBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    companion object {
        fun getBundle(exportPayload: ExportPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, exportPayload)
            }
        }
    }

    override fun initViews() {
        binder.exportJsonPasswordToolbar.setHomeButtonListener { viewModel.back() }

        binder.exportJsonPasswordNext.setOnClickListener { viewModel.nextClicked() }

        binder.exportJsonPasswordNext.prepareForProgress(viewLifecycleOwner)

        binder.exportJsonPasswordNewField.setEndIconOnClickListener { viewModel.toggleShowPassword() }
        binder.exportJsonPasswordConfirmField.setEndIconOnClickListener { viewModel.toggleShowPassword() }
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
        binder.exportJsonPasswordNewField.content.bindTo(viewModel.passwordFlow, lifecycleScope)
        binder.exportJsonPasswordConfirmField.content.bindTo(viewModel.passwordConfirmationFlow, lifecycleScope)

        viewModel.nextButtonState.observe(binder.exportJsonPasswordNext::setState)

        viewModel.showPasswords.observe {
            binder.exportJsonPasswordNewField.content.switchPasswordInputType(it)
            binder.exportJsonPasswordConfirmField.content.switchPasswordInputType(it)
        }

        observeValidations(viewModel)
    }
}
