package com.campersamu.chatheads.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
// Removed unused Map import to keep things clean

import static com.campersamu.chatheads.ChatHeadsInit.DEFAULT_HEAD_TEXTURE;
import static com.campersamu.chatheads.ChatHeadsInit.HEAD_CACHE;
import static com.campersamu.chatheads.ChatHeadsInit.LOGGER;
import static net.minecraft.text.TextColor.fromRgb;

@Mixin(PlayerManager.class)
public abstract class DownloadHeadOnJoin {
    //region Mixin Variables
    @Shadow
    @Final
    private MinecraftServer server;
    //endregion

    //Mixin into the player connect/join event and download the skin for the player (needs a server restart to update)
    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    private void chatheads$invokeDownloadOnJoin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        final var profile = player.getGameProfile();
        //Use a new Thread since downloading a skin is slow and would slow down the player joining process
        new Thread(() -> {
            synchronized (HEAD_CACHE) {
                // FIXED: profile.id()
                final TextColor[][] head = HEAD_CACHE.computeIfAbsent(profile.id(), uuid -> chatheads$getPlayerHead(profile, player));
                HEAD_CACHE.put(profile.id(), head);
            }
        }).start();
    }

    //region Util
    @Unique
    private TextColor[][] chatheads$getPlayerHead(final GameProfile profile, final ServerPlayerEntity player) {
        // FIX: getTextures now returns a 'MinecraftProfileTextures' Record, not a Map.
        // We use 'var' to deduce the type automatically.
        var textures = server.getApiServices().sessionService().getTextures(profile);

        // FIX: Access the skin directly from the record using .skin()
        final MinecraftProfileTexture playerSkin = textures.skin();

        //return default head if skin is null
        if (playerSkin == null) return DEFAULT_HEAD_TEXTURE;

        final String playerSkinUrl = playerSkin.getUrl();

        //return default head if skin url is null
        if (playerSkinUrl == null) return DEFAULT_HEAD_TEXTURE;

        //pull the picture
        final BufferedImage image;
        try {
            image = ImageIO.read(URI.create(playerSkinUrl).toURL());
        } catch (Exception e) {
            // FIXED: player.getName().getString()
            LOGGER.warn("Failed to get image for {}", player.getName().getString());
            LOGGER.warn(e.toString());
            return DEFAULT_HEAD_TEXTURE;
        }

        //generate the head
        final TextColor[][] playerHead = new TextColor[8][8];
        for (int x = 8; x < 16; x++) {
            for (int y = 8; y < 16; y++) {
                int rgb = image.getRGB(x, y);
                playerHead[y - 8][x - 8] = fromRgb(rgb & 0xffffff);
            }
        }

        return playerHead;
    }
    //endregion
}