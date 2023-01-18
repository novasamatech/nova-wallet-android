package io.novafoundation.nova.feature_update_notification_impl.update;

import java.lang.System;

@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0006\b\u00c2\u0002\u0018\u00002\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00030\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0004J\u0018\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\u0003H\u0016J\u0018\u0010\t\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\u0003H\u0016J\u0018\u0010\n\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00022\u0006\u0010\b\u001a\u00020\u0002H\u0016J\u0018\u0010\u000b\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00022\u0006\u0010\b\u001a\u00020\u0002H\u0016\u00a8\u0006\f"}, d2 = {"Lio/novafoundation/nova/feature_update_notification_impl/update/DiffCallback;", "Lio/novafoundation/nova/common/list/BaseGroupedDiffCallback;", "Lio/novafoundation/nova/feature_update_notification_impl/update/UpdateNotificationAlertModel;", "Lio/novafoundation/nova/feature_update_notification_impl/update/UpdateNotificationModel;", "()V", "areChildContentsTheSame", "", "oldItem", "newItem", "areChildItemsTheSame", "areGroupContentsTheSame", "areGroupItemsTheSame", "feature-update-notification-impl_debug"})
final class DiffCallback extends io.novafoundation.nova.common.list.BaseGroupedDiffCallback<io.novafoundation.nova.feature_update_notification_impl.update.UpdateNotificationAlertModel, io.novafoundation.nova.feature_update_notification_impl.update.UpdateNotificationModel> {
    @org.jetbrains.annotations.NotNull()
    public static final io.novafoundation.nova.feature_update_notification_impl.update.DiffCallback INSTANCE = null;
    
    private DiffCallback() {
        super(null);
    }
    
    @java.lang.Override()
    public boolean areGroupItemsTheSame(@org.jetbrains.annotations.NotNull()
    io.novafoundation.nova.feature_update_notification_impl.update.UpdateNotificationAlertModel oldItem, @org.jetbrains.annotations.NotNull()
    io.novafoundation.nova.feature_update_notification_impl.update.UpdateNotificationAlertModel newItem) {
        return false;
    }
    
    @java.lang.Override()
    public boolean areChildItemsTheSame(@org.jetbrains.annotations.NotNull()
    io.novafoundation.nova.feature_update_notification_impl.update.UpdateNotificationModel oldItem, @org.jetbrains.annotations.NotNull()
    io.novafoundation.nova.feature_update_notification_impl.update.UpdateNotificationModel newItem) {
        return false;
    }
    
    @java.lang.Override()
    public boolean areGroupContentsTheSame(@org.jetbrains.annotations.NotNull()
    io.novafoundation.nova.feature_update_notification_impl.update.UpdateNotificationAlertModel oldItem, @org.jetbrains.annotations.NotNull()
    io.novafoundation.nova.feature_update_notification_impl.update.UpdateNotificationAlertModel newItem) {
        return false;
    }
    
    @java.lang.Override()
    public boolean areChildContentsTheSame(@org.jetbrains.annotations.NotNull()
    io.novafoundation.nova.feature_update_notification_impl.update.UpdateNotificationModel oldItem, @org.jetbrains.annotations.NotNull()
    io.novafoundation.nova.feature_update_notification_impl.update.UpdateNotificationModel newItem) {
        return false;
    }
}