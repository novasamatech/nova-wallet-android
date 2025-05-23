package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket

import androidx.recyclerview.widget.DefaultItemAnimator

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.dialog.infoDialog
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentTinderGovBasketBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.adpter.TinderGovBasketAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.adpter.TinderGovBasketRvItem

class TinderGovBasketFragment : BaseFragment<TinderGovBasketViewModel, FragmentTinderGovBasketBinding>(), TinderGovBasketAdapter.Handler {

    override fun createBinding() = FragmentTinderGovBasketBinding.inflate(layoutInflater)

    private val adapter = TinderGovBasketAdapter(this)

    override fun initViews() {
        binder.tinderGovBasketToolbar.applyStatusBarInsets()
        binder.tinderGovBasketToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.tinderGovBasketToolbar.setRightActionClickListener { viewModel.toggleEditMode() }

        binder.tinderGovBasketList.itemAnimator = DefaultItemAnimator()
            .apply {
                supportsChangeAnimations = false
            }
        binder.tinderGovBasketList.adapter = adapter
        binder.tinderGovBasketButton.setOnClickListener { viewModel.voteClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .tinderGovBasketFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: TinderGovBasketViewModel) {
        viewModel.inEditModeFlow.observe { adapter.setEditMode(it) }
        viewModel.editModeButtonText.observe { binder.tinderGovBasketToolbar.setTextRight(it) }

        viewModel.basketFlow.observe {
            adapter.submitList(it)
        }

        viewModel.voteButtonStateFlow.observe {
            binder.tinderGovBasketButton.setState(it)
        }

        viewModel.removeReferendumAction.awaitableActionLiveData.observeEvent { event ->
            warningDialog(
                requireContext(),
                onPositiveClick = { event.onSuccess(true) },
                onNegativeClick = { event.onSuccess(false) },
                positiveTextRes = R.string.common_remove,
                negativeTextRes = R.string.common_cancel,
                styleRes = R.style.AccentNegativeAlertDialogTheme_Reversed,
            ) {
                setTitle(event.payload)
                setMessage(R.string.swipe_gov_basket_remove_item_confirm_message)
            }
        }

        viewModel.itemsWasRemovedFromBasketAction.awaitableActionLiveData.observeEvent { event ->
            infoDialog(
                requireContext()
            ) {
                setPositiveButton(R.string.common_ok) { _, _ -> event.onSuccess(Unit) }

                setTitle(R.string.swipe_gov_basket_removed_items_title)
                setMessage(requireContext().getString(R.string.swipe_gov_basket_removed_items_message, event.payload))
            }
        }
    }

    override fun onItemClicked(item: TinderGovBasketRvItem) {
        viewModel.onItemClicked(item)
    }

    override fun onItemDeleteClicked(item: TinderGovBasketRvItem) {
        viewModel.onItemDeleteClicked(item)
    }
}
