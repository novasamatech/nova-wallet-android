package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import kotlinx.android.synthetic.main.fragment_sign_parity_signer_show.signParitySignerShowQr

class ShowSignParitySignerFragment : BaseFragment<ShowSignParitySignerViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_parity_signer_show, container, false)
    }

    override fun initViews() {
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .showSignParitySignerComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ShowSignParitySignerViewModel) {
        viewModel.qrCode.observe(signParitySignerShowQr::setImageBitmap)
    }
}
