package dev.welles.playericons.mixin;

import dev.welles.playericons.IconRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
/*import net.minecraft.client.gui.Hud;
*///?} else {
import net.minecraft.client.gui.Gui;
//?}
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

/** Draws the player icons on top of the rest of the HUD. Not while the HUD is hidden with F1. */
//? if >=26.2 {
/*@Mixin(Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
public abstract class GuiMixin {
    @Shadow @Final private Minecraft minecraft;

    //? if >=26.2 {
    /*@Shadow
    public abstract boolean isHidden();

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void playericons$icons(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        IconRenderer.render(graphics, deltaTracker, isHidden());
    }
    *///?} else if >=26.1 {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void playericons$icons(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        IconRenderer.render(graphics, deltaTracker, minecraft.options.hideGui);
    }
    *///?} else {
    @Inject(method = "render", at = @At("TAIL"))
    private void playericons$icons(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        IconRenderer.render(graphics, deltaTracker, minecraft.options.hideGui);
    }
    //?}
}
