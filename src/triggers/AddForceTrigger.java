package triggers;

import mathematics.Vec;
import objects.GameObject;
import objects.MovableGameObject;
import buffer.CollisionBuffer;

/**
 * Trigger that adds a force to a movable game object 
 * @author Alan Leeson
 *
 */

public class AddForceTrigger extends Trigger{

	Vec force;
	
	public AddForceTrigger(Vec force){
		this.force = force;
	}
	
	@Override
	public void action(GameObject triggeredBy, CollisionBuffer cBuff) {
		// TODO Auto-generated method stub
		if(triggeredBy instanceof MovableGameObject){
			((MovableGameObject) triggeredBy).addForce(force);
		}
	}

}
