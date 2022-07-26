package io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.formatDateTime
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_account_api.view.showAddress
import io.novafoundation.nova.feature_account_api.view.showChain
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.model.showOperationStatus
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailAmount
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailCall
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailHash
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailIcon
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailModule
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailNetwork
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailSender
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailStatus
import kotlinx.android.synthetic.main.fragment_extrinsic_details.extrinsicDetailToolbar
import javax.inject.Inject

private const val KEY_EXTRINSIC = "KEY_EXTRINSIC"

class ExtrinsicDetailFragment : BaseFragment<ExtrinsicDetailViewModel>() {

    companion object {
        fun getBundle(operation: OperationParcelizeModel.Extrinsic) = Bundle().apply {
            putParcelable(KEY_EXTRINSIC, operation)
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_extrinsic_details, container, false)

    override fun initViews() {
        extrinsicDetailToolbar.setHomeButtonListener { viewModel.backClicked() }

        extrinsicDetailHash.setOnClickListener {
            viewModel.extrinsicClicked()
        }

        extrinsicDetailSender.setOnClickListener {
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
            extrinsicDetailHash.showValue(hash)

            extrinsicDetailStatus.showOperationStatus(statusAppearance)
            extrinsicDetailAmount.setTextColorRes(statusAppearance.amountTint)

            extrinsicDetailToolbar.setTitle(time.formatDateTime(requireContext()))
            extrinsicDetailModule.showValue(module)
            extrinsicDetailCall.showValue(call)

            extrinsicDetailAmount.text = fee
        }

        viewModel.senderAddressModelFlow.observe(extrinsicDetailSender::showAddress)

        viewModel.chainUi.observe(extrinsicDetailNetwork::showChain)

        viewModel.operationIcon.observe {
            extrinsicDetailIcon.loadTokenIcon(it, imageLoader)
        }
    }
}
