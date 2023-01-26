package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.presentation.ExtendedLoadingState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.setupIdentityMixin
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateIcon
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model.setDelegateTypeModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ShortenedTextModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.applyTo
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsAccount
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsContent
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsDelegatedVotes
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsDelegations
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsDescription
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsDescriptionReadMore
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsIcon
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsIdentity
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsMetadataGroup
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsProgress
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsTitle
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsToolbar
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsType
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsVotedOverall
import kotlinx.android.synthetic.main.fragment_delegate_details.delegateDetailsVotedRecently
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

        viewModel.delegateDetailsLoadingState.observe { loadingState ->
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
    }

    private fun setContent(delegate: DelegateDetailsModel) {
        val stats = delegate.stats

        delegateDetailsDelegatedVotes.showValueOrHide(stats?.delegatedVotes)
        delegateDetailsDelegations.showValueOrHide(stats?.delegations)
        delegateDetailsVotedOverall.showValueOrHide(stats?.allVotes)

        delegateDetailsVotedRecently.showValueOrHide(stats?.recentVotes?.value)
        stats?.recentVotes?.label?.let(delegateDetailsVotedRecently::setTitle)

        if (delegate.metadata.description != null) {
            delegateDetailsMetadataGroup.makeVisible()

            with(delegate.metadata) {
                delegateDetailsIcon.setDelegateIcon(icon, imageLoader)
                delegateDetailsTitle.text = name
                delegateDetailsType.setDelegateTypeModel(accountType)
                setDescription(description)
            }
        } else {
            delegateDetailsMetadataGroup.makeGone()
        }

        delegateDetailsAccount.setAddressModel(delegate.addressModel)
    }

    private fun setDescription(maybeModel: ShortenedTextModel?) {
        maybeModel.applyTo(delegateDetailsDescription, delegateDetailsDescriptionReadMore, viewModel.markwon)
    }
}
