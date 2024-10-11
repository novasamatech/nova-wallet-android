package io.novafoundation.nova.feature_staking_impl.presentation.story

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.StakingStoryModel
import jp.shts.android.storiesprogressview.StoriesProgressView

class StoryFragment : BaseFragment<StoryViewModel>(), StoriesProgressView.StoriesListener {

    companion object {
        private const val KEY_STORY = "story"
        private const val STORY_DURATION = 6200L
        private const val STORY_CLICK_MAX_DURATION = 500L

        fun getBundle(story: StakingStoryModel): Bundle {
            return Bundle().apply {
                putParcelable(KEY_STORY, story)
            }
        }
    }

    private var lastActionDown = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_story, container, false)
    }

    override fun initViews() {
        storyCloseIcon.setOnClickListener { viewModel.backClicked() }

        stories.setStoriesListener(this)

        storyContainer.setOnTouchListener(::handleStoryTouchEvent)

        stakingStoryLearnMore.setOnClickListener { viewModel.learnMoreClicked() }
    }

    override fun inject() {
        val story = argument<StakingStoryModel>(KEY_STORY)

        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .storyComponentFactory()
            .create(this, story)
            .inject(this)
    }

    override fun subscribe(viewModel: StoryViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.storyLiveData.observe { story ->
            stories.setStoriesCount(story.elements.size)
            stories.setStoryDuration(STORY_DURATION)
            stories.startStories()
        }

        viewModel.currentStoryLiveData.observe {
            storyTitle.setText(it.titleRes)
            storyBody.setText(it.bodyRes)
        }
    }

    override fun onComplete() {
        viewModel.complete()
    }

    override fun onPrev() {
        viewModel.previousStory()
    }

    override fun onNext() {
        viewModel.nextStory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stories.destroy()
    }

    private fun handleStoryTouchEvent(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastActionDown = System.currentTimeMillis()
                stories.pause()
            }
            MotionEvent.ACTION_UP -> {
                stories.resume()
                val eventTime = System.currentTimeMillis()
                if (eventTime - lastActionDown < STORY_CLICK_MAX_DURATION) {
                    if (view.width / 2 < event.x) {
                        stories.skip()
                    } else {
                        stories.reverse()
                    }
                } else {
                    view.performClick()
                }
            }
        }
        return true
    }
}
