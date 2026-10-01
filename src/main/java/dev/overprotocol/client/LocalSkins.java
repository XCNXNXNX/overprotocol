package dev.overprotocol.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overprotocol.Overprotocol;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Skin images picked off the player's own disk. The chosen file is copied into
 * {@code <gameDir>/overprotocol_skins/<id>.png} and registered as a texture, so the statue keeps
 * wearing it across restarts. Only the client that owns the file can see it.
 *
 * <p>The vanilla skin pipeline validates the image size and converts legacy 64x32 sheets; a hand
 * rolled texture has to do the same, otherwise the second skin layer samples the wrong rows.
 */
@OnlyIn(Dist.CLIENT)
public final class LocalSkins {
    private static final String FOLDER = "overprotocol_skins";
    private static final Map<String, PlayerSkin> CACHE = new HashMap<>();

    public static Path folder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FOLDER);
    }

    /** Opens the platform's file chooser off the render thread, then reports the id (or null). */
    public static void chooseFile(Consumer<String> onPicked) {
        var thread = new Thread(() -> {
            String id = null;
            try {
                var picked = openFileChooser();
                if (picked != null) id = importFile(picked);
            } catch (Throwable failure) {
                Overprotocol.LOGGER.warn("Could not read the chosen skin image", failure);
                complain("screen.overprotocol.honor_guard.file_failed");
            }
            String chosen = id;
            Minecraft.getInstance().execute(() -> onPicked.accept(chosen));
        }, "overprotocol-skin-picker");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Minecraft runs the JVM with {@code java.awt.headless=true}, so an in-process
     * {@code FileDialog} always throws {@code HeadlessException}. Windows therefore gets a real
     * file manager window from a short-lived helper process, which has its own AWT.
     */
    private static Path openFileChooser() throws IOException, InterruptedException {
        if (net.minecraft.Util.getPlatform() == net.minecraft.Util.OS.WINDOWS) {
            // The helper writes the path to a file instead of stdout: PowerShell decorates its
            // output stream with BOMs and progress records, which silently corrupts a piped path.
            Path handoff = Files.createTempFile("overprotocol-skin", ".txt");
            try {
                String script = "Add-Type -AssemblyName System.Windows.Forms;"
                    + "$f=New-Object System.Windows.Forms.OpenFileDialog;"
                    + "$f.Filter='PNG image (*.png)|*.png';"
                    + "$f.Title='Select a 64x64 skin PNG';"
                    + "if($f.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK){"
                    + "[IO.File]::WriteAllText('" + handoff + "', $f.FileName, "
                    + "(New-Object System.Text.UTF8Encoding($false)))}";
                var process = new ProcessBuilder("powershell", "-NoProfile", "-STA", "-Command", script).start();
                process.getOutputStream().close();
                process.waitFor();
                String picked = readHandoff(handoff);
                if (picked == null) {
                    Overprotocol.LOGGER.info("Skin picker returned no file (cancelled or blocked)");
                    return null;
                }
                Path path = Path.of(picked);
                if (!Files.isRegularFile(path)) {
                    Overprotocol.LOGGER.warn("Skin picker returned a path that is not a file: '{}'", picked);
                    return null;
                }
                Overprotocol.LOGGER.info("Skin picker chose {}", path);
                return path;
            } finally {
                Files.deleteIfExists(handoff);
            }
        }
        return openAwtChooser();
    }

    /** Reads the handed over path, tolerating a BOM, stray quotes or trailing newlines. */
    private static String readHandoff(Path handoff) throws IOException {
        String raw = Files.readString(handoff, StandardCharsets.UTF_8);
        if (!raw.isEmpty() && raw.charAt(0) == '\uFEFF') raw = raw.substring(1);
        raw = raw.strip().replace("\r", "").replace("\n", "");
        if (raw.length() > 1 && raw.startsWith("\"") && raw.endsWith("\"")) raw = raw.substring(1, raw.length() - 1);
        return raw.isEmpty() ? null : raw;
    }

    private static Path openAwtChooser() {
        var dialog = new java.awt.FileDialog((java.awt.Frame) null, "Select a 64x64 skin PNG", java.awt.FileDialog.LOAD);
        dialog.setFile("*.png");
        dialog.setFilenameFilter((directory, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"));
        dialog.setMultipleMode(false);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        var directory = dialog.getDirectory();
        var name = dialog.getFile();
        return directory == null || name == null ? null : Path.of(directory, name);
    }

    private static String importFile(Path source) throws IOException {
        byte[] bytes = Files.readAllBytes(source);
        // Reject anything the vanilla-sized layer UVs cannot address before it reaches a texture.
        try (InputStream probe = Files.newInputStream(source)) {
            var image = NativeImage.read(probe);
            int width = image.getWidth();
            int height = image.getHeight();
            image.close();
            if (width != 64 || height != 64) {
                Overprotocol.LOGGER.warn("Rejected {} : it is {}x{}, but 64x64 is required", source, width, height);
                complain("screen.overprotocol.honor_guard.bad_size");
                return null;
            }
            Overprotocol.LOGGER.info("Imported skin {} ({}x{})", source.getFileName(), width, height);
        }
        String id = digest(bytes);
        Path directory = folder();
        Files.createDirectories(directory);
        Path target = directory.resolve(id + ".png");
        Files.write(target, bytes);
        CACHE.remove(id);
        return id;
    }

    /** The skin for an imported id, or null when the image is missing or unreadable. */
    public static PlayerSkin skin(String id) {
        if (id == null || id.isEmpty()) return null;
        var cached = CACHE.get(id);
        if (cached != null) return cached;
        Path file = folder().resolve(id + ".png");
        if (!Files.isRegularFile(file)) return null;
        try (InputStream stream = Files.newInputStream(file)) {
            var image = NativeImage.read(stream);
            if (image.getWidth() != 64 || image.getHeight() != 64) {
                image.close();
                return null;
            }
            var model = detectModel(image);
            var texture = ResourceLocation.fromNamespaceAndPath(Overprotocol.MOD_ID, "skins/" + id);
            Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(image));
            var skin = new PlayerSkin(texture, null, null, null, model, false);
            CACHE.put(id, skin);
            return skin;
        } catch (IOException failure) {
            Overprotocol.LOGGER.warn("Could not load the local skin {}", id, failure);
            return null;
        }
    }

    /**
     * A slim skin leaves the fourth column of every four wide arm face empty; the vanilla client
     * reads the same fact from the profile metadata, which a plain PNG does not carry.
     */
    private static PlayerSkin.Model detectModel(NativeImage image) {
        // A 4px arm needs 2*depth + 2*width = 16 texture columns (40..55); a 3px slim arm needs 14
        // (40..53). So an unpainted 54/55 column pair is the reliable tell, whereas the inner
        // columns are routinely padded by skin artists.
        boolean slim = true;
        for (int y = 20; y < 32 && slim; y++) {
            if (opaque(image, 54, y) || opaque(image, 55, y)) slim = false;
        }
        return slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;
    }

    private static boolean opaque(NativeImage image, int x, int y) {
        return (image.getPixelRGBA(x, y) >>> 24) != 0;
    }

    private static void complain(String key) {
        Minecraft.getInstance().execute(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) player.displayClientMessage(Component.translatable(key), false);
        });
    }

    private static String digest(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(bytes);
            var builder = new StringBuilder();
            for (int i = 0; i < 8; i++) builder.append(String.format("%02x", hash[i]));
            return builder.toString();
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private LocalSkins() {}
}
