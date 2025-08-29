package com.tracersoftware.ui.menu;

public interface CurrentUserProvider {
    String getDisplayName();
    String getUsername();
    default String getAvatarUrl() { return null; }
}
