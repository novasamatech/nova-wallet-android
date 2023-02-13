package io.novafoundation.nova.feature_governance_impl.presentation.voters

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.showValueOrHide

class VoterModel(
    val addressModel: AddressModel,
    val vote: VoteModel,
)

class VoteModel(
    val votesCount: String,
    val votesCountDetails: String
)

fun TableCellView.setVoteModel(model: VoteModel) {
    showValue(model.votesCount, model.votesCountDetails)
}

fun TableCellView.setVoteModelOrHide(model: VoteModel?) {
    showValueOrHide(model?.votesCount, model?.votesCountDetails)
}
