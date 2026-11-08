package dev.welles.playericons;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class PlayerIconsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PlayerIcons.init(FabricLoader.getInstance().getConfigDir(), new PlayerIcons.Network() {
            @Override
            public boolean canSend(ServerPlayer player) {
                return ServerPlayNetworking.canSend(player, PositionsPayload.TYPE);
            }

            @Override
            public void send(ServerPlayer player, CustomPacketPayload payload) {
                ServerPlayNetworking.send(player, payload);
            }
        });

        //? if >=26.1 {
        /*PayloadTypeRegistry.clientboundPlay().register(PositionsPayload.TYPE, PositionsPayload.CODEC);
        *///?} else {
        PayloadTypeRegistry.playS2C().register(PositionsPayload.TYPE, PositionsPayload.CODEC);
        //?}
        ServerTickEvents.END_SERVER_TICK.register(PlayerIcons::onServerTick);
    }
}
//?}
