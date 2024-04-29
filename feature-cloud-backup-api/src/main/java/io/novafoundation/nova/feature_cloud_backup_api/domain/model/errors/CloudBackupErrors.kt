package io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.CloudBackupDiff

interface CloudBackupAuthFailed

interface CloudBackupServiceUnavailable

interface CloudBackupExistingBackupFound

interface CloudBackupNotFound

interface CloudBackupNotEnoughSpace

interface CloudBackupUnknownError

interface CorruptedBackupError

class CannotApplyNonDestructiveDiff(val cloudBackupDiff: CloudBackupDiff, val cloudBackup: CloudBackup) : Throwable()

class PasswordNotSaved : Throwable()

class InvalidBackupPasswordError : Throwable()
