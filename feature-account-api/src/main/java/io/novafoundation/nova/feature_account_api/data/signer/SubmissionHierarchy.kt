package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

class SubmissionHierarchy(
    val path: List<MetaAccount>
) {

    constructor(metaAccount: MetaAccount) : this(listOf(metaAccount))

    operator fun plus(submissionHierarchy: SubmissionHierarchy): SubmissionHierarchy {
        return SubmissionHierarchy(path + submissionHierarchy.path)
    }
}
