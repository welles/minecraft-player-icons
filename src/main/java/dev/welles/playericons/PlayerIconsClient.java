package dev.welles.playericons;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Client side: the toggle key, whether the icons are shown and the positions the server sent. */
public final class PlayerIconsClient {
    //? if >=1.21.9 {
    /*private static final KeyMapping.Category CATEGORY = KeyMapping.Category.MULTIPLAYER;
    *///?} else {
    private static final String CATEGORY = "key.categories.multiplayer";
    //?}
    //? if >=26.3 {
    /*public static final KeyMapping TOGGLE = new KeyMapping("key.playericons.toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_J, CATEGORY);
    *///?} else {
    public static final KeyMapping TOGGLE = new KeyMapping("key.playericons.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_J, CATEGORY);
    //?}

    /** Time in which the icons move from one position update to the next. */
    private static final long INTERPOLATION_NANOS = PlayerIcons.SEND_INTERVAL * 50_000_000L;

    private static boolean visible;
    private static Boolean configuredVisible;

    /** Players the server sent, by UUID; only valid for {@link #remoteLevel}. */
    private static final Map<UUID, RemotePlayer> REMOTE = new HashMap<>();
    private static ClientLevel remoteLevel;

    private PlayerIconsClient() {}

    public static void tick(Minecraft minecraft) {
        IconsConfig config = IconsConfig.get();
        // the configured state applies on start and whenever it is changed in the file
        if (!Boolean.valueOf(config.visible).equals(configuredVisible)) visible = configuredVisible = config.visible;
        // positions from another dimension or server are of no use
        if (minecraft.level != remoteLevel) REMOTE.clear();

        while (TOGGLE.consumeClick()) toggle(minecraft);
    }

    private static void toggle(Minecraft minecraft) {
        visible = !visible;
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        Component message = Component.translatable(visible ? "playericons.shown" : "playericons.hidden");
        //? if >=26.1 {
        /*player.sendOverlayMessage(message);
        *///?} else {
        player.displayClientMessage(message, true);
        //?}
    }

    public static boolean isVisible() {
        return visible;
    }

    /** Called on the client thread with the positions the server sent. */
    public static void onPositions(PositionsPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != remoteLevel) {
            REMOTE.clear();
            remoteLevel = minecraft.level;
        }
        long now = System.nanoTime();
        Set<UUID> sent = new HashSet<>();
        for (PositionsPayload.Entry entry : payload.players()) {
            sent.add(entry.uuid());
            Vec3 position = new Vec3(entry.x(), entry.y(), entry.z());
            RemotePlayer known = REMOTE.get(entry.uuid());
            // continue from where the icon is now, so it does not jump if an update comes early or late
            Vec3 from = known == null ? position : known.position(now);
            REMOTE.put(entry.uuid(), new RemotePlayer(entry.name(), from, position, now));
        }
        REMOTE.keySet().retainAll(sent);
    }

    /** The players the server sent; empty if the server does not have the mod. */
    public static Map<UUID, RemotePlayer> remotePlayers() {
        return REMOTE;
    }

    /** A player that the server sent, moving from {@code from} to {@code to} within one update interval. */
    public record RemotePlayer(String name, Vec3 from, Vec3 to, long receivedAt) {
        public Vec3 position(long now) {
            double progress = Math.clamp((double) (now - receivedAt) / INTERPOLATION_NANOS, 0.0, 1.0);
            return from.lerp(to, progress);
        }
    }
}
