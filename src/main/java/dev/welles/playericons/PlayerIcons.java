package dev.welles.playericons;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/**
 * Server side: sends every player with the mod the positions of the other players in their dimension. A client only
 * knows the players within its render distance by itself, with the positions from the server it can show everyone.
 */
public final class PlayerIcons {
    public static final String MOD_ID = "playericons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Ticks between two position updates, the client moves the icons smoothly in between. */
    public static final int SEND_INTERVAL = 10;

    private static Network network;

    private PlayerIcons() {}

    /** Loader specific networking on the server. */
    public interface Network {
        /** Whether the player's client has the mod. */
        boolean canSend(ServerPlayer player);

        void send(ServerPlayer player, CustomPacketPayload payload);
    }

    public static void init(Path configDir, Network network) {
        PlayerIcons.network = network;
        IconsConfig.init(configDir);
        LOGGER.info("Player Icons loaded");
    }

    //? if >=1.21.11 {
    /*public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
    *///?} else {
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    //?}

    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % SEND_INTERVAL != 0 || !IconsConfig.get().sharePositions) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        // Nobody to show on a server with a single player, e.g. in singleplayer
        if (players.size() < 2) return;

        for (ServerPlayer receiver : players) {
            if (!network.canSend(receiver)) continue;
            List<PositionsPayload.Entry> entries = new ArrayList<>();
            for (ServerPlayer other : players) {
                if (other == receiver || other.level() != receiver.level()) continue;
                // Like vanilla: spectators and invisible players are only seen by spectators
                if (!receiver.isSpectator() && (other.isSpectator() || other.isInvisible())) continue;
                entries.add(new PositionsPayload.Entry(other.getUUID(), other.getName().getString(),other.getX(), other.getY(), other.getZ()));
            }
            network.send(receiver, new PositionsPayload(entries));
        }
    }
}
