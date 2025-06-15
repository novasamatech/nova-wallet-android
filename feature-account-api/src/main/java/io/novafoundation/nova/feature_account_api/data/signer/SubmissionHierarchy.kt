package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

class SubmissionHierarchy(
    val path: List<Node>
) {

    class Node(
        val account: MetaAccount,
        val callExecutionType: CallExecutionType
    )

    constructor(metaAccount: MetaAccount, callExecutionType: CallExecutionType) : this(listOf(Node(metaAccount, callExecutionType)))

    operator fun plus(submissionHierarchy: SubmissionHierarchy): SubmissionHierarchy {
        return SubmissionHierarchy(path + submissionHierarchy.path)
    }
}

fun SubmissionHierarchy.isDelayed() = path.any { it.callExecutionType == CallExecutionType.DELAYED }
