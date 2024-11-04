package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
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
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentDelegateDetailsBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateIcon
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateTypeModel
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.view.setModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ShortenedTextModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.applyTo
import io.novafoundation.nova.feature_governance_impl.presentation.track.list.TrackDelegationListBottomSheet

import javax.inject.Inject

class DelegateDetailsFragment : BaseFragment<DelegateDetailsViewModel, FragmentDelegateDetailsBinding>() {

    companion object {
        private const val PAYLOAD = "DelegateDetailsFragment.Payload"

        fun getBundle(payload: DelegateDetailsPayload): Bundle {
            return Bundle().apply {
                putParcelable(PAYLOAD, payload)
            }
        }
    }

    override val binder by viewBinding(FragmentDelegateDetailsBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun initViews() {
        binder.delegateDetailsToolbar.applyStatusBarInsets()
        binder.delegateDetailsToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.delegateDetailsDelegations.setOnClickListener { viewModel.delegationsClicked() }
        binder.delegateDetailsVotedRecently.setOnClickListener { viewModel.recentVotesClicked() }
        binder.delegateDetailsVotedOverall.setOnClickListener { viewModel.allVotesClicked() }

        binder.delegateDetailsAccount.setOnClickListener { viewModel.accountActionsClicked() }

        binder.delegateDetailsDescriptionReadMore.setOnClickListener { viewModel.readMoreClicked() }

        binder.delegateDetailsAddDelegation.setOnClickListener { viewModel.addDelegationClicked() }

        binder.delegateDetailsYourDelegation.onTracksClicked { viewModel.tracksClicked() }
        binder.delegateDetailsYourDelegation.onEditClicked { viewModel.editDelegationClicked() }
        binder.delegateDetailsYourDelegation.onRevokeClicked { viewModel.revokeDelegationClicked() }
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
        setupIdentityMixin(viewModel.identityMixin, binder.delegateDetailsIdentity)
        observeValidations(viewModel)

        viewModel.delegateDetailsLoadingState.observeWhenVisible { loadingState ->
            when (loadingState) {
                is ExtendedLoadingState.Error -> {}
                is ExtendedLoadingState.Loaded -> {
                    binder.delegateDetailsContent.makeVisible()
                    binder.delegateDetailsProgress.makeGone()

                    setContent(loadingState.data)
                }
                ExtendedLoadingState.Loading -> {
                    binder.delegateDetailsContent.makeGone()
                    binder.delegateDetailsProgress.makeVisible()
                }
            }
        }

        viewModel.showTracksEvent.observeEvent { tracksDelegations ->
            TrackDelegationListBottomSheet(requireContext(), tracksDelegations)
                .show()
        }

        viewModel.addDelegationButtonState.observeWhenVisible(binder.delegateDetailsAddDelegation::setState)
    }

    private fun setContent(delegate: DelegateDetailsModel) {
        val stats = delegate.stats

        binder.delegateDetailsDelegatedVotes.showValueOrHide(stats?.delegatedVotes)

        binder.delegateDetailsDelegations.setVotesModel(stats?.delegations)
        binder.delegateDetailsVotedOverall.setVotesModel(stats?.allVotes)
        binder.delegateDetailsVotedRecently.setVotesModel(stats?.recentVotes)

        if (delegate.metadata.description != null) {
            binder.delegateDetailsMetadataGroup.makeVisible()

            with(delegate.metadata) {
                binder.delegateDetailsIcon.setDelegateIcon(icon, imageLoader, 12)
                binder.delegateDetailsTitle.text = name
                binder.delegateDetailsType.setDelegateTypeModel(accountType)
                setDescription(description)
            }
        } else {
            binder.delegateDetailsMetadataGroup.makeGone()
        }

        binder.delegateDetailsAccount.setAddressModel(delegate.addressModel)

        binder.delegateDetailsYourDelegation.setModel(delegate.userDelegation)
    }

    private fun TableCellView.setVotesModel(model: DelegateDetailsModel.VotesModel?) = letOrHide(model) { votesModel ->
        showValue(votesModel.votes)

        setExtraInfoAvailable(votesModel.extraInfoAvalable)

        votesModel.customLabel?.let(::setTitle)
    }

    private fun setDescription(maybeModel: ShortenedTextModel?) {
        maybeModel.applyTo(binder.delegateDetailsDescription, binder.delegateDetailsDescriptionReadMore, viewModel.markwon)
    }
}
