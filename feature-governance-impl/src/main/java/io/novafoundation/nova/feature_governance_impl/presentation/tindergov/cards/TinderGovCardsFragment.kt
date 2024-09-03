package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.Duration
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter.TinderGovCardRvItem
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter.TinderGovCardsAdapter
import kotlinx.android.synthetic.main.fragment_tinder_gov_cards.tinderGovCardsBack
import kotlinx.android.synthetic.main.fragment_tinder_gov_cards.tinderGovCardsControlView
import kotlinx.android.synthetic.main.fragment_tinder_gov_cards.tinderGovCardsStack
import kotlinx.android.synthetic.main.fragment_tinder_gov_cards.tinderGovCardsStatusBarInsetsContainer

class TinderGovCardsFragment : BaseFragment<TinderGovCardsViewModel>(), TinderGovCardsAdapter.Handler, TinderGovCardStackListener {

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
        tinderGovCardsStack.itemAnimator.apply {
            if (this is DefaultItemAnimator) {
                supportsChangeAnimations = false
            }
        }
        tinderGovCardsStack.layoutManager = CardStackLayoutManager(requireContext(), this)
            .apply {
                setStackFrom(StackFrom.Bottom)
                setDirections(listOf(Direction.Left, Direction.Right, Direction.Top))
                setVisibleCount(3)
                setTranslationInterval(8f)
                setScaleInterval(getScaleIntervalForPadding(16.dp))
                setOverlayInterpolator(CardsOverlayInterpolator(delay = 0.15f, maxResult = 0.64f))
            }

        tinderGovCardsControlView.setAyeClickListener { swipeCardToDirection(Direction.Right) }
        tinderGovCardsControlView.setAbstainClickListener { swipeCardToDirection(Direction.Top) }
        tinderGovCardsControlView.setNayClickListener { swipeCardToDirection(Direction.Left) }
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
        viewModel.cardsFlow.observe { adapter.submitList(it) }

        viewModel.skipCardEvent.observeEvent {
            swipeCardToDirection(Direction.Bottom, force = true)
        }

        viewModel.isCardDraggingAvailable.observe { draggingAvailable ->
            tinderGovCardsStack.cardLayoutManager()
                .apply {
                    setCanScrollHorizontal(draggingAvailable)
                    setCanScrollVertical(draggingAvailable)
                }
        }

        viewModel.retryReferendumInfoLoadingAction.awaitableActionLiveData.observeEvent {
            warningDialog(
                requireContext(),
                onPositiveClick = { it.onSuccess(true) },
                onNegativeClick = { it.onSuccess(false) },
                positiveTextRes = R.string.common_retry,
                negativeTextRes = R.string.common_skip
            ) {
                setTitle(R.string.tinder_gov_card_data_loading_error_title)
                setMessage(R.string.tinder_gov_card_data_loading_error_message)
            }
        }
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

    override fun onCardAppeared(view: View, position: Int) {
        viewModel.onCardOnTop(position)
        viewModel.loadContentForCardByPosition(position)
    }

    override fun onCardSwiped(direction: Direction) {
        val position = tinderGovCardsStack.cardLayoutManager().topPosition
        when (direction) {
            Direction.Left -> viewModel.nayClicked(position)
            Direction.Right -> viewModel.ayeClicked(position)
            Direction.Top -> viewModel.abstainClicked(position)
            Direction.Bottom -> {}
        }
    }

    private fun swipeCardToDirection(direction: Direction, force: Boolean = false) {
        val layoutManager = tinderGovCardsStack.cardLayoutManager()
        if (force || layoutManager.canScrollVertically() && layoutManager.canScrollHorizontally()) {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(direction)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            layoutManager.setSwipeAnimationSetting(setting)
            tinderGovCardsStack.swipe()
        }
    }

    private fun CardStackView.cardLayoutManager(): CardStackLayoutManager {
        return layoutManager as CardStackLayoutManager
    }
}

class CardsOverlayInterpolator(private val delay: Float, private val maxResult: Float) : DecelerateInterpolator() {

    override fun getInterpolation(input: Float): Float {
        val realInput = (input - delay).coerceAtLeast(0f)
        val result = super.getInterpolation(realInput)

        return (result * maxResult).coerceAtMost(maxResult)
    }
}
