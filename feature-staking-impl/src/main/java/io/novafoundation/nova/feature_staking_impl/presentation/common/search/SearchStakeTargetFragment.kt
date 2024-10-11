package io.novafoundation.nova.feature_staking_impl.presentation.common.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.presentation.SearchState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel

typealias DoneAction = () -> Unit

abstract class SearchStakeTargetFragment<V : SearchStakeTargetViewModel<S>, S> : BaseFragment<V>(), StakeTargetAdapter.ItemHandler<S> {

    class Configuration(
        val doneAction: DoneAction?,
        @StringRes val sortingLabelRes: Int,
    )

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    abstract val configuration: Configuration

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_custom_validators, container, false)
    }

    override fun initViews() {
        searchCustomValidatorsContainer.applyStatusBarInsets()

        searchCustomValidatorsList.adapter = adapter
        searchCustomValidatorsList.setHasFixedSize(true)
        searchCustomValidatorsList.itemAnimator = null

        searchCustomValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        if (configuration.doneAction != null) {
            searchCustomValidatorsToolbar.setTextRight(getString(R.string.common_done))

            searchCustomValidatorsToolbar.setRightActionClickListener {
                configuration.doneAction!!.invoke()
            }
        }

        searchCustomValidatorRewards.setText(configuration.sortingLabelRes)

        searchCustomValidatorsInput.requestFocus()
        searchCustomValidatorsInput.content.showSoftKeyboard()
    }

    override fun subscribe(viewModel: V) {
        viewModel.screenState.observe {
            searchCustomValidatorsList.setVisible(it is SearchState.Success, falseState = View.INVISIBLE)
            searchCustomValidatorProgress.setVisible(it is SearchState.Loading, falseState = View.INVISIBLE)
            searchCustomValidatorsPlaceholder.setVisible(it is SearchState.NoResults || it is SearchState.NoInput)
            searchCustomValidatorListHeader.setVisible(it is SearchState.Success)

            when (it) {
                SearchState.NoInput -> {
                    searchCustomValidatorsPlaceholder.setImage(R.drawable.ic_placeholder)
                    searchCustomValidatorsPlaceholder.setText(getString(R.string.search_recipient_welcome_v2_2_0))
                }
                SearchState.NoResults -> {
                    searchCustomValidatorsPlaceholder.setImage(R.drawable.ic_no_search_results)
                    searchCustomValidatorsPlaceholder.setText(getString(R.string.staking_validator_search_empty_title))
                }
                SearchState.Loading -> {}
                is SearchState.Success -> {
                    searchCustomValidatorAccounts.text = it.headerTitle

                    adapter.submitListPreservingViewPoint(it.data, searchCustomValidatorsList)
                }
            }
        }

        searchCustomValidatorsInput.content.bindTo(viewModel.enteredQuery, viewLifecycleOwner.lifecycleScope)
    }

    override fun stakeTargetInfoClicked(stakeTargetModel: StakeTargetModel<S>) {
        viewModel.itemInfoClicked(stakeTargetModel)
    }

    override fun stakeTargetClicked(stakeTargetModel: StakeTargetModel<S>) {
        viewModel.itemClicked(stakeTargetModel)
    }
}
