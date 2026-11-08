package dev.welles.playericons;

//? if neoforge {
/*import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = PlayerIcons.MOD_ID, dist = Dist.CLIENT)
public class PlayerIconsNeoForgeClient {
    public PlayerIconsNeoForgeClient(IEventBus modBus, ModContainer container) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(PlayerIconsClient.TOGGLE));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> PlayerIconsClient.tick(Minecraft.getInstance()));
        // "Config" button in the mod list
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(
            parent, FMLPaths.CONFIGDIR.get().resolve(PlayerIcons.MOD_ID + ".properties"), Component.literal(mod.getModInfo().getDisplayName()),
            IconsConfig.defaults(), IconsConfig.RANGES, IconsConfig.CHOICES
        ));
    }
}
*///?}
