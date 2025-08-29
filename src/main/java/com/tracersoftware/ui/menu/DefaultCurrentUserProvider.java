package com.tracersoftware.ui.menu;

public class DefaultCurrentUserProvider implements CurrentUserProvider {

    private final String username;
    private final String displayName;
    private final String avatarUrl;

    public DefaultCurrentUserProvider(String username, String displayName) {
        this(username, displayName, null);
    }

    public DefaultCurrentUserProvider(String username, String displayName, String avatarUrl) {
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
    }

    @Override
    public String getDisplayName() {
        return displayName == null ? username : displayName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getAvatarUrl() {
        return avatarUrl;
    }
}
