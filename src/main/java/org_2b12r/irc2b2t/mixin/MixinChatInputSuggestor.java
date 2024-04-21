package org_2b12r.irc2b2t.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org_2b12r.irc2b2t.fabric.Config;
import org_2b12r.irc2b2t.fabric.command.MergedCommandDispatcher;

@Mixin(ChatInputSuggestor.class)
public abstract class MixinChatInputSuggestor {
    @Shadow @Final TextFieldWidget textField;
    @Shadow @Final private boolean slashOptional;

    /**
     * <p>Allows completions to be suggested while typing a command with IRC prefix.</p>
     * <pre><code>
     *  INVOKESPECIAL com/mojang/brigadier/StringReader.<init> (Ljava/lang/String;)V
     *  ASTORE 2
     *  <strong><em>[INJECTS HERE]</em></strong></code></pre>
     */
    @Inject(method = "refresh", at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/brigadier/StringReader;<init>(Ljava/lang/String;)V",
            remap = false,
            shift = At.Shift.BY,
            by = 2
    ))
    private void skipIRCPrefix(CallbackInfo ci, @Local StringReader stringReader) {
        if (this.slashOptional) // We don't want IRC in command blocks
            return;

        if (stringReader.canRead() && stringReader.peek() == Config.sendToIRCPrefix)
            stringReader.skip();
    }

    /**
     * Prevents a crash when the cursor is in between the IRC prefix and '/'.
     */
    @Inject(method = "refresh", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getCursor()I",
            shift = At.Shift.AFTER
    ))
    private void checkCursor(CallbackInfo ci, @Local LocalBooleanRef isCommand) {
        if (!this.slashOptional && this.textField.getCursor() <= this.textField.getText().indexOf('/'))
            isCommand.set(false);
    }

    /**
     * <p>Prevents IRC commands from being completed or considered valid in command blocks.</p>
     * <pre><code>
     *  INVOKEVIRTUAL net/minecraft/client/network/ClientPlayNetworkHandler.getCommandDispatcher ()Lcom/mojang/brigadier/CommandDispatcher;
     *  ASTORE 6
     *  <strong><em>[INJECTS HERE]</em></strong></pre></code>
     */
    @Inject(method = "refresh", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;getCommandDispatcher()Lcom/mojang/brigadier/CommandDispatcher;",
            shift = At.Shift.BY,
            by = 2
    ))
    private void changeDispatcher(CallbackInfo ci, @Local LocalRef<CommandDispatcher<CommandSource>> dispatcher) {
        if (this.slashOptional && dispatcher.get() instanceof MergedCommandDispatcher mergedDispatcher)
            dispatcher.set(mergedDispatcher.serverDispatcher);
    }
}
