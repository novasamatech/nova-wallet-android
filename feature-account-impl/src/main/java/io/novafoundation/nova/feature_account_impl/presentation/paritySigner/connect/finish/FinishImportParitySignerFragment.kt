package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.finish

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_api.presenatation.account.createName.CreateWalletNameFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerAccountPayload

class FinishImportParitySignerFragment : CreateWalletNameFragment<FinishImportParitySignerViewModel>() {

    companion object {

        private const val PAYLOAD_KEY = "FinishImportParitySignerFragment.Payload"

        fun getBundle(payload: ParitySignerAccountPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD_KEY, payload)
            }
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .finishImportParitySignerComponentFactory()
            .create(this, argument(PAYLOAD_KEY))
            .inject(this)
    }
}
