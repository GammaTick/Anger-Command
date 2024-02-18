package net.angercommand;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class AngerCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("anger").requires(source -> source.hasPermissionLevel(2))
                .then(literal("add")
                        .then(argument("attacker(s)", EntityArgumentType.entities())
                                .then(argument("target(s)", EntityArgumentType.entities())
                                        .executes(context -> addAnger(context, EntityArgumentType.getEntities(context, "attacker(s)"), EntityArgumentType.getEntities(context, "target(s)"), false, 30))
                                        .then(argument("shuffle_targets", BoolArgumentType.bool())
                                                .executes(context -> addAnger(context, EntityArgumentType.getEntities(context, "attacker(s)"), EntityArgumentType.getEntities(context, "target(s)"), BoolArgumentType.getBool(context, "shuffle_targets"), 30))
                                                .then(argument("anger_time", IntegerArgumentType.integer(0))
                                                        .executes(context -> addAnger(context, EntityArgumentType.getEntities(context, "attacker(s)"), EntityArgumentType.getEntities(context, "target(s)"), BoolArgumentType.getBool(context, "shuffle_targets"), IntegerArgumentType.getInteger(context, "anger_time"))))))))
                .then(literal("clear")
                        .then(argument("attacker(s)", EntityArgumentType.entities())
                                .executes(context -> removeAnger(context, EntityArgumentType.getEntities(context, "attacker(s)")))))));
    }

    public static int addAnger(CommandContext<ServerCommandSource> context, Collection<? extends Entity> attackerEntities, Collection<? extends Entity> targetEntities, boolean shuffleTargets, int angerTime) {
        if (attackerEntities.size() == 1 && !(attackerEntities.iterator().next() instanceof MobEntity)) {
            context.getSource().sendError(Text.of("Unable to set attacker(s)"));
            return 0;
        }

        if (targetEntities.size() == 1 && !(targetEntities.iterator().next() instanceof LivingEntity)) {
            context.getSource().sendError(Text.of("Unable to set target(s)"));
            return 0;
        }

        int angeredEntitiesCount = 0;
        int targetEntitiesCount = 0;
        List<? extends Entity> targetEntitiesList = new ArrayList<>(targetEntities);

        for (Entity attackerEntity : attackerEntities) {
            if (attackerEntity instanceof MobEntity attacker) {
                if (shuffleTargets && attackerEntities.size() > 1) {
                    Collections.shuffle(targetEntitiesList);
                }

                for (Entity targetEntity : targetEntitiesList) {
                    if (targetEntity instanceof LivingEntity target && !attacker.equals(targetEntity)) {
                        attacker.setTarget(target);
                        attacker.getBrain().remember(MemoryModuleType.ANGRY_AT, target.getUuid(), angerTime * 20L);
                        attacker.getBrain().remember(MemoryModuleType.ATTACK_TARGET, target, angerTime * 20L);

                        if (attacker instanceof BeeEntity beeEntity) {
                            beeEntity.setAngryAt(targetEntity.getUuid());
                            beeEntity.setAngerTime(angerTime * 20);
                        } else if (attacker instanceof EndermanEntity endermanEntity) {
                            endermanEntity.setAngryAt(targetEntity.getUuid());
                            endermanEntity.setAngerTime(angerTime * 20);
                        } else if (attacker instanceof IronGolemEntity ironGolemEntity) {
                            ironGolemEntity.setAngryAt(targetEntity.getUuid());
                            ironGolemEntity.setAngerTime(angerTime * 20);
                        } else if (attacker instanceof PolarBearEntity polarBearEntity) {
                            polarBearEntity.setAngryAt(targetEntity.getUuid());
                            polarBearEntity.setAngerTime(angerTime * 20);
                        } else if (attacker instanceof WardenEntity warden) {
                            warden.increaseAngerAt(target, angerTime * 20, false);
                        } else if (attacker instanceof WolfEntity wolfEntity) {
                            wolfEntity.setAngryAt(targetEntity.getUuid());
                            wolfEntity.setAngerTime(angerTime * 20);
                        } else if (attacker instanceof ZombifiedPiglinEntity zombifiedPiglinEntity) {
                            zombifiedPiglinEntity.setAngryAt(targetEntity.getUuid());
                            zombifiedPiglinEntity.setAngerTime(angerTime * 20);
                        }

                        targetEntitiesCount++;
                    }
                }
                angeredEntitiesCount++;
            }
        }

        if (angeredEntitiesCount == 0 || targetEntitiesCount == 0) {
            context.getSource().sendError(Text.of("Unable to set attacker(s) nor target(s)"));
            return 0;
        }

        targetEntitiesCount = targetEntitiesCount / angeredEntitiesCount;

        if (angeredEntitiesCount > 1) {
            int finalAngeredEntitiesCount = angeredEntitiesCount;
            if (targetEntitiesCount > 1) {
                context.getSource().sendFeedback(() -> Text.translatable("Angered %s entities at %s entities", finalAngeredEntitiesCount, targetEntities.size()), true);
            } else {
                context.getSource().sendFeedback(() -> Text.translatable("Angered %s entities at %s", finalAngeredEntitiesCount, targetEntities.iterator().next().getDisplayName()), true);
            }
        } else {
            if (targetEntitiesCount > 1) {
                int finalTargetEntitiesCount = targetEntitiesCount;
                context.getSource().sendFeedback(() -> Text.translatable("Angered %s at %s entities", attackerEntities.iterator().next().getDisplayName(), finalTargetEntitiesCount), true);
            } else {
                context.getSource().sendFeedback(() -> Text.translatable("Angered %s at %s", attackerEntities.iterator().next().getDisplayName(), targetEntities.iterator().next().getDisplayName()), true);
            }
        }
        return 1;
    }

    public static int removeAnger(CommandContext<ServerCommandSource> context, Collection<? extends Entity> attackerEntities) {
        int calmedEntities = 0;
        Optional<LivingEntity> target;

        for (Entity attackerEntity : attackerEntities) {
            if (attackerEntity instanceof MobEntity attacker) {
                attacker.setTarget(null);
                attacker.getBrain().forget(MemoryModuleType.ANGRY_AT);
                attacker.getBrain().forget(MemoryModuleType.ATTACK_TARGET);

                if (attacker instanceof BeeEntity beeEntity) {
                    beeEntity.stopAnger();
                } else if (attacker instanceof EndermanEntity endermanEntity) {
                    endermanEntity.stopAnger();
                } else if (attacker instanceof IronGolemEntity ironGolemEntity) {
                    ironGolemEntity.stopAnger();
                } else if (attacker instanceof PolarBearEntity polarBearEntity) {
                    polarBearEntity.stopAnger();
                } else if (attacker instanceof WardenEntity warden) {
                    target = warden.getPrimeSuspect();
                    while (target.isPresent()) {
                        warden.removeSuspect(target.get());
                        target = warden.getPrimeSuspect();
                    }
                } else if (attacker instanceof WolfEntity wolfEntity) {
                    wolfEntity.stopAnger();
                } else if (attacker instanceof ZombifiedPiglinEntity zombifiedPiglinEntity) {
                    zombifiedPiglinEntity.stopAnger();
                }
                calmedEntities++;
            }
        }

        if (calmedEntities == 0) {
            context.getSource().sendError(Text.of("Unable to remove the target(s) of the attacker(s)"));
            return 0;
        } else if (calmedEntities == 1) {
            context.getSource().sendFeedback(() -> Text.translatable("Successfully removed the target(s) of %s", attackerEntities.iterator().next().getDisplayName()), true);
        } else {
            int finalCalmedEntities = calmedEntities;
            context.getSource().sendFeedback(() -> Text.translatable("Successfully removed the target(s) of %s attackers", finalCalmedEntities), true);
        }

        return 1;
    }
}