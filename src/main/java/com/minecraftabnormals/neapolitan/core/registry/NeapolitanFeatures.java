package com.minecraftabnormals.neapolitan.core.registry;

import com.minecraftabnormals.neapolitan.common.block.StrawberryBushBlock;
import com.minecraftabnormals.neapolitan.common.world.gen.feature.*;
import com.minecraftabnormals.neapolitan.core.Neapolitan;
import com.minecraftabnormals.neapolitan.core.NeapolitanConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.blockplacer.SimpleBlockPlacer;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placement.AtSurfaceWithExtraConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class NeapolitanFeatures {
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, Neapolitan.MOD_ID);
	public static final RegistryObject<Feature<BlockClusterFeatureConfig>> STRAWBERRY_PATCH = FEATURES.register("strawberry_patch", () -> new StrawberryPatchFeature(BlockClusterFeatureConfig.field_236587_a_));
	public static final RegistryObject<Feature<BlockClusterFeatureConfig>> VANILLA_VINE_PATCH = FEATURES.register("vanilla_patch", () -> new VanillaPatchFeature(BlockClusterFeatureConfig.field_236587_a_));
	public static final RegistryObject<Feature<NoFeatureConfig>> BANANA_PLANT = FEATURES.register("banana_plant", () -> new BananaPlantFeature(NoFeatureConfig.field_236558_a_));
	public static final RegistryObject<Feature<BlockClusterFeatureConfig>> ADZUKI_PATCH = FEATURES.register("adzuki_patch", () -> new AdzukiPatchFeature(BlockClusterFeatureConfig.field_236587_a_));
	public static final RegistryObject<Feature<NoFeatureConfig>> MINT_POND = FEATURES.register("mind_pond", () -> new MintPondFeature(NoFeatureConfig.field_236558_a_));

	public static final class Configs {
		public static final BlockClusterFeatureConfig STRAWBERRY_PATCH_CONFIG = (new BlockClusterFeatureConfig.Builder(new SimpleBlockStateProvider(NeapolitanBlocks.STRAWBERRY_BUSH.get().getDefaultState().with(StrawberryBushBlock.TYPE, StrawberryBushBlock.StrawberryType.RED)), SimpleBlockPlacer.PLACER)).replaceable().xSpread(5).zSpread(5).func_227317_b_().tries(512).build();
		public static final BlockClusterFeatureConfig VANILLA_VINE_PATCH_CONFIG = (new BlockClusterFeatureConfig.Builder(new SimpleBlockStateProvider(NeapolitanBlocks.VANILLA_VINE.get().getDefaultState()), SimpleBlockPlacer.PLACER)).func_227317_b_().tries(64).build();
		public static final BlockClusterFeatureConfig ADZUKI_PATCH_CONFIG = (new BlockClusterFeatureConfig.Builder(new SimpleBlockStateProvider(NeapolitanBlocks.ADZUKI_SPROUTS.get().getDefaultState()), SimpleBlockPlacer.PLACER)).replaceable().xSpread(3).ySpread(2).zSpread(3).func_227317_b_().tries(256).build();
	}

	public static final class Configured {
		public static final ConfiguredFeature<?, ?> PATCH_STRAWBERRY_BUSH = NeapolitanFeatures.STRAWBERRY_PATCH.get().withConfiguration(Configs.STRAWBERRY_PATCH_CONFIG).withPlacement(Features.Placements.PATCH_PLACEMENT).chance(NeapolitanConfig.COMMON.strawberryBushGenerationChance.get());
		public static final ConfiguredFeature<?, ?> PATCH_VANILLA_VINE = NeapolitanFeatures.VANILLA_VINE_PATCH.get().withConfiguration(Configs.VANILLA_VINE_PATCH_CONFIG).withPlacement(Features.Placements.PATCH_PLACEMENT).chance(NeapolitanConfig.COMMON.vanillaVineGenerationChance.get());
		public static final ConfiguredFeature<?, ?> PATCH_ADZUKI_SPROUTS = NeapolitanFeatures.ADZUKI_PATCH.get().withConfiguration(Configs.ADZUKI_PATCH_CONFIG).withPlacement(Features.Placements.PATCH_PLACEMENT).chance(NeapolitanConfig.COMMON.adzukiSproutsGenerationChance.get());
		public static final ConfiguredFeature<?, ?> MINT_POND = NeapolitanFeatures.MINT_POND.get().withConfiguration(IFeatureConfig.NO_FEATURE_CONFIG).withPlacement(Features.Placements.HEIGHTMAP_PLACEMENT).chance(NeapolitanConfig.COMMON.mintPondGenerationChance.get());

		public static final ConfiguredFeature<?, ?> BANANA_PLANT = NeapolitanFeatures.BANANA_PLANT.get().withConfiguration(NoFeatureConfig.field_236559_b_).withPlacement(Features.Placements.HEIGHTMAP_PLACEMENT);
		public static final ConfiguredFeature<?, ?> BANANA_PLANT_BEACH = BANANA_PLANT.withPlacement(Placement.COUNT_EXTRA.configure(new AtSurfaceWithExtraConfig(0, 0.075F, 1)));
		public static final ConfiguredFeature<?, ?> BANANA_PLANT_JUNGLE = BANANA_PLANT.withPlacement(Placement.COUNT_EXTRA.configure(new AtSurfaceWithExtraConfig(1, 0.30F, 3)));
		public static final ConfiguredFeature<?, ?> BANANA_PLANT_RARE = BANANA_PLANT.withPlacement(Placement.COUNT_EXTRA.configure(new AtSurfaceWithExtraConfig(0, 0.25F, 1)));
		public static final ConfiguredFeature<?, ?> BANANA_PLANT_VERY_RARE = BANANA_PLANT.withPlacement(Placement.COUNT_EXTRA.configure(new AtSurfaceWithExtraConfig(0, 0.15F, 1)));

		private static <FC extends IFeatureConfig> void register(String name, ConfiguredFeature<FC, ?> configuredFeature) {
			Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, new ResourceLocation(Neapolitan.MOD_ID, name), configuredFeature);
		}

		public static void registerConfiguredFeatures() {
			register("patch_strawberry_bush", PATCH_STRAWBERRY_BUSH);
			register("patch_vanilla_vine", PATCH_VANILLA_VINE);
			register("patch_adzuki_sprouts", PATCH_ADZUKI_SPROUTS);
			register("mint_pond", MINT_POND);

			register("banana_plant", BANANA_PLANT);
			register("banana_plant_beach", BANANA_PLANT_BEACH);
			register("banana_plant_jungle", BANANA_PLANT_JUNGLE);
			register("banana_plant_rare", BANANA_PLANT_RARE);
			register("banana_plant_very_rare", BANANA_PLANT_VERY_RARE);
		}
	}
}
