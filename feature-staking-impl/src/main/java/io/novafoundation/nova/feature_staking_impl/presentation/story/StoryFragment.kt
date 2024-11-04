package io.novafoundation.nova.feature_staking_impl.presentation.story

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentStoryBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.StakingStoryModel
import jp.shts.android.storiesprogressview.StoriesProgressView

class StoryFragment : BaseFragment<StoryViewModel, FragmentStoryBinding>(), StoriesProgressView.StoriesListener {

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

    override fun createBinding() = FragmentStoryBinding.inflate(layoutInflater)

    private var lastActionDown = 0L

    override fun initViews() {
        binder.storyCloseIcon.setOnClickListener { viewModel.backClicked() }

        binder.stories.setStoriesListener(this)

        binder.storyContainer.setOnTouchListener(::handleStoryTouchEvent)

        binder.stakingStoryLearnMore.setOnClickListener { viewModel.learnMoreClicked() }
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
            binder.stories.setStoriesCount(story.elements.size)
            binder.stories.setStoryDuration(STORY_DURATION)
            binder.stories.startStories()
        }

        viewModel.currentStoryLiveData.observe {
            binder.storyTitle.setText(it.titleRes)
            binder.storyBody.setText(it.bodyRes)
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
        binder.stories.destroy()
    }

    private fun handleStoryTouchEvent(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastActionDown = System.currentTimeMillis()
                binder.stories.pause()
            }
            MotionEvent.ACTION_UP -> {
                binder.stories.resume()
                val eventTime = System.currentTimeMillis()
                if (eventTime - lastActionDown < STORY_CLICK_MAX_DURATION) {
                    if (view.width / 2 < event.x) {
                        binder.stories.skip()
                    } else {
                        binder.stories.reverse()
                    }
                } else {
                    view.performClick()
                }
            }
        }
        return true
    }
}
