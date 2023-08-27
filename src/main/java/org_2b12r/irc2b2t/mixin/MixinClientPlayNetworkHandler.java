package org_2b12r.irc2b2t.mixin;


import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org_2b12r.irc2b2t.fabric.IRC2b2t;

@Mixin(value = ClientPlayNetworkHandler.class, priority = 1001)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo cir) {
        if (IRC2b2t.onChat(message))
            cir.cancel();
    }
    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void onSendChatCommand(String command, CallbackInfo cir) {
        if (IRC2b2t.onCommand(command))
            cir.cancel();
    }
}
