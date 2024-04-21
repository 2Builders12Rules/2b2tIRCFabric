package org_2b12r.irc2b2t.mixin;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org_2b12r.irc2b2t.fabric.Clearable;

import java.util.Map;

@Mixin(CommandNode.class)
public abstract class CommandNodeMixin<S> implements Clearable {
    @Shadow @Final private Map<String, CommandNode<S>> children;
    @Shadow @Final private Map<String, LiteralCommandNode<S>> literals;
    @Shadow @Final private Map<String, ArgumentCommandNode<S, ?>> arguments;

    @Intrinsic
    @Override
    public void clear() {
        this.children.clear();
        this.literals.clear();
        this.arguments.clear();
    }
}
