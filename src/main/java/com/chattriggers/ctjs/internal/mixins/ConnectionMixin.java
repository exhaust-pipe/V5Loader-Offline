package com.chattriggers.ctjs.internal.mixins;

import com.chattriggers.ctjs.internal.engine.CTEvents;
import com.chattriggers.ctjs.api.triggers.TriggerType;
import com.chattriggers.ctjs.api.client.GameState;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow
    public abstract PacketFlow getReceiving();

    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void injectHandlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (getReceiving() == PacketFlow.CLIENTBOUND)
            CTEvents.PACKET_RECEIVED.invoker().receive(packet, ci);
        if (ci.isCancelled()) return;
        if (packet instanceof ClientboundDisconnectPacket || packet instanceof ClientboundLoginDisconnectPacket)
            GameState.mark((Connection) (Object) this, "unexpected", "server");
        else if (packet instanceof ClientboundTransferPacket)
            GameState.mark((Connection) (Object) this, "transfer", "server");
    }

    @Inject(method = "initiateServerboundPlayConnection", at = @At("HEAD"))
    private void bindPlayConnection(CallbackInfo ci) {
        GameState.bind((Connection) (Object) this);
    }

    @Inject(method = "channelInactive", at = @At("HEAD"))
    private void networkClosed(ChannelHandlerContext context, CallbackInfo ci) {
        GameState.mark((Connection) (Object) this, "unexpected", "network");
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    private void networkFailed(ChannelHandlerContext context, Throwable error, CallbackInfo ci) {
        GameState.mark((Connection) (Object) this, "unexpected", "network");
    }

    @Inject(method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At("HEAD"))
    private void localClosed(DisconnectionDetails details, CallbackInfo ci) {
        GameState.markLocal((Connection) (Object) this);
    }

    @Inject(method = "handleDisconnection", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketListener;onDisconnect(Lnet/minecraft/network/DisconnectionDetails;)V"))
    private void notifyClosed(CallbackInfo ci) {
        Connection connection = (Connection) (Object) this;
        GameState.connectionClosed(connection, connection.getDisconnectionDetails());
    }

    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void injectSendPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        TriggerType.PACKET_SENT.triggerAll(packet, ci);
    }
}
