package com.campersamu.chatheads;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.polymer.autohost.impl.AutoHost;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

import static net.minecraft.text.Text.literal;
import static net.minecraft.text.TextColor.fromRgb;

public class ChatHeadsInit implements DedicatedServerModInitializer {
    //region Constants
    public static final String MODID = "chatheads", PLAYER = "player";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public static final TextColor[][] DEFAULT_HEAD_TEXTURE = new TextColor[][]{   //hex 0xC01044 -> TextColor.fromRgb(0xC01044)
            {fromRgb(0x191919), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x191919)},
            {fromRgb(0x191919), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x191919)},
            {fromRgb(0x191919), fromRgb(0x191919), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x0c0c0c), fromRgb(0x191919), fromRgb(0x191919)},
            {fromRgb(0x191919), fromRgb(0x191919), fromRgb(0x191919), fromRgb(0x191919), fromRgb(0x191919), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0x191919)},

            {fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0)},
            {fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0)},

            {fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0)},
            {fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0), fromRgb(0xffd7b0)},
    };
    public static final Text DEFAULT_HEAD = paintHead(DEFAULT_HEAD_TEXTURE);
    public static final HashMap<UUID, TextColor[][]> HEAD_CACHE = new HashMap<>();
    //endregion

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void onInitializeServer() {
        //Add Mod Resources to Polymer Resource Pack
        PolymerResourcePackUtils.addModAssets(MODID);

        //Register Placeholder
        Placeholders.register(Identifier.of(MODID, PLAYER), (ctx, arg) -> {
            if (ctx.gameProfile() == null) return PlaceholderResult.value(DEFAULT_HEAD);

            UserCache cache = getCacheRobust(ctx.server());
            if (cache == null) return PlaceholderResult.value(DEFAULT_HEAD);

            if (arg == null || arg.isEmpty())
                return PlaceholderResult.value(paintHead(HEAD_CACHE.getOrDefault(ctx.gameProfile().id(), DEFAULT_HEAD_TEXTURE)));

            // FIX: 'findByName' returns Optional<GameProfile>. We must unwrap it using .map()
            var playerProfileOpt = cache.findByName(arg);

            return playerProfileOpt
                    .map(profile -> PlaceholderResult.value(paintHead(HEAD_CACHE.getOrDefault(profile.id(), DEFAULT_HEAD_TEXTURE))))
                    .orElseGet(() -> PlaceholderResult.value(DEFAULT_HEAD));
        });

        if (!AutoHost.config.enabled && !FabricLoader.getInstance().isModLoaded("arte")) {
            LOGGER.warn("""
              #####################################
                Polymer AutoHost is not enabled!
              The heads in chat might appear buggy!
               Go to config/polymer/autohost.json
                          to enable it!
              #####################################
              """);
        }
    }

    // Helper to find the UserCache regardless of its mapping name
    @Nullable
    private static UserCache getCacheRobust(MinecraftServer server) {
        try {
            Object services = server.getApiServices();
            // Look for any method returning UserCache
            for (Method m : services.getClass().getMethods()) {
                if (m.getReturnType().getSimpleName().equals("UserCache")) {
                    return (UserCache) m.invoke(services);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to find UserCache via reflection", e);
        }
        return null;
    }

    //region Util
    public static @NotNull Text paintHead(TextColor[][] head) {
        MutableText text = Text.empty();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                text = text
                        .append(literal("" + (char) (((int) '\uF810') + y)).setStyle(Style.EMPTY.withColor(head[y][x])/*.withFont(Identifier.of(MODID, "pixel"))*/))
                        .append(literal("\uE001").fillStyle(Style.EMPTY/*.withFont(Identifier.of(MODID, "pixel"))*/));
            }
            text = text.append(literal("\uE008").fillStyle(Style.EMPTY/*.withFont(Identifier.of(MODID, "pixel"))*/));
        }

        text.append(literal("  "));

        return text;
    }
    //endregion
}