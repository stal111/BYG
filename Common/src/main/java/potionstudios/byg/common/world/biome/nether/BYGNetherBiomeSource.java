package potionstudios.byg.common.world.biome.nether;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.NotNull;
import potionstudios.byg.BYG;
import potionstudios.byg.common.world.biome.LayersBiomeData;
import potionstudios.byg.common.world.biome.LazyLoadSeed;
import potionstudios.byg.common.world.math.noise.fastnoise.lite.FastNoiseLite;
import potionstudios.byg.util.BYGUtil;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static potionstudios.byg.util.BYGUtil.createBiomesFromBiomeData;

public abstract class BYGNetherBiomeSource extends BiomeSource implements LazyLoadSeed {
    public static final ResourceLocation LOCATION = BYG.createLocation("nether");

    private  FastNoiseLite lowerLayerRoughnessNoise;
    private FastNoiseLite upperLayerRoughnessNoise;
    private final Registry<Biome> biomeRegistry;
    private BiomeResolver upperBiomeResolver;
    private BiomeResolver middleBiomeResolver;
    private BiomeResolver bottomResolver;
    private final int bottomTopY;

    protected BYGNetherBiomeSource(Registry<Biome> biomeRegistry) {
        super(getPossibleBiomes(biomeRegistry));
        this.biomeRegistry = biomeRegistry;


        NetherBiomesConfig config = NetherBiomesConfig.getConfig();
        Set<ResourceKey<Biome>> possibleBiomes = possibleBiomes().stream().map(Holder::unwrapKey).map(Optional::orElseThrow).collect(Collectors.toSet());
        BiPredicate<Collection<ResourceKey<Biome>>, ResourceKey<Biome>> filter = (existing, added) -> !existing.contains(added) && possibleBiomes.contains(added);

        int usedLayerSize = config.layerSize();
        this.bottomTopY = QuartPos.fromBlock(usedLayerSize);
    }

    @Override
    public void lazyLoad(long seed) {
        NetherBiomesConfig config = NetherBiomesConfig.getConfig();
        Set<ResourceKey<Biome>> possibleBiomes = possibleBiomes().stream().map(Holder::unwrapKey).map(Optional::orElseThrow).collect(Collectors.toSet());
        BiPredicate<Collection<ResourceKey<Biome>>, ResourceKey<Biome>> filter = (existing, added) -> !existing.contains(added) && possibleBiomes.contains(added);

        this.lowerLayerRoughnessNoise = new FastNoiseLite((int) seed);
        this.lowerLayerRoughnessNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        this.lowerLayerRoughnessNoise.SetFrequency(0.005F);

        this.upperLayerRoughnessNoise = new FastNoiseLite((int) seed + 43594389);
        this.upperLayerRoughnessNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        this.upperLayerRoughnessNoise.SetFrequency(0.005F);

        this.upperBiomeResolver = getUpperBiomeResolver(biomeRegistry, seed, config.upperLayer().filter(filter));
        this.middleBiomeResolver = getMiddleBiomeResolver(biomeRegistry, seed, config.middleLayer().filter(filter));
        this.bottomResolver = getLowerBiomeResolver(biomeRegistry, seed, config.bottomLayer().filter(filter));
    }

    public abstract BiomeResolver getUpperBiomeResolver(Registry<Biome> biomeRegistry, long seed, LayersBiomeData upperLayerBiomeData);

    public abstract BiomeResolver getMiddleBiomeResolver(Registry<Biome> biomeRegistry, long seed, LayersBiomeData middleLayerBiomeData);

    public abstract BiomeResolver getLowerBiomeResolver(Registry<Biome> biomeRegistry, long seed, LayersBiomeData lowerLayerBiomeData);


    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        if (y < bottomTopY + this.lowerLayerRoughnessNoise.GetNoise(x, z) * 5) {
            return this.bottomResolver.getNoiseBiome(x, y, z, sampler);
        } else if (y > bottomTopY + bottomTopY + this.upperLayerRoughnessNoise.GetNoise(x, z) * 5) {
            return this.upperBiomeResolver.getNoiseBiome(x, y, z, sampler);
        } else {
            return this.middleBiomeResolver.getNoiseBiome(x, y, z, sampler);
        }
    }

    protected Registry<Biome> getBiomeRegistry() {
        return biomeRegistry;
    }

    @NotNull
    private static List<Holder<Biome>> getPossibleBiomes(Registry<Biome> biomeRegistry) {
        NetherBiomesConfig config = NetherBiomesConfig.getConfig(false, false, biomeRegistry);

        Set<String> missingBiomes = new TreeSet<>();
        BiPredicate<Collection<ResourceKey<Biome>>, ResourceKey<Biome>> filter = (existing, added) -> {
            boolean biomeRegistryHas = biomeRegistry.containsKey(added);

            if (!biomeRegistryHas) {
                missingBiomes.add(added.location().toString());
            }

            return !existing.contains(added) && biomeRegistryHas;
        };

        LayersBiomeData upperLayer = config.upperLayer().filter(filter);
        LayersBiomeData middleLayer = config.middleLayer().filter(filter);
        LayersBiomeData bottomLayer = config.bottomLayer().filter(filter);

        String ignored = BYGUtil.dumpCollection(missingBiomes);
        if (!ignored.isEmpty()) {
            BYG.LOGGER.warn(String.format("Config \"%s\" warned:\nThe following biome entries were ignored due to not being in this world's biome registry:\n%s", NetherBiomesConfig.CONFIG_PATH.get(), ignored.toString()));
        }
        return createBiomesFromBiomeData(biomeRegistry, upperLayer, middleLayer, bottomLayer);
    }
}