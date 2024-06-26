package fr.dynamx.common.entities.modules;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.entities.modules.ISeatsModule;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.PartSeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.network.sync.MessageSeatsSync;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

import static fr.dynamx.common.DynamXMain.log;

public class SeatsModule implements ISeatsModule {
    protected final BaseVehicleEntity<?> entity;
    protected BiMap<PartSeat, EntityPlayer> seatToPassenger = HashBiMap.create();
    protected Map<Byte, Boolean> doorsStatus;

    public SeatsModule(BaseVehicleEntity<?> entity) {
        this.entity = entity;
    }

    @Override
    public void initEntityProperties() {
        for (PartSeat s : entity.getPackInfo().getPartsByType(PartSeat.class)) {
            if (s.hasDoor()) {
                if (doorsStatus == null)
                    doorsStatus = new HashMap<>();
                doorsStatus.put(s.getId(), false);
            }
        }
    }

    public boolean isPlayerSitting(EntityPlayer player) {
        return seatToPassenger.containsValue(player);
    }

    @Override
    public PartSeat getRidingSeat(Entity entity) {
        return seatToPassenger.inverse().get(entity);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isLocalPlayerDriving() {
        PartSeat seat = getRidingSeat(Minecraft.getMinecraft().player);
        return seat != null && seat.isDriver();
    }

    @Override
    public PartSeat getLastRiddenSeat() {
        return lastSeat;
    }

    @Override
    public BiMap<PartSeat, EntityPlayer> getSeatToPassengerMap() {
        return seatToPassenger;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        seatToPassenger.forEach((s, p) -> tag.setString("Seat" + s.getId(), p.getUniqueID().toString()));
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        for (PartSeat seat : entity.getPackInfo().getPartsByType(PartSeat.class)) {
            if (tag.hasKey("Seat" + seat.getId(), Constants.NBT.TAG_STRING)) {
                EntityPlayer player = entity.getServer().getPlayerList().getPlayerByUUID(UUID.fromString(tag.getString("Seat" + seat.getId())));
                if (player != null) {
                    seatToPassenger.put(seat, player);
                }
            }
        }
    }

    @Nullable
    @Override
    public Entity getControllingPassenger() {
        for (Map.Entry<PartSeat, EntityPlayer> e : seatToPassenger.entrySet()) {
            if (e.getKey().isDriver()) {
                return e.getValue();
            }
        }
        return null;
    }

    @Override
    public void updatePassenger(Entity passenger) {
        PartSeat seat = getRidingSeat(passenger);
        //System.out.println("Update passenger : "+passenger+" on "+seat);
        if (seat != null) {
            Vector3fPool.openPool();
            Vector3f posVec = DynamXGeometry.rotateVectorByQuaternion(seat.getPosition(), entity.renderRotation);//PhysicsHelper.getRotatedPoint(seat.getPosition(), -this.rotationPitch, this.rotationYaw, this.rotationRoll);
            passenger.setPosition(entity.posX + posVec.x, entity.posY + posVec.y, entity.posZ + posVec.z);
            Vector3fPool.closePool();
        }
    }

    /**
     * Rotates the passenger, limiting his field of view to avoid stiff necks
     */
    @Override
    public void applyOrientationToEntity(Entity passenger) {
        float f = MathHelper.wrapDegrees(passenger.rotationYaw);
        float f1 = MathHelper.clamp(f, -105.0F, 105.0F);
        passenger.rotationYaw = f1;
        f = MathHelper.wrapDegrees(passenger.prevRotationYaw);
        f1 = MathHelper.clamp(f, -105.0F, 105.0F);
        passenger.prevRotationYaw = f1;
    }

    @Override
    public void addPassenger(Entity passenger) {
        if (!entity.world.isRemote && passenger instanceof EntityPlayer) {
            PartSeat hitPart = seatToPassenger.inverse().get(passenger);
            if (hitPart != null) {
                if (hitPart.isDriver()) {
                    entity.getNetwork().onPlayerStartControlling((EntityPlayer) passenger, true);
                }
                MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.MountVehicleEntityEvent(Side.SERVER, (EntityPlayer) passenger, entity, this, hitPart));
                //System.out.println("Send seat sync : add passenger "+entity+" "+passenger);
                DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) entity), EnumPacketTarget.ALL_TRACKING_ENTITY, entity);
            } else {
                log.error("Cannot add passenger : " + passenger + " : seat not found !");
            }
        }
        //Client side is managed by updateSeats
    }

    public PartSeat lastSeat;

    @Override
    public void removePassenger(Entity passenger) {
        PartSeat seat = getRidingSeat(passenger);
        if (!entity.world.isRemote && seat != null) {
            lastSeat = seat;
            seatToPassenger.remove(seat);
            if (seat.isDriver() && passenger instanceof EntityPlayer) {
                entity.getNetwork().onPlayerStopControlling((EntityPlayer) passenger, true);
            }
            //System.out.println("Send seat sync : remove passenger "+entity+" "+passenger);
            DynamXContext.getNetwork().sendToClient(new MessageSeatsSync((IModuleContainer.ISeatsContainer) entity), EnumPacketTarget.ALL_TRACKING_ENTITY, entity);
            MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.DismountVehicleEntityEvent(entity.world.isRemote ? Side.CLIENT : Side.SERVER, (EntityPlayer) passenger, entity, this, seat));
        }
        //Client side is managed by updateSeats
    }

    @Override
    public void updateSeats(MessageSeatsSync msg, PhysicsEntityNetHandler<?> netHandler) {
        BaseVehicleEntity<?> vehicleEntity = entity;
        List<PartSeat> remove = new ArrayList<>(0);
        //Search for players who dismounted the entity
        for (Map.Entry<PartSeat, EntityPlayer> e : seatToPassenger.entrySet()) {
            if (!msg.getSeatToEntity().containsValue(e.getValue().getEntityId())) {
                //System.out.println("Removing "+e.getValue()+" from seat "+e.getKey()+" of "+entity);
                remove.add(e.getKey());
                if (e.getKey().isDriver()) {
                    netHandler.onPlayerStopControlling(e.getValue(), true);
                }
                MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.DismountVehicleEntityEvent(Side.CLIENT, e.getValue(), vehicleEntity, this, e.getKey()));
            }
        }
        //And remove them
        if (!remove.isEmpty())
            remove.forEach(seatToPassenger::remove);
        //Search for players who mounted on the entity
        if (entity.getPackInfo() == null) //The seats may be sync before the entity's vehicle info is initialized
        {
            entity.setPackInfo(DynamXObjectLoaders.WHEELED_VEHICLES.findInfo(entity.getInfoName()));
            if (entity.getPackInfo() == null)
                entity.setPackInfo(DynamXObjectLoaders.TRAILERS.findInfo(entity.getInfoName()));
            if (entity.getPackInfo() == null)
                entity.setPackInfo(DynamXObjectLoaders.BOATS.findInfo(entity.getInfoName()));
            if (entity.getPackInfo() == null)
                log.fatal("Failed to find info " + entity.getInfoName() + " for modular entity seats sync. Entity : " + entity);
        }
        for (Map.Entry<Byte, Integer> e : msg.getSeatToEntity().entrySet()) {
            PartSeat seat = vehicleEntity.getPackInfo().getPartByTypeAndId(PartSeat.class, e.getKey());
            if (seat != null) {
                Entity player = entity.world.getEntityByID(e.getValue());
                if (player instanceof EntityPlayer) {
                    //System.out.println("Try set "+player+" in "+seat+" of "+entity+" cur "+seatToPassenger);
                    if (seatToPassenger.get(seat) != player) { //And add them
                        seatToPassenger.put(seat, (EntityPlayer) player);
                        if (seat.isDriver()) {
                            netHandler.onPlayerStartControlling((EntityPlayer) player, true);
                        }
                        MinecraftForge.EVENT_BUS.post(new VehicleEntityEvent.MountVehicleEntityEvent(Side.CLIENT, (EntityPlayer) player, vehicleEntity, this, seat));
                    }
                    //System.out.println("Success to seat-sync " + player + " !");
                } else {
                    log.warn("Player with id " + e.getValue() + " not found for seat in " + entity);
                }
            } else {
                log.warn("Seat with id " + e.getKey() + " not found in " + entity);
            }
        }
    }
}
