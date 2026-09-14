package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.client.GameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {
    @Inject(method = "startConnecting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;disconnectWithProgressScreen(Z)V"))
    private static void connecting(Screen parent, Minecraft mc, ServerAddress address, ServerData data,
                                   boolean quickPlay, TransferState transfer, CallbackInfo ci) {
        GameState.connecting(data, transfer != null);
    }
}
