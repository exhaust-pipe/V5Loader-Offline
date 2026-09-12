package com.v5.mixins;

import com.v5.integrations.SkydiaoIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "pers.XiaoShadiao.skydiao.irc.ClientReceiveHandler", remap = false)
public class SkydiaoClientReceiveHandlerMixin {
    @Inject(
        method = "handle(Lpers/XiaoShadiao/skydiao/irc/ChatPacket;Lpers/XiaoShadiao/skydiao/irc/ClientListener;)V",
        at = @At("HEAD"),
        require = 0,
        remap = false
    )
    private void v5$receiveSystemMessage(@Coerce Object packet, @Coerce Object sender, CallbackInfo ci) {
        SkydiaoIntegration.onPacket(packet);
    }
}
