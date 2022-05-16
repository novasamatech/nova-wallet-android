package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.validators.StakeTargetAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.ValidatorModel
import kotlinx.android.synthetic.main.fragment_search_custom_validators.searchCustomValidatorAccounts
import kotlinx.android.synthetic.main.fragment_search_custom_validators.searchCustomValidatorListHeader
import kotlinx.android.synthetic.main.fragment_search_custom_validators.searchCustomValidatorProgress
import kotlinx.android.synthetic.main.fragment_search_custom_validators.searchCustomValidatorsContainer
import kotlinx.android.synthetic.main.fragment_search_custom_validators.searchCustomValidatorsInput
import kotlinx.android.synthetic.main.fragment_search_custom_validators.searchCustomValidatorsList
import kotlinx.android.synthetic.main.fragment_search_custom_validators.searchCustomValidatorsPlaceholder
import kotlinx.android.synthetic.main.fragment_search_custom_validators.searchCustomValidatorsToolbar

class SearchCustomValidatorsFragment : BaseFragment<SearchCustomValidatorsViewModel>(), StakeTargetAdapter.ItemHandler<Validator> {

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        StakeTargetAdapter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_custom_validators, container, false)
    }

    override fun initViews() {
        searchCustomValidatorsContainer.applyInsetter {
            type(statusBars = true) {
                padding()
            }

            consume(true)
        }

        searchCustomValidatorsList.adapter = adapter
        searchCustomValidatorsList.setHasFixedSize(true)
        searchCustomValidatorsList.itemAnimator = null

        searchCustomValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        searchCustomValidatorsToolbar.setRightActionClickListener {
            viewModel.doneClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .searchCustomValidatorsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SearchCustomValidatorsViewModel) {
        viewModel.screenState.observe {
            searchCustomValidatorsList.setVisible(it is SearchValidatorsState.Success, falseState = View.INVISIBLE)
            searchCustomValidatorProgress.setVisible(it is SearchValidatorsState.Loading, falseState = View.INVISIBLE)
            searchCustomValidatorsPlaceholder.setVisible(it is SearchValidatorsState.NoResults || it is SearchValidatorsState.NoInput)
            searchCustomValidatorListHeader.setVisible(it is SearchValidatorsState.Success)

            when (it) {
                SearchValidatorsState.NoInput -> {
                    searchCustomValidatorsPlaceholder.setImage(R.drawable.ic_placeholder)
                    searchCustomValidatorsPlaceholder.setText(getString(R.string.search_recipient_welcome_v2_2_0))
                }
                SearchValidatorsState.NoResults -> {
                    searchCustomValidatorsPlaceholder.setImage(R.drawable.ic_no_search_results)
                    searchCustomValidatorsPlaceholder.setText(getString(R.string.staking_validator_search_empty_title))
                }
                SearchValidatorsState.Loading -> {}
                is SearchValidatorsState.Success -> {
                    searchCustomValidatorAccounts.text = it.headerTitle

                    adapter.submitListPreservingViewPoint(it.validators, searchCustomValidatorsList)
                }
            }
        }

        searchCustomValidatorsInput.content.bindTo(viewModel.enteredQuery, viewLifecycleOwner.lifecycleScope)
    }

    override fun stakeTargetInfoClicked(validatorModel: ValidatorModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }

    override fun stakeTargetClicked(validatorModel: ValidatorModel) {
        viewModel.validatorClicked(validatorModel)
    }
}
