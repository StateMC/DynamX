package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import net.minecraft.util.math.AxisAlignedBB;

public class PartFloat extends BasePart<ModularVehicleInfoBuilder>
{
    public AxisAlignedBB box;

    public PartFloat(ModularVehicleInfoBuilder owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder vehicleInfo) {
        super.appendTo(vehicleInfo);
        Vector3f min = getPosition().subtract(getScale());
        Vector3f max = getPosition().add(getScale());
        this.box = new AxisAlignedBB(
                min.x, min.y, min.z,
                max.x, max.y, max.z);
    }

    @Override
    public String getName() {
        return "PartFloat named "+getPartName()+" in "+getOwner().getName();
    }
}
