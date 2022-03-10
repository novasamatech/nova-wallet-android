package io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatDateTime
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailCall
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailDate
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailFee
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailFrom
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailHash
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailModule
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailStatus
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailStatusIcon
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailToolbar

private const val KEY_EXTRINSIC = "KEY_EXTRINSIC"

class ExtrinsicDetailFragment : BaseFragment<ExtrinsicDetailViewModel>() {
    companion object {
        fun getBundle(operation: OperationParcelizeModel.Extrinsic) = Bundle().apply {
            putParcelable(KEY_EXTRINSIC, operation)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_extrinsic_details, container, false)

    override fun initViews() {
        extrinsicDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        extrinsicDetailHash.setWholeClickListener {
            viewModel.extrinsicClicked()
        }

        extrinsicDetailFrom.setWholeClickListener {
            viewModel.fromAddressClicked()
        }
    }

    override fun inject() {
        val operation = argument<OperationParcelizeModel.Extrinsic>(KEY_EXTRINSIC)

        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .extrinsicDetailComponentFactory()
            .create(this, operation)
            .inject(this)
    }

    override fun subscribe(viewModel: ExtrinsicDetailViewModel) {
        setupExternalActions(viewModel)

        with(viewModel.operation) {
            extrinsicDetailHash.setMessage(hash)
            extrinsicDetailStatus.setText(statusAppearance.labelRes)
            extrinsicDetailStatusIcon.setImageResource(statusAppearance.icon)
            extrinsicDetailDate.text = time.formatDateTime(requireContext())
            extrinsicDetailModule.text = module
            extrinsicDetailCall.text = call
            extrinsicDetailFee.text = fee
        }

        viewModel.fromAddressModelLiveData.observe { addressModel ->
            extrinsicDetailFrom.setMessage(addressModel.nameOrAddress)
            extrinsicDetailFrom.setTextIcon(addressModel.image)
        }
    }
}
