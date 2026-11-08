package dev.welles.playericons;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
*///?} else {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?}

public class PlayerIconsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Handlers run on the client thread
        ClientPlayNetworking.registerGlobalReceiver(PositionsPayload.TYPE, (payload, context) -> PlayerIconsClient.onPositions(payload));

        //? if >=26.1 {
        /*KeyMappingHelper.registerKeyMapping(PlayerIconsClient.TOGGLE);
        *///?} else {
        KeyBindingHelper.registerKeyBinding(PlayerIconsClient.TOGGLE);
        //?}
        ClientTickEvents.END_CLIENT_TICK.register(PlayerIconsClient::tick);
    }
}
//?}
