package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingStoriesAdapter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.StakingStoryModel
import kotlinx.android.synthetic.main.view_network_info.view.activeNominatorsView
import kotlinx.android.synthetic.main.view_network_info.view.lockUpPeriodView
import kotlinx.android.synthetic.main.view_network_info.view.minimumStakeView
import kotlinx.android.synthetic.main.view_network_info.view.stakingNetworkCollapsibleView
import kotlinx.android.synthetic.main.view_network_info.view.stakingNetworkInfoTitle
import kotlinx.android.synthetic.main.view_network_info.view.stakingStoriesList
import kotlinx.android.synthetic.main.view_network_info.view.totalStakeView

class NetworkInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle), StakingStoriesAdapter.StoryItemHandler {

    companion object {
        private const val ANIMATION_DURATION = 220L
    }

    enum class State {
        EXPANDED,
        COLLAPSED
    }

    var storyItemHandler: (StakingStoryModel) -> Unit = {}

    private val storiesAdapter = StakingStoriesAdapter(this)

    private var currentState = State.EXPANDED

    init {
        View.inflate(context, R.layout.view_network_info, this)

        with(context) {
            background = getBlurDrawable()
        }

        orientation = VERTICAL

        applyAttributes(attrs)

        stakingStoriesList.setHasFixedSize(true)
        stakingStoriesList.adapter = storiesAdapter

        stakingNetworkInfoTitle.setOnClickListener { changeExpandableState() }
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.NetworkInfoView)

            val isExpanded = typedArray.getBoolean(R.styleable.NetworkInfoView_expanded, true)
            if (isExpanded) expand() else collapse()

            typedArray.recycle()
        }
    }

    fun setTitle(title: String) {
        stakingNetworkInfoTitle.text = title
    }

    fun submitStories(stories: List<StakingStoryModel>) {
        storiesAdapter.submitList(stories)
    }

    fun showLoading() {
        totalStakeView.showLoading()
        minimumStakeView.showLoading()
        activeNominatorsView.showLoading()
        lockUpPeriodView.showLoading()
    }

    fun setTotalStake(inTokens: String, inFiat: String?) {
        totalStakeView.showValue(inTokens, inFiat)
    }

    fun setNominatorsCount(nominatorsCount: String) {
        activeNominatorsView.showValue(nominatorsCount)
    }

    fun setMinimumStake(inTokens: String, inFiat: String?) {
        minimumStakeView.showValue(inTokens, inFiat)
    }

    fun setLockupPeriod(period: String) {
        lockUpPeriodView.showValue(period)
    }

    override fun storyClicked(story: StakingStoryModel) {
        storyItemHandler(story)
    }

    private fun changeExpandableState() {
        if (State.EXPANDED == currentState) {
            collapse()
        } else {
            expand()
        }
    }

    private fun collapse() {
        stakingNetworkInfoTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_down_white, 0)
        currentState = State.COLLAPSED
        stakingNetworkCollapsibleView.animate()
            .setDuration(ANIMATION_DURATION)
            .alpha(0f)
            .withEndAction { stakingNetworkCollapsibleView.makeGone() }
    }

    private fun expand() {
        stakingNetworkInfoTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_up_white, 0)
        stakingNetworkCollapsibleView.makeVisible()
        currentState = State.EXPANDED
        stakingNetworkCollapsibleView.animate()
            .setDuration(ANIMATION_DURATION)
            .alpha(1f)
    }
}
