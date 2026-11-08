package dev.welles.playericons;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
//?}
//? if <26.1
import dev.welles.playericons.mixin.GameRendererAccessor;
//? if <1.21.2
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Draws the faces of the other players on the HUD: where the player is while they are in view, otherwise at the edge
 * of the screen in the direction the camera has to turn to see them.
 */
public final class IconRenderer {
    /** GUI pixels between the icons and the edge of the screen. */
    private static final float EDGE_MARGIN = 4;
    /** GUI pixels between the face and the label below it. */
    private static final float LABEL_GAP = 1;
    /** The icon floats this far above the player's head, in blocks. */
    private static final double HEAD_GAP = 0.3;
    /** Height of a standing player, for players the client only knows from the server. */
    private static final double PLAYER_HEIGHT = 1.8;
    /** Icons fainter than this are not drawn. */
    private static final float MIN_ALPHA = 0.02F;

    private IconRenderer() {}

    private record Target(UUID uuid, String name, Vec3 feet, double height, double distance) {}

    //? if >=26.1 {
    /*public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, boolean hudHidden) {
    *///?} else {
    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker, boolean hudHidden) {
    //?}
        if (hudHidden || !PlayerIconsClient.isVisible()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        ClientPacketListener connection = minecraft.getConnection();
        if (self == null || level == null || connection == null) return;
        //? if >=26.2 {
        /*Camera camera = minecraft.gameRenderer.mainCamera();
        *///?} else {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        //?}
        if (!camera.isInitialized()) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        IconsConfig config = IconsConfig.get();
        List<Target> targets = collectTargets(minecraft, self, level, partialTick);
        if (targets.isEmpty()) return;
        // the nearest players are drawn last, on top
        targets.sort(Comparator.comparingDouble(Target::distance).reversed());

        //? if >=1.21.11 {
        /*Vec3 cameraPosition = camera.position();
        *///?} else {
        Vec3 cameraPosition = camera.getPosition();
        //?}
        Quaternionf toCamera = new Quaternionf(camera.rotation()).conjugate();
        //? if >=26.1 {
        /*double fov = camera.getFov();
        *///?} else {
        double fov = ((GameRendererAccessor) minecraft.gameRenderer).playericons$getFov(camera, partialTick, true);
        //?}
        float width = graphics.guiWidth();
        float height = graphics.guiHeight();
        // pixels per unit at a distance of one: the projection of the vertical field of view onto the screen
        float focal = (float) (height / 2 / Math.tan(Math.toRadians(fov) / 2));

        Font font = minecraft.font;
        boolean face = config.showFace;
        boolean labels = config.showNames || config.showDistance;
        if (!face && !labels) return;
        int size = face ? config.faceSize : 0;
        float textScale = (float) config.textScale;
        float labelHeight = labels ? font.lineHeight * textScale : 0;
        // How far the icon reaches above and below its center: the face with the label below it, or the label alone,
        // centered where the face would be
        float above = face ? size / 2F : labelHeight / 2;
        float below = face ? size / 2F + (labels ? labelHeight + LABEL_GAP : 0) : labelHeight / 2;
        // the area for the centers of the icons, so face and label stay on the screen; labels move inwards themselves
        float left = EDGE_MARGIN + size / 2F;
        float right = width - EDGE_MARGIN - size / 2F;
        float top = EDGE_MARGIN + above;
        float bottom = height - EDGE_MARGIN - below;
        float centerX = width / 2;
        float centerY = height / 2;
        if (left >= right || top >= bottom) return;

        for (Target target : targets) {
            float alpha = alpha(target.distance(), config);
            if (alpha < MIN_ALPHA) continue;
            PlayerInfo info = connection.getPlayerInfo(target.uuid());
            if (info == null) continue;

            Vec3 head = target.feet().add(0, target.height() + HEAD_GAP, 0);
            // camera space: x to the right, y up, the camera looks along -z
            Vector3f view = new Vector3f(
                (float) (head.x - cameraPosition.x), (float) (head.y - cameraPosition.y), (float) (head.z - cameraPosition.z)
            ).rotate(toCamera);
            float depth = -view.z;

            float x = 0;
            float y = 0;
            boolean inView = false;
            if (depth > 0.01F) {
                x = centerX + view.x / depth * focal;
                // face and label stand on the point above the head, so they do not cover the player
                y = centerY - view.y / depth * focal - below;
                inView = x >= left && x <= right && y >= top && y <= bottom;
            }
            if (!inView) {
                // At the edge of the screen in the direction of the player. Players behind the camera go to the side
                // the camera has to turn to, the further behind, the closer to the middle of that side.
                float dx = depth > 0.01F ? view.x : Math.copySign(Math.max(Math.abs(view.x), view.z), view.x);
                float dy = -view.y;
                if (dx == 0 && dy == 0) dy = 1;
                float scale = Float.MAX_VALUE;
                if (dx > 0) scale = Math.min(scale, (right - centerX) / dx);
                else if (dx < 0) scale = Math.min(scale, (left - centerX) / dx);
                if (dy > 0) scale = Math.min(scale, (bottom - centerY) / dy);
                else if (dy < 0) scale = Math.min(scale, (top - centerY) / dy);
                x = centerX + dx * scale;
                y = centerY + dy * scale;
            }

            if (face) drawFace(graphics, info, x - size / 2F, y - size / 2F, size, alpha);
            float labelY = face ? y + size / 2F + LABEL_GAP : y - labelHeight / 2;
            if (labels) drawLabel(graphics, font, label(target, config), x, labelY, textScale, alpha, width);
        }
    }

    /** The other players: from the client's world if they are close enough to be in it, from the server otherwise. */
    private static List<Target> collectTargets(Minecraft minecraft, LocalPlayer self, ClientLevel level, float partialTick) {
        Vec3 position = self.getPosition(partialTick);
        List<Target> targets = new ArrayList<>();
        Set<UUID> inWorld = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            if (player == self || player == minecraft.getCameraEntity()) continue;
            inWorld.add(player.getUUID());
            // Like vanilla: spectators and invisible players are only seen by spectators
            if (player.isSpectator() && !self.isSpectator() || player.isInvisibleTo(self)) continue;
            Vec3 feet = player.getPosition(partialTick);
            targets.add(new Target(player.getUUID(), player.getName().getString(), feet, player.getBbHeight(), feet.distanceTo(position)));
        }
        long now = System.nanoTime();
        for (Map.Entry<UUID, PlayerIconsClient.RemotePlayer> entry : PlayerIconsClient.remotePlayers().entrySet()) {
            if (inWorld.contains(entry.getKey()) || entry.getKey().equals(self.getUUID())) continue;
            Vec3 feet = entry.getValue().position(now);
            targets.add(new Target(entry.getKey(), entry.getValue().name(), feet, PLAYER_HEIGHT, feet.distanceTo(position)));
        }
        return targets;
    }

    /** Invisible within the hide distance, fading in over the fade distance beyond it. */
    private static float alpha(double distance, IconsConfig config) {
        double beyond = distance - config.hideDistance;
        if (beyond <= 0) return 0;
        double fade = config.fadeDistance <= 0 ? 1 : Math.min(1, beyond / config.fadeDistance);
        return (float) (fade * config.opacity);
    }

    private static String label(Target target, IconsConfig config) {
        String distance = Math.round(target.distance()) + " m";
        if (config.showNames && config.showDistance) return target.name() + " · " + distance;
        return config.showNames ? target.name() : distance;
    }

    private static int color(float alpha) {
        return Math.round(alpha * 255) << 24 | 0xFFFFFF;
    }

    //? if >=26.1 {
    /*private static void drawFace(GuiGraphicsExtractor graphics, PlayerInfo info, float x, float y, int size, float alpha) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        PlayerFaceExtractor.extractRenderState(graphics, info.getSkin(), 0, 0, size, color(alpha));
        graphics.pose().popMatrix();
    }

    private static void drawLabel(GuiGraphicsExtractor graphics, Font font, String text, float x, float y, float scale, float alpha, float screenWidth) {
        Component label = Component.literal(text);
        float halfWidth = font.width(label) * scale / 2;
        x = Math.clamp(x, halfWidth + 1, Math.max(halfWidth + 1, screenWidth - halfWidth - 1));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, label, -font.width(label) / 2, 0, color(alpha), true);
        graphics.pose().popMatrix();
    }
    *///?} else {
    private static void drawFace(GuiGraphics graphics, PlayerInfo info, float x, float y, int size, float alpha) {
        //? if >=1.21.6 {
        /*graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        PlayerFaceRenderer.draw(graphics, info.getSkin(), 0, 0, size, color(alpha));
        graphics.pose().popMatrix();
        *///?} else if >=1.21.2 {
        /*graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        PlayerFaceRenderer.draw(graphics, info.getSkin(), 0, 0, size, color(alpha));
        graphics.pose().popPose();
        *///?} else {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        PlayerFaceRenderer.draw(graphics, info.getSkin(), 0, 0, size);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        graphics.pose().popPose();
        //?}
    }

    private static void drawLabel(GuiGraphics graphics, Font font, String text, float x, float y, float scale, float alpha, float screenWidth) {
        Component label = Component.literal(text);
        float halfWidth = font.width(label) * scale / 2;
        x = Math.clamp(x, halfWidth + 1, Math.max(halfWidth + 1, screenWidth - halfWidth - 1));
        //? if >=1.21.6 {
        /*graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, label, -font.width(label) / 2, 0, color(alpha), true);
        graphics.pose().popMatrix();
        *///?} else {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, label, -font.width(label) / 2, 0, color(alpha), true);
        graphics.pose().popPose();
        //?}
    }
    //?}
}
