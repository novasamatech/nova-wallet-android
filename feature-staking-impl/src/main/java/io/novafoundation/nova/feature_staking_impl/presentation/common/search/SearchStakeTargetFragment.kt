package io.novafoundation.nova.feature_staking_impl.presentation.common.search

import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.presentation.SearchState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentSearchCustomValidatorsBinding
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel

typealias DoneAction = () -> Unit

abstract class SearchStakeTargetFragment<V : SearchStakeTargetViewModel<S>, S> : BaseFragment<V, FragmentSearchCustomValidatorsBinding>(), StakeTargetAdapter.ItemHandler<S> {

    class Configuration(
        val doneAction: DoneAction?,
        @StringRes val sortingLabelRes: Int,
    )

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    override val binder by viewBinding(FragmentSearchCustomValidatorsBinding::bind)

    abstract val configuration: Configuration

    override fun initViews() {
        binder.searchCustomValidatorsContainer.applyStatusBarInsets()

        binder.searchCustomValidatorsList.adapter = adapter
        binder.searchCustomValidatorsList.setHasFixedSize(true)
        binder.searchCustomValidatorsList.itemAnimator = null

        binder.searchCustomValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        if (configuration.doneAction != null) {
            binder.searchCustomValidatorsToolbar.setTextRight(getString(R.string.common_done))

            binder.searchCustomValidatorsToolbar.setRightActionClickListener {
                configuration.doneAction!!.invoke()
            }
        }

        binder.searchCustomValidatorRewards.setText(configuration.sortingLabelRes)

        binder.searchCustomValidatorsInput.requestFocus()
        binder.searchCustomValidatorsInput.content.showSoftKeyboard()
    }

    override fun subscribe(viewModel: V) {
        viewModel.screenState.observe {
            binder.searchCustomValidatorsList.setVisible(it is SearchState.Success, falseState = View.INVISIBLE)
            binder.searchCustomValidatorProgress.setVisible(it is SearchState.Loading, falseState = View.INVISIBLE)
            binder.searchCustomValidatorsPlaceholder.setVisible(it is SearchState.NoResults || it is SearchState.NoInput)
            binder.searchCustomValidatorListHeader.setVisible(it is SearchState.Success)

            when (it) {
                SearchState.NoInput -> {
                    binder.searchCustomValidatorsPlaceholder.setImage(R.drawable.ic_placeholder)
                    binder.searchCustomValidatorsPlaceholder.setText(getString(R.string.search_recipient_welcome_v2_2_0))
                }
                SearchState.NoResults -> {
                    binder.searchCustomValidatorsPlaceholder.setImage(R.drawable.ic_no_search_results)
                    binder.searchCustomValidatorsPlaceholder.setText(getString(R.string.staking_validator_search_empty_title))
                }
                SearchState.Loading -> {}
                is SearchState.Success -> {
                    binder.searchCustomValidatorAccounts.text = it.headerTitle

                    adapter.submitListPreservingViewPoint(it.data, binder.searchCustomValidatorsList)
                }
            }
        }

        binder.searchCustomValidatorsInput.content.bindTo(viewModel.enteredQuery, viewLifecycleOwner.lifecycleScope)
    }

    override fun stakeTargetInfoClicked(stakeTargetModel: StakeTargetModel<S>) {
        viewModel.itemInfoClicked(stakeTargetModel)
    }

    override fun stakeTargetClicked(stakeTargetModel: StakeTargetModel<S>) {
        viewModel.itemClicked(stakeTargetModel)
    }
}
