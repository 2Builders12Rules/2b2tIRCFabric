package org_2b12r.irc2b2t.mixin;

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
import org_2b12r.irc2b2t.fabric.IRC2b2t;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public class MixinChatInputSuggestor {
    @Shadow private @Nullable CompletableFuture<Suggestions> pendingSuggestions;

    @Inject(method = "refresh", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void refresh(CallbackInfo ci, String string) {
        IRC2b2t.addCompletions(string, pendingSuggestions);
    }

    @Inject(method = "provideRenderText", at = @At(value = "HEAD"), cancellable = true)
    private void provideRenderText(String original, int firstCharacterIndex, CallbackInfoReturnable<OrderedText> cir) {
        OrderedText text = IRC2b2t.highlightText(original, firstCharacterIndex);
        if (text != null)
            cir.setReturnValue(text);
    }
}
