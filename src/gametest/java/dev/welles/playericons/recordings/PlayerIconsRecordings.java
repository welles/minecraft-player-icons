package dev.welles.playericons.recordings;

import com.mojang.authlib.GameProfile;
import dev.welles.playericons.PlayerIcons;
import dev.welles.playericons.PlayerIconsClient;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Plays scenes of the mod and saves every tick as a frame in {@code recordings/<scene>/} of the run directory, for
 * {@code record-gifs.sh} to turn into GIFs. Not a test: it checks nothing and only runs on request.
 * <p>
 * The other players are bots on the integrated server: server players with a connection that leads nowhere, which the
 * server and the client treat like real players, so the client gets their skins and positions as usual.
 */
public class PlayerIconsRecordings implements FabricClientGameTest {
    private static final int WIDTH = 854;
    private static final int HEIGHT = 480;
    /** The flat world's surface, where the players stand. */
    private static final int GROUND = -60;
    /** Blocks per tick of a walking player. */
    private static final double WALK_SPEED = 0.215;

    /** The connections of the bots; the packets the server sends them pile up and are thrown away every frame. */
    private static final List<EmbeddedChannel> CHANNELS = new ArrayList<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        // A bit larger than the defaults, so faces and text can be made out in a GIF of 480 pixels width
        setConfig("faceSize=16\ntextScale=1.0\n");
        context.runOnClient(minecraft -> minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0));
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            server.runCommand("gamemode creative @a");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            // a calm floor: the noisy grass texture changes in every frame of a moving view and bloats the GIFs
            for (int x = -64; x < 64; x += 64) {
                for (int z = -64; z < 64; z += 64) {
                    server.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:light_gray_concrete",
                        x, GROUND - 1, z, x + 63, GROUND - 1, z + 63));
                }
            }
            server.runCommand(String.format(Locale.ROOT, "tp @a 0.5 %d 0.5 0 0", GROUND));
            singleplayer.getClientWorld().waitForChunksRender();

            // One short scene per feature, so each GIF fits into 2 MB at a high frame rate
            floatingFaces(context, server);
            fadeOut(context, server);
            screenEdge(context, server);
            toggle(context, server);
        }
    }

    /** Looking ahead: the faces float above the players in the distance and follow one of them walking by. */
    private static void floatingFaces(ClientGameTestContext context, TestServerContext server) {
        ServerPlayer lena = spawn(server, LENA, -9.5, 17.5);
        ServerPlayer tom = spawn(server, TOM, 13.5, 32.5);
        ServerPlayer max = spawn(server, MAX, -11.5, 25.5);
        look(context, 0, 4);
        context.waitTicks(40);

        Recorder recorder = new Recorder(context, server, "floating-faces");
        recorder.frames(15);
        walk(recorder, server, max, -11.5, 25.5, 6.5, 25.5);
        recorder.frames(15);

        remove(server, lena, tom, max);
    }

    /** A player walks up to the camera: the distance counts down and the face fades out once the player is close. */
    private static void fadeOut(ClientGameTestContext context, TestServerContext server) {
        ServerPlayer lena = spawn(server, LENA, -12.5, 30.5);
        ServerPlayer max = spawn(server, MAX, 6.5, 24.5);
        look(context, 0, 8);
        context.waitTicks(40);

        Recorder recorder = new Recorder(context, server, "fade-out");
        recorder.frames(15);
        walk(recorder, server, max, 6.5, 24.5, 1.5, 3.5);
        recorder.frames(25);

        remove(server, lena, max);
    }

    /**
     * The camera turns around once: the faces of the players outside the view wait at the edge of the screen in the
     * direction of their players and move into the view with them.
     */
    private static void screenEdge(ClientGameTestContext context, TestServerContext server) {
        ServerPlayer lena = spawn(server, LENA, -22.5, 24.5);
        ServerPlayer max = spawn(server, MAX, 30.5, 8.5);
        ServerPlayer tom = spawn(server, TOM, -6.5, -30.5);
        look(context, 0, 4);
        context.waitTicks(40);

        Recorder recorder = new Recorder(context, server, "screen-edge");
        recorder.frames(15);
        int turnTicks = 170;
        for (int i = 1; i <= turnTicks; i++) {
            float t = (float) i / turnTicks;
            // eases in and out
            t = t * t * (3 - 2 * t);
            look(context, 360 * t, 4);
            recorder.frame();
        }
        recorder.frames(15);

        remove(server, lena, max, tom);
    }

    /** The toggle key hides the icons and shows them again, the new state is shown above the hotbar. */
    private static void toggle(ClientGameTestContext context, TestServerContext server) {
        ServerPlayer lena = spawn(server, LENA, -9.5, 17.5);
        ServerPlayer tom = spawn(server, TOM, 11.5, 30.5);
        ServerPlayer max = spawn(server, MAX, 1.5, 24.5);
        look(context, 0, 4);
        context.waitTicks(40);

        Recorder recorder = new Recorder(context, server, "toggle");
        recorder.frames(20);
        context.getInput().pressKey(PlayerIconsClient.TOGGLE);
        recorder.frames(35);
        context.getInput().pressKey(PlayerIconsClient.TOGGLE);
        recorder.frames(35);

        remove(server, lena, tom, max);
    }

    /** The other players; the UUID picks the default skin. */
    private static final GameProfile LENA = new GameProfile(UUID.fromString("6f1a3c2e-8d4b-4e7a-9c1f-2b5d7e9a0c31"), "Lena");
    private static final GameProfile MAX = new GameProfile(UUID.fromString("c7b2e9f4-1a3d-4f6c-8e2b-9d4a6c1e3f57"), "Max");
    private static final GameProfile TOM = new GameProfile(UUID.fromString("a3d9e4b1-7c2f-4a8e-b5d6-1e3f9c7a2b48"), "Tom");

    /** Joins a bot and puts it at x, z facing the camera. */
    private static ServerPlayer spawn(TestServerContext server, GameProfile profile, double x, double z) {
        ServerPlayer bot = server.computeOnServer(minecraftServer -> {
            // all skin layers, like players have them by default
            ClientInformation information = new ClientInformation("en_us", 2, ChatVisiblity.FULL, true, 0x7F, HumanoidArm.RIGHT,
                false, true, ParticleStatus.ALL);
            ServerPlayer player = new ServerPlayer(minecraftServer, minecraftServer.overworld(), profile, information);
            minecraftServer.getPlayerList().placeNewPlayer(new BotConnection(), player, new CommonListenerCookie(profile, 0, information, false));
            return player;
        });
        move(server, bot, x, z, facingCamera(x, z));
        return bot;
    }

    private static void remove(TestServerContext server, ServerPlayer... bots) {
        server.runOnServer(minecraftServer -> {
            for (ServerPlayer bot : bots) minecraftServer.getPlayerList().remove(bot);
        });
    }

    /** Walks a bot in a straight line, looking where it goes. */
    private static void walk(Recorder recorder, TestServerContext server, ServerPlayer bot, double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        int ticks = (int) Math.ceil(Math.hypot(dx, dz) / WALK_SPEED);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        for (int i = 1; i <= ticks; i++) {
            double t = (double) i / ticks;
            move(server, bot, fromX + dx * t, fromZ + dz * t, yaw);
            recorder.frame();
        }
    }

    private static void move(TestServerContext server, ServerPlayer bot, double x, double z, float yaw) {
        server.runOnServer(minecraftServer -> {
            bot.snapTo(x, GROUND, z, yaw, 0);
            bot.setYHeadRot(yaw);
            bot.setYBodyRot(yaw);
        });
    }

    /** The yaw that faces the camera in the middle of the floor. */
    private static float facingCamera(double x, double z) {
        return (float) Math.toDegrees(Math.atan2(x - 0.5, -(z - 0.5)));
    }

    /** Turns the player, without the interpolation from the last rotation. */
    private static void look(ClientGameTestContext context, float yaw, float pitch) {
        context.runOnClient(minecraft -> {
            var player = minecraft.player;
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.yRotO = yaw;
            player.xRotO = pitch;
        });
    }

    private static void setConfig(String content) {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(PlayerIcons.MOD_ID + ".properties");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A connection to nowhere for a bot: everything the server sends it stays in an embedded channel. */
    private static final class BotConnection extends Connection {
        BotConnection() {
            super(PacketFlow.SERVERBOUND);
            EmbeddedChannel channel = new EmbeddedChannel();
            CHANNELS.add(channel);
            try {
                Field field = Connection.class.getDeclaredField("channel");
                field.setAccessible(true);
                field.set(this, channel);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        // a real connection switches the protocol handlers of its channel here, the embedded channel has none

        @Override
        public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener) {}

        @Override
        public void setupOutboundProtocol(ProtocolInfo<?> protocolInfo) {}

        @Override
        public void setReadOnly() {}

        @Override
        public void handleDisconnection() {}
    }

    /** Saves numbered frames of one scene; taking a screenshot advances the game by one tick. */
    private static final class Recorder {
        private final ClientGameTestContext context;
        private final TestServerContext server;
        private final Path dir;
        private int frame;

        Recorder(ClientGameTestContext context, TestServerContext server, String scene) {
            this.context = context;
            this.server = server;
            this.dir = FabricLoader.getInstance().getGameDir().resolve("recordings").resolve(scene);
        }

        void frame() {
            // join messages, toasts and the packets sent to the bots would pile up
            context.runOnClient(minecraft -> {
                minecraft.getToastManager().clear();
                minecraft.gui.getChat().clearMessages(false);
            });
            server.runOnServer(minecraftServer -> CHANNELS.forEach(EmbeddedChannel::releaseOutbound));
            context.takeScreenshot(TestScreenshotOptions.of(String.format(Locale.ROOT, "%04d", frame++))
                .disableCounterPrefix().withSize(WIDTH, HEIGHT).withDestinationDir(dir));
        }

        void frames(int count) {
            for (int i = 0; i < count; i++) frame();
        }
    }
}
