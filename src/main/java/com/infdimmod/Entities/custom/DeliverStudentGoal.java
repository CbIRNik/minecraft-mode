package com.infdimmod.Entities.custom;

import com.infdimmod.world.collider.DrunnyColliderSystemManager;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class DeliverStudentGoal extends Goal {
    private final DrunGuardEntity mob;
    private BlockPos targetCore;

    public DeliverStudentGoal(DrunGuardEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return mob.getFirstPassenger() instanceof StudentEntity;
    }

    @Override
    public void start() {
        targetCore = mob.getGuardPost(); // Defaulting to guard post since it assumes they guard a core
        if (targetCore == null) {
            targetCore = mob.getBlockPos(); // fallback
        }
        mob.getNavigation().startMovingTo(targetCore.getX(), targetCore.getY(), targetCore.getZ(), 1.0);
    }

    @Override
    public void tick() {
        if (targetCore == null) return;
        mob.getNavigation().startMovingTo(targetCore.getX(), targetCore.getY(), targetCore.getZ(), 1.0);
        if (mob.squaredDistanceTo(targetCore.toCenterPos()) < 16.0) {
            if (mob.getFirstPassenger() instanceof StudentEntity student) {
                student.stopRiding();
                student.discard(); // 'Consumed' by collider to cool it down
                if (mob.getWorld() instanceof ServerWorld serverWorld) {
                    DrunnyColliderSystemManager.absorbStudent(serverWorld, targetCore);
                }
            }
        }
    }
}
