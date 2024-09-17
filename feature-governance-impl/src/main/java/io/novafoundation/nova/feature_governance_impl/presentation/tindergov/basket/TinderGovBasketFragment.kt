package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.dialog.infoDialog
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.adpter.TinderGovBasketAdapter
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.adpter.TinderGovBasketRvItem
import kotlinx.android.synthetic.main.fragment_tinder_gov_basket.tinderGovBasketButton
import kotlinx.android.synthetic.main.fragment_tinder_gov_basket.tinderGovBasketList
import kotlinx.android.synthetic.main.fragment_tinder_gov_basket.tinderGovBasketToolbar

class TinderGovBasketFragment : BaseFragment<TinderGovBasketViewModel>(), TinderGovBasketAdapter.Handler {

    private val adapter = TinderGovBasketAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_tinder_gov_basket, container, false)
    }

    override fun initViews() {
        tinderGovBasketToolbar.applyStatusBarInsets()
        tinderGovBasketToolbar.setHomeButtonListener { viewModel.backClicked() }
        tinderGovBasketToolbar.setRightActionClickListener { viewModel.toggleEditMode() }

        tinderGovBasketList.itemAnimator = DefaultItemAnimator()
            .apply {
                supportsChangeAnimations = false
            }
        tinderGovBasketList.adapter = adapter
        tinderGovBasketButton.setOnClickListener { viewModel.voteClicked() }
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
        viewModel.editModeButtonText.observe { tinderGovBasketToolbar.setTextRight(it) }

        viewModel.basketFlow.observe {
            adapter.submitList(it)
        }

        viewModel.removeReferendumAction.awaitableActionLiveData.observeEvent { event ->
            warningDialog(
                requireContext(),
                onPositiveClick = { event.onSuccess(true) },
                onNegativeClick = { event.onSuccess(false) },
                positiveTextRes = R.string.common_remove,
                negativeTextRes = R.string.common_cancel
            ) {
                setTitle(event.payload)
                setMessage(R.string.tinder_gov_basket_remove_item_confirm_message)
            }
        }

        viewModel.itemsWasRemovedFromBasketAction.awaitableActionLiveData.observeEvent { event ->
            infoDialog(
                requireContext()
            ) {
                setPositiveButton(R.string.common_ok) { _, _ -> event.onSuccess(Unit) }

                setTitle(R.string.tinder_gov_basket_removed_items_title)
                setMessage(R.string.tinder_gov_basket_removed_items_message)
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
