package org_2b12r.irc2b2t.mixin;

import com.mojang.brigadier.suggestion.Suggestions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Suggestions.class)
public interface SuggestionsAccessor {
    @Accessor("EMPTY")
    static Suggestions getEmpty() {
        throw new AssertionError();
    }
}
