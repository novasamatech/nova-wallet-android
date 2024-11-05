package io.novafoundation.nova.feature_account_impl.presentation.node.add

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.databinding.FragmentNodeAddBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class AddNodeFragment : BaseFragment<AddNodeViewModel, FragmentNodeAddBinding>() {

    override fun createBinding() = FragmentNodeAddBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.novaToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.nodeNameField.content.bindTo(viewModel.nodeNameInputLiveData)

        binder.nodeHostField.content.bindTo(viewModel.nodeHostInputLiveData)

        binder.addBtn.setOnClickListener { viewModel.addNodeClicked() }

        binder.addBtn.prepareForProgress(viewLifecycleOwner)
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .addNodeComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AddNodeViewModel) {
        viewModel.addButtonState.observe(binder.addBtn::setState)
    }
}
