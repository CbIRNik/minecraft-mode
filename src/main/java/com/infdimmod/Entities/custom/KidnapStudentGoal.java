package com.infdimmod.Entities.custom;

import com.infdimmod.Blocks.ModBlocks;
import com.infdimmod.world.collider.DrunnyColliderSystemManager;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class KidnapStudentGoal extends Goal {
    private final DrunGuardEntity mob;
    private StudentEntity targetStudent;

    public KidnapStudentGoal(DrunGuardEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (mob.getFirstPassenger() instanceof StudentEntity) return false;
        
        java.util.List<StudentEntity> list = mob.getWorld().getEntitiesByClass(
                StudentEntity.class, 
                mob.getBoundingBox().expand(16.0), 
                student -> student.isAlive() && !isSafeZone(student.getBlockPos())
        );
        
        targetStudent = null;
        double closestDist = Double.MAX_VALUE;
        for (StudentEntity student : list) {
            double dist = student.squaredDistanceTo(mob);
            if (dist < closestDist) {
                closestDist = dist;
                targetStudent = student;
            }
        }
        
        return targetStudent != null;
    }

    @Override
    public void start() {
        mob.getNavigation().startMovingTo(targetStudent, 1.25);
    }

    @Override
    public void tick() {
        if (targetStudent == null || !targetStudent.isAlive()) return;
        
        mob.getNavigation().startMovingTo(targetStudent, 1.25);
        if (mob.squaredDistanceTo(targetStudent) < 4.0) {
            targetStudent.startRiding(mob, true);
        }
    }

    @Override
    public boolean shouldContinue() {
        return targetStudent != null && targetStudent.isAlive() && !(mob.getFirstPassenger() instanceof StudentEntity);
    }
    
    private boolean isSafeZone(BlockPos pos) {
        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            return com.infdimmod.util.SafeZoneHelper.isInsideDormitory(serverWorld, pos);
        }
        return false;
    }
}
