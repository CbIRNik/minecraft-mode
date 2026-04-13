package com.infdimmod.Entities.custom;

import com.infdimmod.util.SafeZoneHelper;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.Optional;

public class StudentStateMachine {
    public enum State {
        WORKING,
        FLEEING,
        SOCIALIZING
    }

    public static void tick(StudentEntity student) {
        if (!(student.getWorld() instanceof ServerWorld serverWorld)) return;

        State currentState = determineState(student, serverWorld);

        switch (currentState) {
            case FLEEING -> handleFleeing(student, serverWorld);
            case WORKING -> handleWorking(student, serverWorld);
            case SOCIALIZING -> handleSocializing(student, serverWorld);
        }
    }

    private static State determineState(StudentEntity student, ServerWorld world) {
        if (!SafeZoneHelper.isInsideDormitory(world, student.getBlockPos())) {
            List<DrunGuardEntity> threats = world.getEntitiesByClass(
                    DrunGuardEntity.class,
                    student.getBoundingBox().expand(24.0),
                    entity -> entity.isAlive()
            );
            if (!threats.isEmpty()) {
                return State.FLEEING;
            }
        }

        if (world.getTimeOfDay() % 24000 > 12000) {
            return State.SOCIALIZING;
        }

        return State.WORKING;
    }

    private static void handleFleeing(StudentEntity student, ServerWorld world) {
        // Clear normal work/play memories to force flee behavior
        student.getBrain().forget(MemoryModuleType.JOB_SITE);
        student.getBrain().forget(MemoryModuleType.MEETING_POINT);

        // Find nearest dormitory block (simple spiral logic or fallback to generic run)
        BlockPos currentPos = student.getBlockPos();
        Optional<BlockPos> safePos = BlockPos.findClosest(currentPos, 32, 32, pos -> SafeZoneHelper.isInsideDormitory(world, pos));
        
        if (safePos.isPresent()) {
            student.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(safePos.get(), 1.6f, 1));
        } else {
            // Run aimlessly if no dorm is found nearby
            student.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(currentPos.add(world.random.nextInt(20) - 10, 0, world.random.nextInt(20) - 10), 1.6f, 1));
        }
    }

    private static void handleWorking(StudentEntity student, ServerWorld world) {
        // Restore normal villager routines
        if (student.age % 40 == 0) {
            student.performPulses(); // Call their regular buff pulses
        }
    }

    private static void handleSocializing(StudentEntity student, ServerWorld world) {
        // Just wander around the safe zone playfully
        if (student.getBrain().getOptionalRegisteredMemory(MemoryModuleType.WALK_TARGET).isEmpty()) {
            BlockPos currentPos = student.getBlockPos();
            student.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(currentPos.add(world.random.nextInt(10) - 5, 0, world.random.nextInt(10) - 5), 0.8f, 1));
        }
    }
}
