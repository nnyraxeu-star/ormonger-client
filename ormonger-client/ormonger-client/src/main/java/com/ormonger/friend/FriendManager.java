package com.ormonger.friend;

import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Keeps the list of allied player names. Modules call {@link #isFriend(PlayerEntity)}
 * to skip allies (e.g. tracers, PlayerESP colouring, CrystalAura targets).
 * Names are compared case-insensitively.
 */
public final class FriendManager {
    private final Set<String> friends = new TreeSet<>();

    public boolean add(String name) {
        return name != null && !name.isBlank() && friends.add(normalize(name));
    }

    public boolean remove(String name) {
        return name != null && friends.remove(normalize(name));
    }

    /** Adds the player if missing, removes them if present. Returns true when they are now a friend. */
    public boolean toggle(String name) {
        if (isFriend(name)) {
            remove(name);
            return false;
        }
        add(name);
        return true;
    }

    public boolean isFriend(String name) {
        return name != null && friends.contains(normalize(name));
    }

    public boolean isFriend(PlayerEntity player) {
        return player != null && isFriend(player.getName().getString());
    }

    public List<String> getFriends() {
        return Collections.unmodifiableList(new ArrayList<>(friends));
    }

    public void setAll(Iterable<String> names) {
        friends.clear();
        for (String name : names) {
            add(name);
        }
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
