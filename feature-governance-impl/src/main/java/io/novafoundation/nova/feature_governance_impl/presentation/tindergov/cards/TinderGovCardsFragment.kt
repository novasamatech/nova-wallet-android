package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.StackFrom
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.asLoaded
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import kotlinx.android.synthetic.main.fragment_tinder_gov_cards.tinderGovCardsBack
import kotlinx.android.synthetic.main.fragment_tinder_gov_cards.tinderGovCardsControlView
import kotlinx.android.synthetic.main.fragment_tinder_gov_cards.tinderGovCardsStack
import kotlinx.android.synthetic.main.fragment_tinder_gov_cards.tinderGovCardsStatusBarInsetsContainer

class TinderGovCardsFragment : BaseFragment<TinderGovCardsViewModel>(), TinderGovCardsAdapter.Handler {

    private val adapter = TinderGovCardsAdapter(lifecycleOwner = this, handler = this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_tinder_gov_cards, container, false)
    }

    override fun initViews() {
        tinderGovCardsStatusBarInsetsContainer.applyStatusBarInsets()
        tinderGovCardsBack.setOnClickListener { viewModel.back() }

        tinderGovCardsStack.adapter = adapter
        tinderGovCardsStack.layoutManager = CardStackLayoutManager(requireContext())
            .apply {
                setStackFrom(StackFrom.Bottom)
                setDirections(listOf(Direction.Left, Direction.Right, Direction.Top))
                setVisibleCount(3)
                setTranslationInterval(8f)
                setScaleInterval(getScaleIntervalForPadding(16.dp))
                setOverlayInterpolator(CardsOverlayInterpolator(delay = 0.15f, maxResult = 0.64f))
            }

        tinderGovCardsControlView.setAyeClickListener { viewModel.ayeClicked() }
        tinderGovCardsControlView.setAbstainClickListener { viewModel.abstainClicked() }
        tinderGovCardsControlView.setNayClickListener { viewModel.nayClicked() }

        fun createItem(id: Int): TinderGovCardRvItem {
            return TinderGovCardRvItem(
                id = id.toBigInteger(),
                summary = "Referendum $id".asLoaded(),
                requestedAmount = ExtendedLoadingState.Loading,
                descriptiveButtonState = DescriptiveButtonState.Loading,
                backgroundRes = R.drawable.ic_tinder_gov_entry_banner_background
            )
        }

        adapter.submitList(
            listOf(
                createItem(0),
                createItem(1),
                createItem(2),
                createItem(3),
                createItem(4)
            )
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .tinderGovCardsFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: TinderGovCardsViewModel) {

    }

    private fun getScaleIntervalForPadding(desiredPadding: Int): Float {
        val screenWidth = resources.displayMetrics.widthPixels
        val cardsPadding = tinderGovCardsStack.paddingStart + tinderGovCardsStack.paddingEnd
        val cardWidth = screenWidth - cardsPadding
        val nextCardWidth = cardWidth - desiredPadding * 2
        return nextCardWidth.toFloat() / cardWidth.toFloat()
    }

    override fun onReadMoreClicked(item: TinderGovCardRvItem) {
        viewModel.openReadMore(item)
    }
}

class CardsOverlayInterpolator(private val delay: Float, private val maxResult: Float) : DecelerateInterpolator() {

    override fun getInterpolation(input: Float): Float {
        val realInput = (input - delay).coerceAtLeast(0f)
        val result = super.getInterpolation(realInput)

        return (result * maxResult).coerceAtMost(maxResult)
    }
}
