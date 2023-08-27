package org_2b12r.irc2b2t.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org_2b12r.irc2b2t.fabric.IRC2b2t;

@Mixin(value = ClientPlayNetworkHandler.class, priority = Integer.MAX_VALUE)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void sendChatMessage(String content, CallbackInfo ci) {
        if (IRC2b2t.onChat(content))
            ci.cancel();
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    public void sendChatCommand(String command, CallbackInfo ci) {
        if (IRC2b2t.onChat("/" + command))
            ci.cancel();
    }
}
