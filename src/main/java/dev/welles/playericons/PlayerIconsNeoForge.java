package dev.welles.playericons;

//? if neoforge {
/*import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(PlayerIcons.MOD_ID)
public class PlayerIconsNeoForge {
    public PlayerIconsNeoForge(IEventBus modBus) {
        PlayerIcons.init(FMLPaths.CONFIGDIR.get(), new PlayerIcons.Network() {
            @Override
            public boolean canSend(ServerPlayer player) {
                return player.connection.hasChannel(PositionsPayload.TYPE);
            }

            @Override
            public void send(ServerPlayer player, CustomPacketPayload payload) {
                player.connection.send(payload);
            }
        });
        modBus.addListener(PlayerIconsNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> PlayerIcons.onServerTick(event.getServer()));
    }

    // Optional channel: players without the mod can join, clients with the mod can join servers without it.
    // The handler runs on the main thread and only on clients.
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional()
            .playToClient(PositionsPayload.TYPE, PositionsPayload.CODEC, (payload, context) -> PlayerIconsClient.onPositions(payload));
    }
}
*///?}
