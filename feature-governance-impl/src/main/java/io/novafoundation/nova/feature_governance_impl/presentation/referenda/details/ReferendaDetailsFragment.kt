package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.feature_governance_impl.R

class ReferendumDetailsFragment : BaseFragment<ReferendumDetailsViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_referendum_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun initViews() {

    }

    override fun inject() {

    }

    override fun subscribe(viewModel: ReferendumDetailsViewModel) {

    }
}
