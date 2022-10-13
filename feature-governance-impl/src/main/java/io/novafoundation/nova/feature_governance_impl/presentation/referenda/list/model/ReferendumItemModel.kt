package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId

data class ReferendaGroupModel(val name: String, val badge: String)

data class ReferendumModel(
    val id: ReferendumId,
    val status: ReferendumStatusModel,
    val name: String,
    val timeEstimation: ReferendumTimeEstimation?,
    val track: ReferendumTrackModel?,
    val number: String,
    val voting: ReferendumVotingModel?,
    val yourVote: YourVoteModel?
)

data class ReferendumTrackModel(val name: String, @DrawableRes val iconRes: Int)

data class ReferendumStatusModel(val name: String, @ColorRes val colorRes: Int)

typealias ReferendumTimeEstimationStyleRefresher = () -> ReferendumTimeEstimation.TextStyle

sealed class ReferendumTimeEstimation {

    data class TextStyle(
        @DrawableRes val iconRes: Int,
        @ColorRes val colorRes: Int
    ) {
        companion object
    }

    data class Timer(
        val time: TimerValue,
        @StringRes val timeFormat: Int,
        val textStyleRefresher: ReferendumTimeEstimationStyleRefresher,
    ) : ReferendumTimeEstimation()

    data class Text(
        val text: String,
        val textStyle: TextStyle
    ) : ReferendumTimeEstimation()
}

data class ReferendumVotingModel(
    val positiveFraction: Float?,
    val thresholdFraction: Float,
    @DrawableRes val votingResultIcon: Int,
    @ColorRes val votingResultIconColor: Int,
    val thresholdInfo: String,
    val positivePercentage: String,
    val negativePercentage: String,
    val thresholdPercentage: String,
)

data class YourVoteModel(val voteType: String, @ColorRes val colorRes: Int, val details: String)

fun TextView.setReferendumTimeEstimation(timeEstimation: ReferendumTimeEstimation) {
    when (timeEstimation) {
        is ReferendumTimeEstimation.Text -> {
            stopTimer()

            text = timeEstimation.text
            setReferendumTextStyle(timeEstimation.textStyle)
        }

        is ReferendumTimeEstimation.Timer -> {
            setReferendumTextStyle(timeEstimation.textStyleRefresher())

            startTimer(
                value = timeEstimation.time,
                customMessageFormat = timeEstimation.timeFormat,
                onTick = { view, _ ->
                    view.setReferendumTextStyle(timeEstimation.textStyleRefresher())
                }
            )
        }
    }
}

private fun TextView.setReferendumTextStyle(textStyle: ReferendumTimeEstimation.TextStyle) {
    setTextColorRes(textStyle.colorRes)
    setDrawableEnd(textStyle.iconRes, widthInDp = 16, paddingInDp = 4, tint = textStyle.colorRes)
}
