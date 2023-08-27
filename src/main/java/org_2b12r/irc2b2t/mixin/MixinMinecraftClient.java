package org_2b12r.irc2b2t.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org_2b12r.irc2b2t.fabric.IRC2b2t;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Inject(at = @At("HEAD"), method = "tick()V")
    public void tick(CallbackInfo callbackInfo) {
        IRC2b2t.onTick();
    }

    @Inject(at = @At("HEAD"), method = "setWorld")
    public void onWorldChange(CallbackInfo callbackInfo) {
        IRC2b2t.sendChatState();
    }
}