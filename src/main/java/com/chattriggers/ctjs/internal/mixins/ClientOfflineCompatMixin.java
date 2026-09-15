package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.client.Client;
import com.chattriggers.ctjs.api.client.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Client.class, remap = false)
public abstract class ClientOfflineCompatMixin {
    @Unique
    public static void connect(String ip, int port, String source) {
        Minecraft minecraft = Client.getMinecraft();
        minecraft.execute(() -> GameState.withConnectingSource(source, () ->
            ConnectScreen.startConnecting(
                new JoinMultiplayerScreen(new TitleScreen()), minecraft,
                new ServerAddress(ip, port), new ServerData("Server", ip, ServerData.Type.OTHER), false, null
            )
        ));
    }

    @Unique
    public static void disconnect(String reason, String source) {
        Minecraft minecraft = Client.getMinecraft();
        minecraft.execute(() -> {
            GameState.markActive("script", source);
            minecraft.disconnectFromWorld(Component.literal(reason));
        });
    }
}
