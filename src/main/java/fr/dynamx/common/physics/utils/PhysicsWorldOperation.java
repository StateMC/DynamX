package fr.dynamx.common.physics.utils;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsVehicle;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * An operation made on the physics world, in the physics thread <br>
 *     See {@link PhysicsWorldOperationType} for a list of available operations <br>
 *         You can add a callback to, for example, add a joint after adding a {@link com.jme3.bullet.objects.PhysicsRigidBody}. The callback is called immediately after this operation
 *
 * @param <A> The added/removed object type
 */
public class PhysicsWorldOperation<A>
{
    private final PhysicsWorldOperationType operation;
    private final A object;
    @Nullable
    private final Callable<PhysicsWorldOperation<?>> callback;

    /**
     * Creates a new PhysicsWorldOperation with no callback
     */
    public PhysicsWorldOperation(PhysicsWorldOperationType operation, A object) {
        this(operation, object, null);
    }

    /**
     * Creates a new PhysicsWorldOperation with a callback
     */
    public PhysicsWorldOperation(PhysicsWorldOperationType operation, A object, @Nullable Callable<PhysicsWorldOperation<?>> callback) {
        this.operation = operation;
        this.object = object;
        this.callback = callback;
    }

    /**
     * Modifies the PhysicsWorld, executing this operation
     *  @param dynamicsWorld The bullet PhysicsSpace
     * @param rigidbodies The cache of added PhysicsCollisionObjects
     * @param vehicles The cache of added vehicles
     * @param joints
     * @param entities The cache of added entities
     */
    public void execute(PhysicsSpace dynamicsWorld, Collection<PhysicsCollisionObject> rigidbodies, Collection<PhysicsVehicle> vehicles, Set<PhysicsJoint> joints, Collection<PhysicsEntity<?>> entities) {
        switch (operation)
        {
            case ADD_VEHICLE:
                if(!vehicles.contains(object))
                    vehicles.add((PhysicsVehicle) object);
                else
                    DynamXMain.log.fatal("PhysicsVehicle "+object+" is already registered, please report this !");
            case ADD_OBJECT:
                if(!rigidbodies.contains(object)) {
                    dynamicsWorld.addCollisionObject((PhysicsCollisionObject) object);
                    rigidbodies.add((PhysicsCollisionObject) object);
                }
                else
                    DynamXMain.log.fatal("PhysicsCollisionObject "+object+" is already registered, please report this !");
                break;
            case REMOVE_VEHICLE:
                vehicles.remove(object);
            case REMOVE_OBJECT:
                if(rigidbodies.contains(object)) {
                    dynamicsWorld.removeCollisionObject((PhysicsCollisionObject) object);
                    rigidbodies.remove(object);
                }
                break;
            case ADD_ENTITY:
                if(entities.contains(object))
                    DynamXMain.log.fatal("Entity "+object+" is already registered, please report this !");
                entities.add((PhysicsEntity<?>) object);
                ((PhysicsEntity<?>) object).isRegistered = 2;
                break;
            case REMOVE_ENTITY:
                PhysicsEntity<?> et = (PhysicsEntity<?>) object;
                entities.remove(et);

                DynamXMain.proxy.scheduleTask(et.world, () -> {
                    List<PhysicsEntity> physicsEntities = et.world.getEntitiesWithinAABB(PhysicsEntity.class, et.getEntityBoundingBox().expand(10,10,10));
                    physicsEntities.forEach(entity -> {
                        if(entity != et){
                            entity.forcePhysicsActivation();
                        }
                    });
                });
                break;
            case ADD_CONSTRAINT:
                if(object != null && !joints.contains(object)) {
                    joints.add((PhysicsJoint) object);
                    dynamicsWorld.addJoint((PhysicsJoint) object);
                }
                else
                    DynamXMain.log.fatal("PhysicsJoint "+object+" is already registered, please report this !");
                break;
            case REMOVE_CONSTRAINT:
                if(joints.contains(object)) {
                    joints.remove(object);
                    dynamicsWorld.removeJoint((PhysicsJoint) object);
                }
                break;
        }
        if(callback != null)
        {
            PhysicsWorldOperation<?> operation = null;
            try {
                operation = callback.call();
                if(operation != null)
                    operation.execute(dynamicsWorld, rigidbodies, vehicles, joints, entities);
            } catch (Exception e) {
                DynamXMain.log.fatal("Exception while executing callback of "+this+". Callback: "+callback, e);
            }
        }
    }

    @Override
    public String toString() {
        return "PhysicsWorldOperation{" +
                "operation=" + operation +
                ", object=" + object +
                '}';
    }

    /**
     * All possible {@link PhysicsWorldOperation}s
     */
    public enum PhysicsWorldOperationType
    {
        ADD_OBJECT, REMOVE_OBJECT, ADD_ENTITY, REMOVE_ENTITY, ADD_VEHICLE, REMOVE_VEHICLE, ADD_CONSTRAINT, REMOVE_CONSTRAINT
    }
}
