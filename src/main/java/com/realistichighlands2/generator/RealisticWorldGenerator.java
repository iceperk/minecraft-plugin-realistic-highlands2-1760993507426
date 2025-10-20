package com.realistichighlands2.generator;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.PerlinNoiseGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RealisticWorldGenerator extends ChunkGenerator {

    private PerlinNoiseGenerator heightNoise;
    private PerlinNoiseGenerator biomeNoise;
    private PerlinNoiseGenerator featureNoise;

    private final double HEIGHT_SCALE = 120.0; // Ogólna wysokość gór
    private final double RIDGE_SCALE = 0.008; // Skala pasm górskich (im mniejsza, tym większe pasma)
    private final double DETAIL_SCALE = 0.02; // Drobniejsze detale terenu

    private final double BIOME_SCALE = 0.005; // Skala dla przejść biomów
    private final double FEATURE_SCALE = 0.05; // Skala dla rzek/jezior

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        if (heightNoise == null || biomeNoise == null || featureNoise == null) {
            initNoise(world.getSeed());
        }

        ChunkData chunk = create    ChunkData(world);

        int worldMinY = world.getMinHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int blockX = chunkX * 16 + x;
                int blockZ = chunkZ * 16 + z;

                // Generowanie wysokości terenu z uwzględnieniem dolin i szczytów
                double heightValue = heightNoise.noise(blockX * RIDGE_SCALE, blockZ * RIDGE_SCALE, 0.5, 2, true);
                heightValue += heightNoise.noise(blockX * DETAIL_SCALE, blockZ * DETAIL_SCALE, 0.5, 3, false) * 0.3; // Drobniejsze detale
                heightValue = (heightValue + 1) / 2; // Normalizacja do 0-1
                
                // Większa różnorodność wysokości
                double surfaceHeight = worldMinY + 60 + heightValue * HEIGHT_SCALE;

                // Symulacja rzek i jezior
                double featureValue = featureNoise.noise(blockX * FEATURE_SCALE, blockZ * FEATURE_SCALE, 0.5, 2, true);
                if (featureValue < -0.4 && surfaceHeight < worldMinY + 65) { // Tworzenie głębokich dolin/koryt rzecznych
                    surfaceHeight = worldMinY + 60 + featureValue * 15; // Obniżamy teren dla rzek/jezior
                } else if (featureValue > 0.4 && surfaceHeight > worldMinY + 100) {
                    // Może stworzyć wyższe 'płaskowyże' na szczytach
                }

                int y = (int) surfaceHeight;
                
                // Woda i dno rzek/jezior
                if (y < worldMinY + 63) { // Poziom wody
                    chunk.setBlock(x, y, z, Material.GRAVEL); // Dno jeziora/rzeki
                    for (int waterY = y + 1; waterY < worldMinY + 63; waterY++) {
                        chunk.setBlock(x, waterY, z, Material.WATER);
                    }
                    y = worldMinY + 62; // Ustawiamy poziom terenu nad wodą
                }

                // Obliczenie biomu
                double biomeFactor = biomeNoise.noise(blockX * BIOME_SCALE, blockZ * BIOME_SCALE, 0.5, 2, true);
                Biome currentBiome = getBiomeFromNoise(biomeFactor, y);
                biome.setBiome(x, z, currentBiome);

                // Ułożenie bloków
                for (int currentY = worldMinY; currentY <= y; currentY++) {
                    if (currentY == y) {
                        chunk.setBlock(x, currentY, z, getSurfaceMaterial(currentBiome));
                    } else if (currentY > y - 5) { // Warstwa ziemi pod powierzchnią
                        chunk.setBlock(x, currentY, z, Material.DIRT);
                    } else {
                        chunk.setBlock(x, currentY, z, Material.STONE);
                    }
                }

                // Dodawanie sporadycznej skały na szczytach gór
                if (y > worldMinY + 140 && random.nextDouble() < 0.1) {
                    chunk.setBlock(x, y + 1, z, Material.STONE);
                }
            }
        }
        return chunk;
    }

    private void initNoise(long seed) {
        Random seedRandom = new Random(seed);
        heightNoise = new PerlinNoiseGenerator(seedRandom.nextLong());
        biomeNoise = new PerlinNoiseGenerator(seedRandom.nextLong());
        featureNoise = new PerlinNoiseGenerator(seedRandom.nextLong());
    }

    private Biome getBiomeFromNoise(double biomeFactor, int y) {
        // Normalizacja biomeFactor do 0-1
        biomeFactor = (biomeFactor + 1) / 2;

        int worldMinY = Bukkit.getWorlds().get(0).getMinHeight(); // Przykład, można zrobić to dynamicznie

        if (y < worldMinY + 63) return Biome.RIVER; // Jeśli jest pod wodą, to rzeka/ocean
        if (y < worldMinY + 70) return Biome.BEACH; // Niskie tereny przy wodzie

        if (biomeFactor < 0.2) {
            return Biome.SNOWY_TAIGA; // Wysokie i zimne
        } else if (biomeFactor < 0.4) {
            return Biome.FOREST; // Umiarkowane
        } else if (biomeFactor < 0.6) {
            return Biome.PLAINS; // Otwarte przestrzenie
        } else if (biomeFactor < 0.8) {
            return Biome.DARK_FOREST; // Gęste lasy
        } else {
            return Biome.MEADOW; // Łąki
        }
    }

    private Material getSurfaceMaterial(Biome biome) {
        switch (biome) {
            case SNOWY_TAIGA:
            case SNOWY_PLAINS:
            case SNOWY_SLOPES:
                return Material.SNOW_BLOCK;
            case RIVER:
            case BEACH:
                return Material.GRAVEL;
            case DESERT:
                return Material.SAND;
            default:
                return Material.GRASS_BLOCK;
        }
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new RealisticBiomeProvider(worldInfo.getSeed());
    }

    @Override
    public boolean shouldGenerateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public boolean shouldGenerateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public boolean shouldGenerateBedrock(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public boolean shouldGenerateCaves(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return true; // Domyślna generacja jaskiń
    }

    @Override
    public boolean shouldGenerateDecorations(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return true; // Domyślna generacja dekoracji (drzewa, kwiaty)
    }

    @Override
    public boolean shouldGenerateMobs(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return true; // Domyślna generacja mobów
    }

    @Override
    public boolean shouldGenerateStructures(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return true; // Domyślna generacja struktur (np. wioski, fortece)
    }

    // Implementacja BiomeProvider, aby świat prawidłowo rozpoznawał biomy
    public static class RealisticBiomeProvider extends BiomeProvider {
        private PerlinNoiseGenerator biomeNoise;
        private final double BIOME_SCALE = 0.005;

        public RealisticBiomeProvider(long seed) {
            this.biomeNoise = new PerlinNoiseGenerator(new Random(seed).nextLong());
        }

        @Override
        public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
            // W Generowaniu ChunksData już obliczamy wysokość i na jej podstawie biom.
            // Tutaj potrzebujemy tylko biomu na podstawie pozycji X,Z.
            double biomeFactor = biomeNoise.noise(x * BIOME_SCALE, z * BIOME_SCALE, 0.5, 2, true);
            biomeFactor = (biomeFactor + 1) / 2; // Normalizacja do 0-1

            // Ponieważ nie mamy tutaj informacji o wysokości y wygenerowanego terenu,
            // musimy uprościć logikę lub przekazać "średnią" wysokość.
            // Dla uproszczenia, w BiomeProviderze przyjmujemy, że jesteśmy "na powierzchni".
            if (y < worldInfo.getMinHeight() + 63) return Biome.RIVER; // Pod poziomem morza

            if (biomeFactor < 0.2) {
                return Biome.SNOWY_TAIGA;
            } else if (biomeFactor < 0.4) {
                return Biome.FOREST;
            } else if (biomeFactor < 0.6) {
                return Biome.PLAINS;
            } else if (biomeFactor < 0.8) {
                return Biome.DARK_FOREST;
            } else {
                return Biome.MEADOW;
            }
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            // Zwraca listę wszystkich biomów, które mogą być generowane przez ten generator
            return Arrays.asList(Biome.PLAINS, Biome.FOREST, Biome.DARK_FOREST, Biome.MEADOW,
                                 Biome.RIVER, Biome.BEACH, Biome.SNOWY_TAIGA);
        }
    }

        @Override
    public Location get   FixedSpawnLocation(World world, Random random) {
        return new Location(world, 0, world.getHighestBlockYAt(0, 0, HeightMap.WORLD_SURFACE) + 2, 0);
    }
}
