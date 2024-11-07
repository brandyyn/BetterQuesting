package betterquesting.api.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.eventhandler.Event;

public class BQLivingUpdateEvent extends Event {

    public final EntityLivingBase entityLiving;
    public final Entity entity;

    public BQLivingUpdateEvent(EntityPlayerMP player) {
        this.entityLiving = player;
        this.entity = player;
    }
}
