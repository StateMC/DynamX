package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;

/**
 * Info of the steering wheel of a {@link ModularVehicleInfo}
 */
@RegisteredSubInfoType(name = "steeringwheel", registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
public class SteeringWheelInfo extends SubInfoType<ModularVehicleInfo> {
    @PackFileProperty(configNames = "PartName", required = false, defaultValue = "SteeringWheel")
    private String partName = "SteeringWheel";
    @PackFileProperty(configNames = {"BaseRotation", "BaseRotationQuat"}, required = false, defaultValue = "0 0 0 1")
    private Quaternion steeringWheelBaseRotation = null;
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    private Vector3f position = new Vector3f(0.5f, 1.1f, 1);

    public SteeringWheelInfo(ModularVehicleInfo owner) {
        super(owner);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        owner.addSubProperty(this);
        getSteeringWheelPosition().multLocal(owner.getScaleModifier());
        owner.addRenderedParts(getPartName());
    }

    public String getPartName() {
        return partName;
    }

    public Quaternion getSteeringWheelBaseRotation() {
        return steeringWheelBaseRotation;
    }

    public Vector3f getSteeringWheelPosition() {
        return position;
    }

    @Override
    public String getName() {
        return "SteeringWheel";
    }
}
