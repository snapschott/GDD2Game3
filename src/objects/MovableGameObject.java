package objects;

import java.util.ArrayList;

import buffer.CollisionBuffer;
import engine.Engine;
import engine.manager.CollisionManager;
import mathematics.Vec;

/**
 * Movable game object defines any gameObjects which need to move
 * and have resolved collisions. While all gameobjects can technically move,
 * if there is a collision there's no simple resolution, and if we do nothing 
 * the gameobject will get "stuck" inside of the other object.
 * 
 * Movable game objects solve this problem by tracking a previous position.
 * In case of a collision MovableGameObjects will be reverted back
 * to their previousPosition by the collision manager.
 * 
 * Movable game objects also implement an activeCheckpoint. If movableGameobjects
 * collide with a deathTrigger they are set back to their activeCheckpoint.
 * @author Nex
 *
 */
public class MovableGameObject extends GameObject {

	//Static attributes
	static private double maxSpeed = 0.005;
	
	//Attributes
	protected Vec previousPosition;
	protected Vec netForce;
	protected Vec velocity;
	protected double mass;
	
	//Accessors / Modifiers
	/**
	 * Gets this object's velocity vector
	 * @return the vector containing the velocity of this object
	 */
	public Vec getVelocity(){
		return velocity;
	}
	
	/**
	 * Gets the net force of this object
	 * @return The vector containing the net force of this object
	 */
	public Vec getNetForce(){
		return netForce;
	}
	
	/**
	 * Gets the mass of this object
	 * @return The mass of this object
	 */
	public double getMass(){
		return mass;
	}


	/**
	 * Constructs a MovableGameObject
	 * @param xx X Position of movable game object
	 * @param yy Y position of movable game object
	 * @param w Width of movable game object
	 * @param h Height of movable game object
	 * @param fwd Forward vector of movable game object
	 * @param m Mass of movable game object
	 */
	public MovableGameObject(double xx, double yy, double w, double h, Vec fwd, double m) {
		super(xx, yy, w, h, fwd);
		previousPosition = new Vec(2);
		
		netForce = new Vec(2);
		velocity = new Vec(2);
		mass = m;
	}
	
	/**
	 * Adds a force to the movable game object's net force
	 * @param forceInc Force to increment net force by
	 */
	public void addForce(Vec forceInc){
		netForce.add(forceInc);
	}
	
	/**
	 * Converts the netForce to acceleration
	 * Adds the acceleration to velocity
	 * Limits velocity
	 * Moves object by velocity
	 * Zero's net force
	 */
	public void forceMove(){
		velocity.add(Vec.scalarMultiply(netForce, 1.0/mass));
		if(velocity.getComponent(0) > MovableGameObject.maxSpeed) velocity.setComponent(0, MovableGameObject.maxSpeed);
		if(velocity.getComponent(0) < -MovableGameObject.maxSpeed) velocity.setComponent(0, -MovableGameObject.maxSpeed);

		//if(velocity.getComponent(1) > MovableGameObject.maxSpeed) velocity.setComponent(1, MovableGameObject.maxSpeed);
		//if(velocity.getComponent(1) < -MovableGameObject.maxSpeed) velocity.setComponent(1, -MovableGameObject.maxSpeed);

		move(velocity);
		netForce.scalarMultiply(0);
	}
	
	/**
	 * Moves the movable game object based on the net force in a specific axis
	 * Converts net force in axis to acceleration
	 * Adds acceleration to velocity in axis
	 * Limits velocity if axis is 0 (X axis)
	 * Moves object by velocity in axis
	 * Note: Net force is not zeroed after this method
	 * @param axis Axis to move on
	 */
	public void specifiedForceMove(int axis){
		//Increment velocity in the axis by the netforce in axis divided by the mass of the object
		velocity.incrementComponent(axis, netForce.getComponent(axis) * 1.0 / mass);
		
		//If moving in the X axis
		if(axis == 0){
			//Limit the velocity
			if(velocity.getComponent(axis) > MovableGameObject.maxSpeed) velocity.setComponent(axis, MovableGameObject.maxSpeed);
			if(velocity.getComponent(axis) < -MovableGameObject.maxSpeed) velocity.setComponent(axis, -MovableGameObject.maxSpeed);
		}
		
		//Move object in axis
		Vec axisVelocity = new Vec(2);
		axisVelocity.setComponent(axis, velocity.getComponent(axis));
		move(axisVelocity);
	}

	/**
	 * Updates previousPosition and increments position by the movementVector
	 * Also makes call to updateShape
	 * @param movementVec The vector to increment position by
	 */
	public void move(Vec movementVec){
		previousPosition.copy(position);
		position.add(movementVec);
		updateShape();
	}

	/**
	 * Reverts the position back to the previous position
	 * And makes call to updateShape
	 */
	public void revert(){
		position.copy(previousPosition);
		updateShape();
	}
	
	/**
	 * Reverts the position back to the previousPosition on a given axis
	 * Updates the object's shape
	 * @param axis The axis to revert
	 */
	public void revertAxis(int axis){
		position.setComponent(axis, previousPosition.getComponent(axis));
		
		updateShape();
	}
	
	/**
	 * Sets the previousosition to the currentPosition
	 */
	public void refresh(){
		previousPosition.copy(position);
	}
	
	/**
	 * Queries the collision manager for any collisions,
	 * And checks if any of the returned collisions is occurring on the floor
	 * @return Boolean indicating whether or not this movable game object is on the floor
	 */
	public boolean checkAllOnFloor(){
		boolean returnBool = false;
		
		//Save current velocity and position
		Vec savedVelocity = new Vec(2);
		savedVelocity.copy(velocity);
		
		//Move player in Y Axis
		specifiedForceMove(1);
		
		//Get list of collision buffers from collision manager
		ArrayList<CollisionBuffer> cBuffs = ((CollisionManager)Engine.currentInstance.getManager(Engine.Managers.COLLISIONMANAGER)).getCollisionsOnObject(this);
		
		//Loop through registered collisions to check if any are with a floor
		for(CollisionBuffer cBuff : cBuffs){
			//Make sure the collision occurred on the Y Axis
			//If not, skip checking this object
			if(cBuff.obj2CollidedSide.getComponent(1) != 0){
				continue;				
			}
			
			//Once found a collision on Y axis, check if the abs(colliding object's Y position - (this objects Y position + this object's height)) 
			//is no more than the Y velocity of this object
			//If this is true, the object is on the floor
			double difference = Math.abs(cBuff.obj2.position.getComponent(1) - (position.getComponent(1) + height));
			if(difference <= MovableGameObject.maxSpeed){
				returnBool = true;
			}

		}
		
		//Revert object to saved velocity and position
		velocity.copy(savedVelocity);
		position.copy(previousPosition);
		
		//If no collisions with the floor are found, return false
		return returnBool;
		
	}
	
	public boolean checkOnFloor(CollisionBuffer cBuff){
		
		boolean isOnFloor = false;
		
		if(cBuff.obj1 == this){
			//Make sure the collision occurred on the Y Axis
			//If not, skip checking this object
			if(cBuff.obj2CollidedSide.getComponent(1) != 0) return false;
			
			//Once found a collision on Y axis, check if the abs(colliding object's Y position - (this objects Y position + this object's height)) 
			//is no more than the Y velocity of this object
			//If this is true, the object is on the floor
			double difference = Math.abs(cBuff.obj2.position.getComponent(1) - (position.getComponent(1) + height));
			if(difference <= MovableGameObject.maxSpeed){
				isOnFloor = true;
			}
		}
		else{
			//Make sure the collision occurred on the Y Axis
			//If not, skip checking this object
			if(cBuff.obj1CollidedSide.getComponent(1) != 0) return false;
			
			//Once found a collision on Y axis, check if the abs(colliding object's Y position - (this objects Y position + this object's height)) 
			//is no more than the Y velocity of this object
			//If this is true, the object is on the floor
			double difference = Math.abs(cBuff.obj1.position.getComponent(1) - (position.getComponent(1) + height));
			if(difference <= MovableGameObject.maxSpeed){
				isOnFloor = true;
			}
		}

		
		return isOnFloor;
	}

}
