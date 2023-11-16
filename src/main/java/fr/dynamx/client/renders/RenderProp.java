package fr.dynamx.client.renders;

import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PropsEntity;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.utils.debug.renderer.BoatDebugRenderer;
import fr.dynamx.utils.debug.renderer.DebugRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.common.MinecraftForge;

public class RenderProp<T extends PropsEntity<?>> extends RenderPhysicsEntity<T> {
    public RenderProp(RenderManager manager) {
        super(manager);
        addDebugRenderers(new BoatDebugRenderer.FloatsDebug(), new DebugRenderer.SeatDebug());
        MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.InitRenderer<>(PropsEntity.class, this));
    }

    @Override
    public void renderMain(T entity, float partialsTicks) {
        // Scale to the config scale value
        GlStateManager.scale(entity.getPackInfo().getScaleModifier().x, entity.getPackInfo().getScaleModifier().y, entity.getPackInfo().getScaleModifier().z);
        //Render the model
        renderModel(DynamXContext.getDxModelRegistry().getModel(entity.getPackInfo().getModel()), entity, (byte) entity.getMetadata(), false);
    }

    @Override
    public void renderParts(T entity, float partialTicks) {
        //TODO WIP PARTS ON PROPS
        /*if (entity.getPackInfo().isModelValid()) {
            entity.getPackInfo().getDrawableParts().forEach(d -> ((IDrawablePart<T>) d).drawParts(entity, this, entity.getPackInfo(), entity.getEntityTextureID(), partialTicks));
        }*/
        if (entity.getPackInfo().isModelValid()) {
            if(entity.hasModuleOfType(AbstractLightsModule.class))
                entity.getPackInfo().getOwner().getLightSources().values().forEach(d -> d.drawLights(null, entity.ticksExisted, entity.getPackInfo().getModel(), entity.getPackInfo().getScaleModifier(), entity.getModuleByType(AbstractLightsModule.class), false));
        }
    }
}
