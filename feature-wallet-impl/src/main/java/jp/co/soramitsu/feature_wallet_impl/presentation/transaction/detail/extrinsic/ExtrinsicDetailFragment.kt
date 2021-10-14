package jp.co.soramitsu.feature_wallet_impl.presentation.transaction.detail.extrinsic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import jp.co.soramitsu.common.base.BaseFragment
import jp.co.soramitsu.common.di.FeatureUtils
import jp.co.soramitsu.common.utils.formatDateTime
import jp.co.soramitsu.feature_account_api.presenatation.actions.setupExternalActions
import jp.co.soramitsu.feature_wallet_api.di.WalletFeatureApi
import jp.co.soramitsu.feature_wallet_impl.R
import jp.co.soramitsu.feature_wallet_impl.di.WalletFeatureComponent
import jp.co.soramitsu.feature_wallet_impl.presentation.model.OperationParcelizeModel
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

        FeatureUtils.getFeature<WalletFeatureComponent>(
            requireContext(),
            WalletFeatureApi::class.java
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
