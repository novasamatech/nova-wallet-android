package io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors

interface CloudBackupAuthFailed

interface CloudBackupServiceUnavailable

interface CloudBackupExistingBackupFound

interface CloudBackupNotFound

interface CloudBackupNotEnoughSpace

interface CloudBackupUnknownError

interface CorruptedBackupError

class CannotApplyNonDestructiveDiff : Throwable()

class PasswordNotSaved : Throwable()

class InvalidBackupPasswordError : Throwable()
