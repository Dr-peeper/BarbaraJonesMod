package com.barbarajones.v2.airline.structure;

import com.barbarajones.content.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class AirportGenerator {

    /**
     * Generate a basic airport structure starting from the airport core block
     */
    public static void generateAirport(Level level, BlockPos corePos) {
        // Prevent regeneration if already exists
        if (level.getBlockState(corePos.offset(1, 0, 0)).getBlock() != Blocks.AIR) {
            return;
        }

        // Generate runway (100 blocks long, 15 blocks wide)
        generateRunway(level, corePos.offset(0, -2, -30));

        // Generate terminal building (simple structure)
        generateTerminal(level, corePos.offset(40, 0, -10));

        // Generate gates (4 gates)
        for (int i = 0; i < 4; i++) {
            generateGate(level, corePos.offset(50, 0, -10 + (i * 8)));
        }

        // Generate control tower
        generateControlTower(level, corePos.offset(70, 0, 0));

        // Generate parking areas
        generateParkingArea(level, corePos.offset(-50, 0, -20));
    }

    private static void generateRunway(Level level, BlockPos pos) {
        // Runway: 100 blocks long, 15 blocks wide
        for (int x = 0; x < 100; x++) {
            for (int z = 0; z < 15; z++) {
                BlockPos runwayPos = pos.offset(x, 0, z);
                level.setBlock(runwayPos, ModBlocks.RUNWAY.get().defaultBlockState(), 3);

                // Add painted line down the middle
                if (z == 7 && x % 5 == 0) {
                    level.setBlock(runwayPos.above(), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void generateTerminal(Level level, BlockPos pos) {
        // Terminal building: 30x10x8 blocks
        for (int x = 0; x < 30; x++) {
            for (int y = 0; y < 8; y++) {
                for (int z = 0; z < 10; z++) {
                    BlockPos blockPos = pos.offset(x, y, z);

                    if (y == 0) {
                        // Foundation
                        level.setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
                    } else if (y == 7) {
                        // Roof
                        level.setBlock(blockPos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 3);
                    } else if ((x == 0 || x == 29 || z == 0 || z == 9) && (y < 7)) {
                        // Walls
                        if (Math.random() > 0.2) {
                            level.setBlock(blockPos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 3);
                        } else {
                            // Windows
                            level.setBlock(blockPos, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 3);
                        }
                    } else if (y > 1) {
                        // Interior empty
                        level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        // Floor
                        level.setBlock(blockPos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 3);
                    }
                }
            }
        }

        // Add security gates
        for (int i = 0; i < 3; i++) {
            BlockPos securityPos = pos.offset(5 + (i * 8), 1, 0);
            level.setBlock(securityPos, ModBlocks.SECURITY_CHECK.get().defaultBlockState(), 3);
        }

        // Add baggage claim
        for (int i = 0; i < 2; i++) {
            BlockPos baggagePos = pos.offset(10 + (i * 10), 1, 9);
            level.setBlock(baggagePos, ModBlocks.BAGGAGE_CLAIM.get().defaultBlockState(), 3);
        }
    }

    private static void generateGate(Level level, BlockPos pos) {
        // Gate structure: 8x8x5 blocks
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 8; z++) {
                    BlockPos blockPos = pos.offset(x, y, z);

                    if (y == 0) {
                        // Floor
                        level.setBlock(blockPos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 3);
                    } else if (y == 4) {
                        // Ceiling
                        level.setBlock(blockPos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 3);
                    } else if (z == 0 && (x == 0 || x == 7)) {
                        // Support pillars
                        level.setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
                    } else if (z == 7 && (x == 0 || x == 7)) {
                        // Support pillars
                        level.setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
                    } else if (x == 0 || x == 7) {
                        if (Math.random() > 0.3) {
                            level.setBlock(blockPos, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 3);
                        }
                    } else {
                        // Interior
                        level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        // Add gate marker
        level.setBlock(pos.offset(4, 1, 0), ModBlocks.GATE.get().defaultBlockState(), 3);
    }

    private static void generateControlTower(Level level, BlockPos pos) {
        // Control tower: 6x6x20 blocks tall
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 20; y++) {
                for (int z = 0; z < 6; z++) {
                    BlockPos blockPos = pos.offset(x, y, z);

                    if ((x == 0 || x == 5 || z == 0 || z == 5)) {
                        // Walls
                        if (y < 18) {
                            level.setBlock(blockPos, Blocks.GRAY_CONCRETE.defaultBlockState(), 3);
                        } else {
                            // Top observation deck
                            level.setBlock(blockPos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 3);
                        }
                    } else if (y == 0) {
                        // Foundation
                        level.setBlock(blockPos, Blocks.STONE.defaultBlockState(), 3);
                    } else {
                        // Interior
                        level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void generateParkingArea(Level level, BlockPos pos) {
        // Parking area: 40x30 blocks for aircraft parking
        for (int x = 0; x < 40; x++) {
            for (int z = 0; z < 30; z++) {
                BlockPos parkingPos = pos.offset(x, 0, z);
                level.setBlock(parkingPos, Blocks.GRAY_CONCRETE.defaultBlockState(), 3);

                // Add parking lines every 10 blocks
                if ((x == 0 || x == 39) || (x % 20 == 10 && z < 29)) {
                    level.setBlock(parkingPos.above(), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);
                }
            }
        }
    }
}
