package fr.dynamx.common.physics.entities.modules;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IEngineModule;
import fr.dynamx.api.physics.entities.IPropulsionHandler;
import fr.dynamx.common.contentpack.parts.PartWheel;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.EngineModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.physics.entities.BaseVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.BaseWheeledVehiclePhysicsHandler;
import fr.dynamx.common.physics.entities.parts.wheel.PacejkaMagicFormula;
import fr.dynamx.common.physics.entities.parts.wheel.WheelPhysics;
import fr.dynamx.common.physics.entities.parts.wheel.WheelState;
import fr.dynamx.utils.optimization.Vector3fPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @see IPropulsionHandler
 * @see WheelsModule
 */
@RequiredArgsConstructor
public class WheelsPhysicsHandler implements IPropulsionHandler {
    private final WheelsModule module;
    private final BaseWheeledVehiclePhysicsHandler<? extends BaseVehicleEntity<?>> handler;

    public BiMap<Byte, Byte> wheelIDByPartID = HashBiMap.create();
    public List<WheelPhysics> vehicleWheelData = new ArrayList<>();

    public PacejkaMagicFormula pacejkaMagicFormula;

    private static final Vector3f direction = new Vector3f(0, -1, 0);
    private static final Vector3f axle = new Vector3f(-1, 0, 0);

    @Getter
    private boolean isAccelerating;

    public BaseWheeledVehiclePhysicsHandler<? extends BaseVehicleEntity<?>> getHandler() {
        return handler;
    }

    public byte getNumWheels() {
        return (byte) vehicleWheelData.size();
    }

    public WheelPhysics getWheel(int index) {
        return vehicleWheelData.get(index);
    }

    public WheelPhysics getWheelByPartIndex(byte index) {
        return vehicleWheelData.get(wheelIDByPartID.get(index));
    }

    public void init() {
        handler.getPackInfo().getPartsByType(PartWheel.class).forEach(partWheel -> partWheel.addPart(handler.getHandledEntity()));
        pacejkaMagicFormula = new PacejkaMagicFormula(this);
    }

    public void addWheel(PartWheel partWheel, PartWheelInfo wheelInfo) {
        Vector3f wheelPosition = Vector3fPool.get(partWheel.getPosition()).addLocal(handler.getPackInfo().getCenterOfMass()).addLocal(0, -wheelInfo.getSuspensionRestLength(), 0);
        VehicleWheel vehicleWheel = handler.getPhysicsVehicle().addWheel(wheelPosition, direction, axle, wheelInfo.getSuspensionRestLength(), wheelInfo.getWheelRadius(), partWheel.isWheelIsSteerable());
        byte index = (byte) (handler.getPhysicsVehicle().getNumWheels() - 1);
        WheelPhysics wheelPhysics = new WheelPhysics(handler.getPhysicsVehicle(), vehicleWheel, index, partWheel);
        for (Map.Entry<Byte, Byte> entry : wheelIDByPartID.entrySet()) {
            byte newWheelID = entry.getValue();
            if (newWheelID >= index)
                entry.setValue(++newWheelID);
        }
        vehicleWheelData.add(wheelPhysics);
        wheelIDByPartID.put(partWheel.getId(), index);
    }

    public void update() {
        pacejkaMagicFormula.update();
    }

    public void removeWheel(byte partID) {
        if (getNumWheels() > 0) {
            if (wheelIDByPartID.get(partID) != null) {
                byte wheelID = wheelIDByPartID.get(partID);
                for (WheelPhysics wheelPhysics : vehicleWheelData) {
                    if (wheelPhysics.getWheelIndex() > wheelID)
                        wheelPhysics.setWheelIndex((byte) (wheelPhysics.getWheelIndex() - 1));
                }
                for (PartWheel partWheel : handler.getPackInfo().getPartsByType(PartWheel.class)) {
                    if (partID == partWheel.getId()) {
                        module.getWheelsStates()[partID] = WheelState.REMOVED; //removed state
                        handler.getPhysicsVehicle().removeWheel(wheelID);
                        wheelIDByPartID.remove(partID, wheelID);
                        vehicleWheelData.remove(wheelID);
                    }
                }
                for (Map.Entry<Byte, Byte> entry : wheelIDByPartID.entrySet()) {
                    byte newWheelID = entry.getValue();
                    if (newWheelID > wheelID)
                        entry.setValue(--newWheelID);
                }
            }
        }
    }

    @Override
    public void accelerate(IEngineModule engine, float strength, float speedLimit) {
        EngineModule engine2 = (EngineModule) engine;
        EnginePhysicsHandler module = engine2.getPhysicsHandler();
        if (module.getEngine().isStarted()) {
            for (WheelPhysics wheelPhysics : vehicleWheelData) {
                if (wheelPhysics.isDrivingWheel()) {
                    if (strength != 0 && module.isEngaged() && Math.abs(handler.getSpeed(BaseVehiclePhysicsHandler.SpeedUnit.KMH)) < speedLimit) {
                        float power = (module.getEngine().getPowerOutputAtRevs());
                        wheelPhysics.accelerate(power * strength * 2);
                        isAccelerating = true;
                    } else
                        wheelPhysics.accelerate(0);
                } else {
                    // we always set this because the wheel could be "broken down" over time.
                    wheelPhysics.accelerate(0);
                }
            }
        }
    }

    @Override
    public void disengageEngine() {
        for (WheelPhysics wheelPhysics : vehicleWheelData) {
            wheelPhysics.accelerate(0);
        }
    }

    @Override
    public void brake(float strength) {
        for (WheelPhysics wheelPhysics : vehicleWheelData) {
            wheelPhysics.brake(strength, 0);
        }
    }

    @Override
    public void handbrake(float strength) {
        // just apply the brakes to the rear wheels.
        for (WheelPhysics wheel : vehicleWheelData) {
            if (wheel.isHandBrakingWheel())
                wheel.brake(0, strength);
        }
    }

    @Override
    public void steer(float strength) {
        for (WheelPhysics wheelPhysics : vehicleWheelData) {
            wheelPhysics.steer(strength);
        }
    }

    @Override
    public void applyEngineBraking(IEngineModule engine) {
        EngineModule engine2 = (EngineModule) engine;
        disengageEngine();
        for (int i = 0; i < getNumWheels(); i++) {
            WheelPhysics wheelPhysics = getWheel(i);
            if (wheelPhysics.isDrivingWheel())
                wheelPhysics.brake(0, 0, engine2.getPhysicsHandler().getEngine().getBraking());
        }
    }

    public List<WheelPhysics> getWheels() {
        return vehicleWheelData;
    }
}
