package dev.welles.playericons.mixin;

//? if <26.1 {
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The field of view of the world, including sprinting, the spyglass and other effects. Only before 26.1, since then the camera knows it. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    //? if >=1.21.2 {
    /*@Invoker("getFov")
    float playericons$getFov(Camera camera, float partialTick, boolean useFovSetting);
    *///?} else {
    @Invoker("getFov")
    double playericons$getFov(Camera camera, float partialTick, boolean useFovSetting);
    //?}
}
//?}
