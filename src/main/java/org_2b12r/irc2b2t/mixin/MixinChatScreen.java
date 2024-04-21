package org_2b12r.irc2b2t.mixin;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org_2b12r.irc2b2t.fabric.Config;
import org_2b12r.irc2b2t.fabric.command.ModCommandManager;

@Mixin(ChatScreen.class)
public class MixinChatScreen {
    /**
     * Handles client and IRC commands while letting server commands pass through
     */
    @Inject(method = "sendMessage", at = @At(
            value = "INVOKE",
            target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z",
            ordinal = 0
    ), cancellable = true)
    private void sendMessage(String message, boolean addToHistory, CallbackInfoReturnable<Boolean> ci) {
        StringReader command = new StringReader(message);
        if (command.canRead() && command.peek() == Config.sendToIRCPrefix)
            command.skip();

        if (command.canRead() && command.peek() == '/') {
            command.skip();
            if (ModCommandManager.handleCommand(command))
                ci.setReturnValue(true);
        }
    }
}
