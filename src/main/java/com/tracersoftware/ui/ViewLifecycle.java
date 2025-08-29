package com.tracersoftware.ui;

public interface ViewLifecycle {
    /** Called when the view has been attached to the UI and is visible - use to start background loads */
    void onViewShown();
}
