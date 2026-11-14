package com.ormonger.module;

public enum Category {
    COMBAT("Combat"),
    DEFENSE("Defense"),
    TRAVEL("Travel"),
    RENDER("Render"),
    MISC("Misc");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
