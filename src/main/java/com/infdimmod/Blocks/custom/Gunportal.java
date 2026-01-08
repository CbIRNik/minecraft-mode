package com.infdimmod.Blocks.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class Gunportal extends Block implements Waterloggable {
    // Собственное свойство для 2 направлений
    public static final EnumProperty<PortalDirection> PORTAL_DIRECTION =
            EnumProperty.of("portal_direction", PortalDirection.class);
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    // Воксельные формы для двух направлений (2 пикселя = 0.125 блока)
    private static final VoxelShape NORTH_SOUTH_SHAPE =
            Block.createCuboidShape(0.0, 0.0, 7.5, 16.0, 16.0, 8.5); // Толщина 1 пиксель посередине
    private static final VoxelShape EAST_WEST_SHAPE =
            Block.createCuboidShape(7.5, 0.0, 0.0, 8.5, 16.0, 16.0); // Толщина 1 пиксель посередине

    public enum PortalDirection implements StringIdentifiable {
        NORTH_SOUTH("north_south"),
        EAST_WEST("east_west");

        private final String name;

        PortalDirection(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }
    public Gunportal(Settings settings){
        super(settings
                .strength(-1.0F, 3600000.0F) // Неразрушимый
                .dropsNothing() // Не выпадает при разрушении
                .noCollision() // Нет коллизии
                .nonOpaque()); // Непрозрачный
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(PORTAL_DIRECTION, PortalDirection.NORTH_SOUTH)
                .with(WATERLOGGED, false));
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PORTAL_DIRECTION, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        if (player == null) {
            return this.getDefaultState();
        }

        // Определяем направление взгляда игрока
        Direction playerFacing = player.getHorizontalFacing();

        // Преобразуем в одно из двух направлений
        PortalDirection portalDir;
        if (playerFacing == Direction.NORTH || playerFacing == Direction.SOUTH) {
            portalDir = PortalDirection.EAST_WEST; // Игрок смотрит на север/юг - портал восток-запад
        } else {
            portalDir = PortalDirection.NORTH_SOUTH; // Игрок смотрит на восток/запад - портал север-юг
        }

        return this.getDefaultState().with(PORTAL_DIRECTION, portalDir);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        PortalDirection direction = state.get(PORTAL_DIRECTION);

        return switch (direction) {
            case NORTH_SOUTH -> NORTH_SOUTH_SHAPE; // Вертикальная плита север-юг
            case EAST_WEST -> EAST_WEST_SHAPE;     // Вертикальная плита восток-запад
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Возвращаем пустую форму для отсутствия коллизии
        return VoxelShapes.empty();
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    // Отключаем вращение блока - только 2 направления
    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        // При вращении меняем направление на противоположное
        PortalDirection current = state.get(PORTAL_DIRECTION);
        return state.with(PORTAL_DIRECTION,
                current == PortalDirection.NORTH_SOUTH ?
                        PortalDirection.EAST_WEST : PortalDirection.NORTH_SOUTH);
    }
    // Реализация Waterloggable интерфейса
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : Fluids.EMPTY.getDefaultState();
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canFillWithFluid(@Nullable PlayerEntity player, BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
        return !state.get(WATERLOGGED) && fluid == Fluids.WATER;
    }

    @Override
    public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!state.get(WATERLOGGED) && fluidState.getFluid() == Fluids.WATER) {
            if (!world.isClient()) {
                world.setBlockState(pos, state.with(WATERLOGGED, true), Block.NOTIFY_ALL);
                world.scheduleFluidTick(pos, fluidState.getFluid(), fluidState.getFluid().getTickRate(world));
            }
            return true;
        }
        return false;
    }
    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        // При зеркалировании также меняем направление
        return this.rotate(state, BlockRotation.CLOCKWISE_180);
    }

}
