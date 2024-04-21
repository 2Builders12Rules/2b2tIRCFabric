package org_2b12r.irc2b2t.fabric.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;

import java.util.function.Predicate;

public class ModCommand extends LiteralCommandNode<CommandSource> {
    public ModCommand(String literal, Command<CommandSource> command, Predicate<CommandSource> requirement, CommandNode<CommandSource> redirect, RedirectModifier<CommandSource> modifier, boolean forks) {
        super(literal, command, requirement, redirect, modifier, forks);
    }
}
