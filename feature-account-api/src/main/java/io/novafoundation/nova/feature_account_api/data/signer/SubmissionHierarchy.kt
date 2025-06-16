package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

/**
 * A signing chain of accounts.
 * Contains at least 1 item in path.
 * Ordering of accounts is built in the following order:
 * - path[0] always contains account for Leaf Signer
 * - path[1] Nested account
 * ...
 * - path[n - 1] Nested account
 * - path[n] is always Selected account
 *
 */
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
