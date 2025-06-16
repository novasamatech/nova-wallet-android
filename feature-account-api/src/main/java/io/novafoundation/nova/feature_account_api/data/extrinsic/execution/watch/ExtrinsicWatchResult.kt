package io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch

import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus

class ExtrinsicWatchResult<T : ExtrinsicStatus>(
    val status: T,
    val submissionHierarchy: SubmissionHierarchy
)

fun List<ExtrinsicWatchResult<*>>.submissionHierarchy() = first().submissionHierarchy

inline fun <reified T : ExtrinsicStatus> ExtrinsicWatchResult<*>.mapWithStatus() = ExtrinsicWatchResult(status as T, submissionHierarchy)
