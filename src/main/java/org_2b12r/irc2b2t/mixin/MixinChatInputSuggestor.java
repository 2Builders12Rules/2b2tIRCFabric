package org_2b12r.irc2b2t.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.text.OrderedText;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org_2b12r.irc2b2t.fabric.Config;
import org_2b12r.irc2b2t.fabric.IRC2b2t;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class MixinChatInputSuggestor {
    @Shadow private @Nullable CompletableFuture<Suggestions> pendingSuggestions;

    @Inject(method = "refresh", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenRun(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;", remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
    private void refresh(CallbackInfo ci, String string) {
        IRC2b2t.addCompletions(string, pendingSuggestions);
    }

    @Inject(method = "refresh", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;<init>(Ljava/lang/String;)V", remap = false, shift = At.Shift.BY, by = 2), locals = LocalCapture.CAPTURE_FAILHARD)
    private void skipIRCPrefix(CallbackInfo ci, String string, StringReader stringReader) {
        if (stringReader.canRead() && stringReader.peek() == Config.sendToIRCPrefix)
            stringReader.skip();
    }

    @Inject(method = "provideRenderText", at = @At(value = "HEAD"), cancellable = true)
    private void provideRenderText(String original, int firstCharacterIndex, CallbackInfoReturnable<OrderedText> cir) {
        OrderedText text = IRC2b2t.highlightText(original, firstCharacterIndex);
        if (text != null)
            cir.setReturnValue(text);
    }
}
