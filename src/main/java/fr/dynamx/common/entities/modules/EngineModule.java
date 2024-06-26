package fr.dynamx.common.entities.modules;

import fr.dynamx.api.audio.EnumSoundState;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.physics.entities.IEnginePhysicsHandler;
import fr.dynamx.client.ClientProxy;
import fr.dynamx.client.handlers.hud.CarController;
import fr.dynamx.client.sound.EngineSound;
import fr.dynamx.common.contentpack.type.vehicle.EngineInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.vars.VehicleSynchronizedVariables;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.modules.EnginePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.engine.AutomaticGearboxHandler;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.dynamx.client.ClientProxy.SOUND_HANDLER;

/**
 * Basic {@link IEngineModule} implementation for cars <br>
 * Works with an {@link AutomaticGearboxHandler} and a {@link fr.dynamx.common.entities.modules.WheelsModule}
 *
 * @see fr.dynamx.api.entities.VehicleEntityProperties.EnumEngineProperties
 * @see EnginePhysicsHandler
 */
public class EngineModule implements IEngineModule<AbstractEntityPhysicsHandler<?, ?>>, IPhysicsModule.IPhysicsUpdateListener, IPhysicsModule.IEntityUpdateListener {
    protected final BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity;
    protected final EngineInfo engineInfo;
    protected EnginePhysicsHandler physicsHandler;

    //Handbrake on spawn
    private int controls = 2;
    /**
     * The active speed limit, or Integer.MAX_VALUE
     */
    private float speedLimit = Integer.MAX_VALUE;

    /**
     * @see fr.dynamx.api.entities.VehicleEntityProperties.EnumEngineProperties
     */
    private float[] engineProperties = new float[VehicleEntityProperties.EnumEngineProperties.values().length];

    public EngineModule(BaseVehicleEntity<? extends BaseVehiclePhysicsHandler<?>> entity, EngineInfo engineInfo) {
        this.entity = entity;
        this.engineInfo = engineInfo;
    }

    /**
     * These vars are automatically synchronised from server (or driver) to others
     *
     * @return All engine properties, see {@link fr.dynamx.api.entities.VehicleEntityProperties.EnumEngineProperties}
     */
    @Override
    public float[] getEngineProperties() {
        return engineProperties;
    }

    public float getEngineProperty(VehicleEntityProperties.EnumEngineProperties engineProperty) {
        return engineProperties[engineProperty.ordinal()];
    }

    public EngineInfo getEngineInfo() {
        return engineInfo;
    }

    @Override
    public IEnginePhysicsHandler getPhysicsHandler() {
        return physicsHandler;
    }

    /**
     * Used for synchronization, don't use this
     */
    @Override
    public void setEngineProperties(float[] engineProperties) {
        this.engineProperties = engineProperties;
    }

    public boolean isAccelerating() {
        return EnginePhysicsHandler.inTestFullGo || (controls & 1) == 1;
    }

    public boolean isHandBraking() {
        return (controls & 2) == 2;
    }

    public boolean isReversing() {
        return (controls & 4) == 4;
    }

    public boolean isTurningLeft() {
        return (controls & 8) == 8;
    }

    public boolean isTurningRight() {
        return (controls & 16) == 16;
    }

    @Override
    public void setEngineStarted(boolean started) {
        //System.out.println("Setting start " + started);
        if (started) {
            setControls(controls | 32);
        } else {
            setControls((Integer.MAX_VALUE - 32) & controls);
        }
    }

    @Override
    public boolean isEngineStarted() {
        return (EnginePhysicsHandler.inTestFullGo) || ((controls & 32) == 32);
    }

    public int getControls() {
        return controls;
    }


    /**
     * Set all engine controls <br>
     * If called on client side and if the engine is switched on, plays the starting sound
     */
    public void setControls(int controls) {
        if (entity.world.isRemote && entity.ticksExisted > 60 && !this.isEngineStarted() && (controls & 32) == 32) {
            playStartingSound();
        }
        //System.out.println("Set start " + ((controls & 32) == 32) + " was " + isEngineStarted());
        this.controls = controls;
    }

    /**
     * Resets all controls except engine on/off state
     */
    public void resetControls() {
        setControls(controls & 32 | (controls & 2));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IVehicleController createNewController() {
        return new CarController(entity, this);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        if (tag.getBoolean("isEngineStarted"))
            setControls(controls | 32); //set engine on
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setBoolean("isEngineStarted", isEngineStarted());
    }

    @Override
    public void initPhysicsEntity(@Nullable AbstractEntityPhysicsHandler<?, ?> handler) {
        if (handler != null) {
            physicsHandler = new EnginePhysicsHandler(this, (BaseVehiclePhysicsHandler<?>) handler, ((BaseVehiclePhysicsHandler<?>) handler).getPropulsion());
        }
    }

    @Override
    public void preUpdatePhysics(boolean simulatePhysics) {
        if (simulatePhysics) {
            physicsHandler.update();
        }
    }

    @Override
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            this.engineProperties[VehicleEntityProperties.EnumEngineProperties.SPEED.ordinal()] = entity.physicsHandler.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH);
            this.engineProperties[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] = physicsHandler.getEngine().getRevs();
            //this.engineProperties[VehicleEntityProperties.EnumEngineProperties.MAXREVS.ordinal()] = physicEntity.getEngine().getMaxRevs();
            this.engineProperties[VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR.ordinal()] = physicsHandler.getGearBox().getActiveGearNum();
            //this.engineProperties[VehicleEntityProperties.EnumEngineProperties.MAXSPEED.ordinal()] = physicEntity.getGearBox().getMaxSpeed(Vehicle.SpeedUnit.KMH);
            //this.engineProperties[VehicleEntityProperties.EnumEngineProperties.POWER.ordinal()] = physicsHandler.getEngine().getPower();
            //this.engineProperties[VehicleEntityProperties.EnumEngineProperties.BRAKING.ordinal()] = physicsHandler.getEngine().getBraking();
        }
    }

    @Override
    public void removePassenger(Entity passenger) {
        if (entity.getControllingPassenger() == null) {
            resetControls();
        }
    }

    /**
     * @return The max speed, set by the current driver, or from the vehicle info
     */
    public float getRealSpeedLimit() {
        return speedLimit == Integer.MAX_VALUE ? entity.getPackInfo().getVehicleMaxSpeed() : speedLimit;
    }

    @Override
    public void addSynchronizedVariables(Side side, SimulationHolder simulationHolder, List<ResourceLocation> variables) {
        if (simulationHolder.isPhysicsAuthority(side) || simulationHolder.ownsControls(side)) {
            variables.add(VehicleSynchronizedVariables.Controls.NAME);
        }
    }

    //Sounds

    public final Map<Integer, EngineSound> sounds = new HashMap<>();
    private EngineSound lastVehicleSound;
    private EngineSound currentVehicleSound;

    public EngineSound getCurrentEngineSound() {
        return currentVehicleSound;
    }

    @SideOnly(Side.CLIENT)
    private void playStartingSound() {
        boolean forInterior = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && entity.isRidingOrBeingRiddenBy(Minecraft.getMinecraft().player);
        ClientProxy.SOUND_HANDLER.playSingleSound(entity.physicsPosition, forInterior ? engineInfo.startingSoundInterior : engineInfo.startingSoundExterior, 1, 1);
    }

    @Override
    public boolean listenEntityUpdates(Side side) {
        return side.isClient();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateEntity() {
        if (!MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateVehicleSoundEntityEvent(entity, this, PhysicsEntityEvent.Phase.PRE))) {
            if (entity.getPackInfo() != null) {
                if (engineInfo != null && engineInfo.getEngineSounds() != null) {
                    if (sounds.isEmpty()) { //Sounds are not initialized
                        engineInfo.getEngineSounds().forEach(engineSound -> sounds.put(engineSound.id, new EngineSound(engineSound, entity, this)));
                    }
                    if (isEngineStarted()) {
                        if (engineProperties != null) {
                            boolean forInterior = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 && entity.isRidingOrBeingRiddenBy(Minecraft.getMinecraft().player);
                            float rpm = engineProperties[VehicleEntityProperties.EnumEngineProperties.REVS.ordinal()] * engineInfo.getMaxRevs();
                            lastVehicleSound = currentVehicleSound;
                            if (currentVehicleSound == null || !currentVehicleSound.shouldPlay(rpm, forInterior)) {
                                sounds.forEach((id, vehicleSound) -> {
                                    if (vehicleSound.shouldPlay(rpm, forInterior)) {
                                        this.currentVehicleSound = vehicleSound;
                                    }
                                });
                            }
                        }
                        if (currentVehicleSound != lastVehicleSound) //if playing sound changed
                        {
                            if (lastVehicleSound != null)
                                SOUND_HANDLER.stopSound(lastVehicleSound);
                            if (currentVehicleSound != null) {
                                if (currentVehicleSound.getState() == EnumSoundState.STOPPING) //already playing
                                    currentVehicleSound.onStarted();
                                else
                                    SOUND_HANDLER.playStreamingSound(Vector3fPool.get(currentVehicleSound.getPosX(), currentVehicleSound.getPosY(), currentVehicleSound.getPosZ()), currentVehicleSound);
                            }
                        }
                    } else {
                        if (currentVehicleSound != null)
                            SOUND_HANDLER.stopSound(currentVehicleSound);
                        currentVehicleSound = lastVehicleSound = null;
                    }
                }
            }
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.UpdateVehicleSoundEntityEvent(entity, this, PhysicsEntityEvent.Phase.POST));
        }
    }

    /**
     * The active speed limit, or Integer.MAX_VALUE
     */
    public float getSpeedLimit() {
        return speedLimit;
    }

    public void setSpeedLimit(float speedLimit) {
        this.speedLimit = speedLimit;
    }
}