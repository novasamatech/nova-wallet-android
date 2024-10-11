package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.setExtraInfoAvailable
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.setupIdentityMixin
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateIcon
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateTypeModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.view.setModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ShortenedTextModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.applyTo
import io.novafoundation.nova.feature_governance_impl.presentation.track.list.TrackDelegationListBottomSheet

import javax.inject.Inject

class DelegateDetailsFragment : BaseFragment<DelegateDetailsViewModel>() {

    companion object {
        private const val PAYLOAD = "DelegateDetailsFragment.Payload"

        fun getBundle(payload: DelegateDetailsPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD, payload)
            }
        }
    }

    @Inject
    protected lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_delegate_details, container, false)
    }

    override fun initViews() {
        delegateDetailsToolbar.applyStatusBarInsets()
        delegateDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        delegateDetailsDelegations.setOnClickListener { viewModel.delegationsClicked() }
        delegateDetailsVotedRecently.setOnClickListener { viewModel.recentVotesClicked() }
        delegateDetailsVotedOverall.setOnClickListener { viewModel.allVotesClicked() }

        delegateDetailsAccount.setOnClickListener { viewModel.accountActionsClicked() }

        delegateDetailsDescriptionReadMore.setOnClickListener { viewModel.readMoreClicked() }

        delegateDetailsAddDelegation.setOnClickListener { viewModel.addDelegationClicked() }

        delegateDetailsYourDelegation.onTracksClicked { viewModel.tracksClicked() }
        delegateDetailsYourDelegation.onEditClicked { viewModel.editDelegationClicked() }
        delegateDetailsYourDelegation.onRevokeClicked { viewModel.revokeDelegationClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .delegateDetailsFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: DelegateDetailsViewModel) {
        setupExternalActions(viewModel)
        setupIdentityMixin(viewModel.identityMixin, delegateDetailsIdentity)
        observeValidations(viewModel)

        viewModel.delegateDetailsLoadingState.observeWhenVisible { loadingState ->
            when (loadingState) {
                is ExtendedLoadingState.Error -> {}
                is ExtendedLoadingState.Loaded -> {
                    delegateDetailsContent.makeVisible()
                    delegateDetailsProgress.makeGone()

                    setContent(loadingState.data)
                }
                ExtendedLoadingState.Loading -> {
                    delegateDetailsContent.makeGone()
                    delegateDetailsProgress.makeVisible()
                }
            }
        }

        viewModel.showTracksEvent.observeEvent { tracksDelegations ->
            TrackDelegationListBottomSheet(requireContext(), tracksDelegations)
                .show()
        }

        viewModel.addDelegationButtonState.observeWhenVisible(delegateDetailsAddDelegation::setState)
    }

    private fun setContent(delegate: DelegateDetailsModel) {
        val stats = delegate.stats

        delegateDetailsDelegatedVotes.showValueOrHide(stats?.delegatedVotes)

        delegateDetailsDelegations.setVotesModel(stats?.delegations)
        delegateDetailsVotedOverall.setVotesModel(stats?.allVotes)
        delegateDetailsVotedRecently.setVotesModel(stats?.recentVotes)

        if (delegate.metadata.description != null) {
            delegateDetailsMetadataGroup.makeVisible()

            with(delegate.metadata) {
                delegateDetailsIcon.setDelegateIcon(icon, imageLoader, 12)
                delegateDetailsTitle.text = name
                delegateDetailsType.setDelegateTypeModel(accountType)
                setDescription(description)
            }
        } else {
            delegateDetailsMetadataGroup.makeGone()
        }

        delegateDetailsAccount.setAddressModel(delegate.addressModel)

        delegateDetailsYourDelegation.setModel(delegate.userDelegation)
    }

    private fun TableCellView.setVotesModel(model: DelegateDetailsModel.VotesModel?) = letOrHide(model) { votesModel ->
        showValue(votesModel.votes)

        setExtraInfoAvailable(votesModel.extraInfoAvalable)

        votesModel.customLabel?.let(::setTitle)
    }

    private fun setDescription(maybeModel: ShortenedTextModel?) {
        maybeModel.applyTo(delegateDetailsDescription, delegateDetailsDescriptionReadMore, viewModel.markwon)
    }
}
