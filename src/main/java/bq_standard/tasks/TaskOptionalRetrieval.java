package bq_standard.tasks;

import java.util.UUID;

import net.minecraft.util.ResourceLocation;

import bq_standard.core.BQ_Standard;
import bq_standard.tasks.factory.FactoryTaskOptionalRetrieval;

public class TaskOptionalRetrieval extends TaskRetrieval {

    @Override
    public String getUnlocalisedName() {
        return BQ_Standard.MODID + ".task.optional_retrieval";
    }

    @Override
    public ResourceLocation getFactoryID() {
        return FactoryTaskOptionalRetrieval.INSTANCE.getRegistryName();
    }

    @Override
    public boolean ignored(UUID uuid) {
        return true;
    }
}
