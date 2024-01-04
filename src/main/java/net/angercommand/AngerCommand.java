package net.angercommand;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class AngerCommand {
    private static Collection<? extends Entity> global_targetEntities = null;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("anger").requires(source -> source.hasPermissionLevel(2))
                .then(literal("set")
                        .then(argument("attacker(s)", EntityArgumentType.entities())
                                .then(argument("target(s)", EntityArgumentType.entities())
                                        .executes(context -> setAnger(context, EntityArgumentType.getEntities(context, "attacker(s)"), EntityArgumentType.getEntities(context, "target(s)"), false))
                                        .then(argument("shuffle_targets", BoolArgumentType.bool())
                                                .executes(context -> setAnger(context, EntityArgumentType.getEntities(context, "attacker(s)"), EntityArgumentType.getEntities(context, "target(s)"), BoolArgumentType.getBool(context, "shuffle_targets")))))))
                .then(literal("clear")
                        .then(argument("attacker(s)", EntityArgumentType.entities())
                                .executes(ctx -> removeAnger(ctx, EntityArgumentType.getEntities(ctx, "attacker(s)")))))));
    }

    public static int setAnger(CommandContext<ServerCommandSource> ctx, Collection<? extends Entity> attackerEntities, Collection<? extends Entity> targetEntities, boolean shuffleTargets) {
        if (attackerEntities.size() == 1 && !(attackerEntities.iterator().next() instanceof MobEntity)) {
            ctx.getSource().sendError(Text.of("Unable to set attacker(s)"));
            return 0;
        }

        if (targetEntities.size() == 1 && !(targetEntities.iterator().next() instanceof LivingEntity)) {
            ctx.getSource().sendError(Text.of("Unable to set target(s)"));
            return 0;
        }

        int angeredEntities = 0;
        global_targetEntities = targetEntities;

        List<? extends Entity> targetEntitiesList = new ArrayList<>(targetEntities);

        for (Entity attackerEntity : attackerEntities) {
            if (shuffleTargets && attackerEntities.size() > 1) {
                Collections.shuffle(targetEntitiesList);
            }

            if (attackerEntity instanceof MobEntity attacker && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(attacker)) {
                for (Entity targetEntity : targetEntitiesList) {
                    if (targetEntity instanceof LivingEntity target && !attacker.equals(targetEntity) && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(target)) {
                        attacker.setTarget(target);
                        attacker.getBrain().remember(MemoryModuleType.ANGRY_AT, target.getUuid(), 600L);
                        attacker.getBrain().remember(MemoryModuleType.ATTACK_TARGET, target, 600L);

                        if (attacker instanceof WardenEntity warden) {
                            warden.increaseAngerAt(target, 150, false);
                            warden.increaseAngerAt(target);
                        }
                    }
                }
                angeredEntities++;
            }
        }

        if (angeredEntities == 0) {
            ctx.getSource().sendError(Text.of("Unable to set attacker(s) nor target(s)"));
            return 0;
        }

        if (angeredEntities > 1) {
            if (targetEntities.size() > 1) {
                ctx.getSource().sendFeedback(() -> Text.translatable("Angered %s entities at %s entities", attackerEntities.size(), targetEntities.size()), true);
            } else {
                ctx.getSource().sendFeedback(() -> Text.translatable("Angered %s entities at %s", attackerEntities.size(), targetEntities.iterator().next().getDisplayName()), true);
            }
        } else {
            if (targetEntities.size() > 1) {
                ctx.getSource().sendFeedback(() -> Text.translatable("Angered %s at %s entities", attackerEntities.iterator().next().getDisplayName(), targetEntities.size()), true);
            } else {
                ctx.getSource().sendFeedback(() -> Text.translatable("Angered %s at %s", attackerEntities.iterator().next().getDisplayName(), targetEntities.iterator().next().getDisplayName()), true);
            }
        }

        return 1;
    }

    public static int removeAnger(CommandContext<ServerCommandSource> ctx, Collection<? extends Entity> attackerEntities) {
        if (global_targetEntities == null) {
            if (attackerEntities.size() == 1) {
                ctx.getSource().sendError(Text.of("Unable to remove the anger of the attacker because it didn't have target(s)"));
            } else {
                ctx.getSource().sendError(Text.of("Unable to remove the anger of the attackers because they didn't have target(s)"));
            }
            return 0;
        }

        int calmedEntities = 0;

        for (Entity attackerEntity : attackerEntities) {
            if (attackerEntity instanceof MobEntity attacker && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(attacker)) {
                attacker.setTarget(null);
                attacker.getBrain().forget(MemoryModuleType.ANGRY_AT);
                attacker.getBrain().forget(MemoryModuleType.ATTACK_TARGET);

                if (attacker instanceof WardenEntity warden) {
                    for (Entity targetEntity : global_targetEntities) {
                        warden.removeSuspect(targetEntity);
                    }
                }
                calmedEntities++;
            }
        }

        if (calmedEntities == 0) {
            ctx.getSource().sendError(Text.of("Unable to remove the target(s) of the attacker(s)"));
            return 0;
        } else if (calmedEntities == 1) {
            ctx.getSource().sendFeedback(() -> Text.translatable("Successfully removed the target(s) of %s", attackerEntities.iterator().next().getDisplayName()), true);
        } else {
            int finalCalmedEntities = calmedEntities;
            ctx.getSource().sendFeedback(() -> Text.translatable("Successfully removed the target(s) of %s attackers", finalCalmedEntities), true);
        }

        return 1;
    }
}
