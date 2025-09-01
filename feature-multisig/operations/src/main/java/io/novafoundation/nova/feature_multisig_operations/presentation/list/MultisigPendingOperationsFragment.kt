package io.novafoundation.nova.feature_multisig_operations.presentation.list

import android.view.View
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.domain.onNotLoaded
import io.novafoundation.nova.common.utils.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_multisig_operations.databinding.FragmentMultisigPendingOperationsBinding
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureApi
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureComponent
import io.novafoundation.nova.feature_multisig_operations.presentation.list.model.PendingMultisigOperationModel
import javax.inject.Inject

class MultisigPendingOperationsFragment :
    BaseFragment<MultisigPendingOperationsViewModel, FragmentMultisigPendingOperationsBinding>(),
    MultisigPendingOperationsAdapter.ItemHandler {

    override fun createBinding() = FragmentMultisigPendingOperationsBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter: MultisigPendingOperationsAdapter by lazy(LazyThreadSafetyMode.NONE) { MultisigPendingOperationsAdapter(this, imageLoader) }

    override fun applyInsets(rootView: View) {
        binder.multisigPendingOperationsToolbar.applyStatusBarInsets()
        binder.multisigPendingOperationsList.applyNavigationBarInsets(consume = false)
    }

    override fun initViews() {
        binder.multisigPendingOperationsList.setHasFixedSize(true)
        binder.multisigPendingOperationsList.adapter = adapter

        binder.multisigPendingOperationsToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<MultisigOperationsFeatureComponent>(
            requireContext(),
            MultisigOperationsFeatureApi::class.java
        )
            .multisigPendingOperations()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: MultisigPendingOperationsViewModel) {
        viewModel.pendingOperationsFlow.observe {
            it.onLoaded { data ->
                binder.multisigPendingOperationsPlaceholder.isVisible = data.isEmpty()
                binder.multisigPendingOperationsProgress.makeGone()
                binder.multisigPendingOperationsList.makeVisible()
                adapter.submitList(data)
            }.onNotLoaded {
                binder.multisigPendingOperationsProgress.makeVisible()
                binder.multisigPendingOperationsList.makeGone()
            }
        }
    }

    override fun itemClicked(model: PendingMultisigOperationModel) {
        viewModel.operationClicked(model)
    }
}
