package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.api.client.Client;
import com.chattriggers.ctjs.api.client.GameState;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.chat.Component;
import com.chattriggers.ctjs.api.world.Scoreboard;
import com.chattriggers.ctjs.api.world.TabList;
import com.chattriggers.ctjs.internal.engine.CTEvents;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.internal.engine.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Nullable public ClientLevel level;
    @Shadow @Nullable public Screen screen;

    @Shadow public abstract ServerData getCurrentServer();
    @Shadow public abstract boolean hasSingleplayerServer();

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void injectWorldUnload(ClientLevel world, CallbackInfo ci) {
        if (this.level != null && this.level != world) GameState.worldChanging();
        if (world == null)
            Client.unpressKeys();

        if (this.level == null && world != null) {
            TriggerType.SERVER_CONNECT.triggerAll();
        } else if (this.level != null && world == null) {
            TriggerType.SERVER_DISCONNECT.triggerAll();
        }

        if (this.level != null) {
            TriggerType.WORLD_UNLOAD.triggerAll();
            Scoreboard.INSTANCE.clearCustom$ctjs();
            TabList.INSTANCE.clearCustom$ctjs();
        }
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void injectWorldLoad(ClientLevel world, CallbackInfo ci) {
        if (world != null) {
            GameState.worldLoaded();
            TriggerType.WORLD_LOAD.triggerAll();
        }
    }

    @Inject(method = "disconnectFromWorld", at = @At("HEAD"))
    private void injectManualDisconnect(Component reason, CallbackInfo ci) {
        boolean script = GameState.isScriptCall();
        GameState.markActive(script ? "script" : "manual", script ? "unattributed" : "user");
    }

    @Inject(method = "clearClientLevel", at = @At("HEAD"))
    private void injectReconfiguration(Screen screen, CallbackInfo ci) {
        if (this.level == null) return;
        GameState.worldChanging();
        Client.unpressKeys();
        TriggerType.WORLD_UNLOAD.triggerAll();
        Scoreboard.INSTANCE.clearCustom$ctjs();
        TabList.INSTANCE.clearCustom$ctjs();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void injectDisconnect(Screen disconnectionScreen, boolean transferring, boolean clearChat, CallbackInfo ci) {
        GameState.disconnecting(transferring);
        // disconnect() is also called when connecting, so we check that there is
        // an existing server
        if (this.level != null) {
            Client.unpressKeys();
            TriggerType.WORLD_UNLOAD.triggerAll();
            TriggerType.SERVER_DISCONNECT.triggerAll();
            Scoreboard.INSTANCE.clearCustom$ctjs();
            TabList.INSTANCE.clearCustom$ctjs();
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void injectScreenOpened(Screen screen, CallbackInfo ci) {
        if (screen instanceof DisconnectedScreen)
            GameState.connectionFailed(((DisconnectedScreenAccessor) screen).getDetails().reason());
        if (this.screen instanceof ConnectScreen && ((ConnectScreenAccessor) this.screen).isAborted())
            GameState.cancelConnecting();
        if (screen != null) {
            Client.automatedAttackHeld = false;
            TriggerType.GUI_OPENED.triggerAll(screen, ci);
        }
    }

    @Inject(method = "setOverlay", at = @At("HEAD"))
    private void injectOverlayOpened(Overlay overlay, CallbackInfo ci) {
        if (overlay != null)
            Client.unpressKeys();
    }

    @Inject(method = "run", at = @At("HEAD"))
    private void injectRun(CallbackInfo ci) {
        new Thread(() -> {
            ModuleManager.INSTANCE.entryPass();
            TriggerType.GAME_LOAD.triggerAll();
        }).start();
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void injectRender(boolean tick, CallbackInfo ci) {
        CTEvents.RENDER_GAME.invoker().run();
    }
}
