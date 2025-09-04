package io.novafoundation.nova.feature_staking_impl.presentation.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.StakingStoryModel

class StoryViewModel(
    private val router: StakingRouter,
    private val story: StakingStoryModel
) : BaseViewModel(), Browserable {

    private val _storyLiveData = MutableLiveData(story)
    val storyLiveData: LiveData<StakingStoryModel> = _storyLiveData

    private val _currentStoryLiveData = MutableLiveData<StakingStoryModel.Element>()
    val currentStoryLiveData: LiveData<StakingStoryModel.Element> = _currentStoryLiveData

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    init {
        story.elements.firstOrNull()?.let {
            _currentStoryLiveData.value = it
        }
    }

    fun backClicked() {
        router.back()
    }

    fun nextStory() {
        val stories = storyLiveData.value?.elements ?: return
        val currentStory = currentStoryLiveData.value ?: return

        val nextStoryIndex = stories.indexOf(currentStory) + 1
        if (nextStoryIndex < stories.size) {
            _currentStoryLiveData.value = stories[nextStoryIndex]
        }
    }

    fun previousStory() {
        val stories = storyLiveData.value?.elements ?: return
        val currentStory = currentStoryLiveData.value ?: return

        val previousStoryIndex = stories.indexOf(currentStory) - 1
        if (previousStoryIndex >= 0) {
            _currentStoryLiveData.value = stories[previousStoryIndex]
        }
    }

    fun complete() {
        router.back()
    }

    fun learnMoreClicked() {
        currentStoryLiveData.value?.let {
            openBrowserEvent.value = Event(it.url)
        }
    }
}
