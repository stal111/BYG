package potionstudios.byg.common.world.structure;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import potionstudios.byg.BYG;
import potionstudios.byg.common.world.structure.arch.ArchStructure;
import potionstudios.byg.common.world.structure.volcano.VolcanoStructure;
import potionstudios.byg.reg.RegistrationProvider;
import potionstudios.byg.reg.RegistryObject;

import java.util.function.Supplier;

public class BYGStructureTypes {

    public static final RegistrationProvider<StructureType<?>> PROVIDER = RegistrationProvider.get(Registry.STRUCTURE_TYPES, BYG.MOD_ID);

    public static final RegistryObject<StructureType<ArchStructure>> ARCH = register("arch", () -> ArchStructure.CODEC);
    public static final RegistryObject<StructureType<VolcanoStructure>> VOLCANO = register("volcano", () -> VolcanoStructure.CODEC);

    private static <S extends Structure> RegistryObject<StructureType<S>> register(String id, Supplier<? extends Codec<S>> codec) {
        return PROVIDER.register(id, () -> new StructureType<S>() {
            @Override
            public Codec<S> codec() {
                return codec.get();
            }
        });
    }

    public static void loadClass() {
    }
}
