package com.campersamu.chatheads;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.polymer.autohost.impl.AutoHost;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;
import java.util.Optional;

import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.network.chat.TextColor.fromRgb;

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
    public static final Component DEFAULT_HEAD = paintHead(DEFAULT_HEAD_TEXTURE);
    public static final HashMap<UUID, TextColor[][]> HEAD_CACHE = new HashMap<>();
    //endregion

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void onInitializeServer() {
        //Add Mod Resources to Polymer Resource Pack
        PolymerResourcePackUtils.addModAssets(MODID);

        //Register Placeholder
        Placeholders.registerServer(Identifier.fromNamespaceAndPath(MODID, PLAYER), (ctx, arg) -> {
            if (ctx.gameProfile() == null) return PlaceholderResult.value(DEFAULT_HEAD);

            Object cache = getCacheRobust(ctx.server());
            if (cache == null) return PlaceholderResult.value(DEFAULT_HEAD);

            if (arg == null || arg.isEmpty())
                return PlaceholderResult.value(paintHead(HEAD_CACHE.getOrDefault(ctx.gameProfile().id(), DEFAULT_HEAD_TEXTURE)));

            // FIX: 'findByName' returns Optional<GameProfile>. We must unwrap it using .map()
            return getProfileByName(cache, arg)
                    .map(profile -> PlaceholderResult.value(paintHead(HEAD_CACHE.getOrDefault(getProfileId(profile), DEFAULT_HEAD_TEXTURE))))
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

    @Nullable
    private static Object getCacheRobust(MinecraftServer server) {
        try {
            Object services = getServices(server);
            for (Method m : services.getClass().getMethods()) {
                if (m.getReturnType().getSimpleName().equals("UserCache")
                        || m.getReturnType().getSimpleName().equals("UserNameToIdResolver")) {
                    return m.invoke(services);
                }
            }
            for (String methodName : new String[]{"nameToIdCache", "userCache", "getUserCache"}) {
                try {
                    Method m = services.getClass().getMethod(methodName);
                    return m.invoke(services);
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to find player name resolver via reflection", e);
        }
        return null;
    }

    @NotNull
    private static Object getServices(MinecraftServer server) throws Exception {
        for (String methodName : new String[]{"services", "getApiServices"}) {
            try {
                Method m = server.getClass().getMethod(methodName);
                return m.invoke(server);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException("No services accessor found");
    }

    @SuppressWarnings("unchecked")
    private static Optional<Object> getProfileByName(Object cache, String name) {
        try {
            for (String methodName : new String[]{"get", "findByName"}) {
                try {
                    Method m = cache.getClass().getMethod(methodName, String.class);
                    Object result = m.invoke(cache, name);
                    if (result instanceof Optional<?> optional) {
                        return (Optional<Object>) optional;
                    }
                    if (result != null) {
                        return Optional.of(result);
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to resolve player profile by name", e);
        }
        return Optional.empty();
    }

    private static UUID getProfileId(Object profile) {
        try {
            Method m = profile.getClass().getMethod("id");
            return (UUID) m.invoke(profile);
        } catch (Exception e) {
            return null;
        }
    }

    //region Util
    public static @NotNull Component paintHead(TextColor[][] head) {
        MutableComponent text = Component.empty();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                text = text
                        .append(literal("" + (char) (((int) '\uF810') + y)).setStyle(Style.EMPTY.withColor(head[y][x])/*.withFont(Identifier.fromNamespaceAndPath(MODID, "pixel"))*/))
                        .append(literal("\uE001").setStyle(Style.EMPTY/*.withFont(Identifier.of(MODID, "pixel"))*/));
            }
            text = text.append(literal("\uE008").setStyle(Style.EMPTY/*.withFont(Identifier.of(MODID, "pixel"))*/));
        }

        text.append(literal("  "));

        return text;
    }
    //endregion
}
