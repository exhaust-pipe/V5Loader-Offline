package com.chattriggers.ctjs.internal.mixins;

import net.minecraft.client.gui.screens.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConnectScreen.class)
public interface ConnectScreenAccessor {
    @Accessor("aborted")
    boolean isAborted();
}
