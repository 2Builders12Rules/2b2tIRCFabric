package org_2b12r.irc2b2t.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org_2b12r.irc2b2t.fabric.IRC2b2t;
import org_2b12r.irc2b2t.fabric.command.ModCommandManager;

@Mixin(value = ClientPlayNetworkHandler.class, priority = Integer.MAX_VALUE)
public class MixinClientPlayNetworkHandler {
    @Shadow private CommandDispatcher<CommandSource> commandDispatcher;

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void sendChatMessage(String content, CallbackInfo ci) {
        if (IRC2b2t.onChat(content))
            ci.cancel();
    }

    @Inject(method = "onCommandTree", at = @At("TAIL"))
    public void onCommandTree(CallbackInfo ci) {
        this.commandDispatcher = ModCommandManager.addModCommands(this.commandDispatcher);
    }
}
