package projectbanana.main.entity;

import java.util.ArrayList;

import projectbanana.main.CollisionEvent;
import projectbanana.main.Engine;
import projectbanana.main.World;
import projectbanana.main.values.EntityType;
import projectbanana.main.values.Geometry;
import projectbanana.main.values.Rotation;

public abstract class Entity implements VisibleObject {
	
	public static ArrayList<Entity> entities = new ArrayList<Entity>();
	
	private final Geometry GEOMETRY;
	private final EntityType TYPE;
	
	protected double x, y, width, height;
	protected double boundingRad;
	protected int boundingWidth, boundingHeight;
	
	protected int startX, startY;
	protected double lastValidX, lastValidY;
	
	protected double vel = 0, velX = 0, velY = 0, accX = 0, accY = 0;
	protected double rotVel = 0, rotAcc = 0;
	private double rotation = (3 * Math.PI) / 2; // Default rotation is facing up (North)
	protected final double MIN_VEL = 0.005, MIN_ROT_VEL = Math.toRadians(0.005); // Slowest speeds that are allowed before velocity goes to 0
	protected double velDamping = 0.985, rotVelDamping = 0.92;
	
	protected boolean isDone = false;
	private boolean collisionChecked = false;
	
	public Entity(int x, int y, Geometry geometry, EntityType type) {
		this.x = lastValidX = x;
		this.y = lastValidY = y;
		this.GEOMETRY = geometry;
		this.TYPE = type;
		
		// Adding this entity to the global list of entities
		entities.add(this);
	}
	
	protected void applyForces() {
		// Storing last valid location
		lastValidX = x;
		lastValidY = y;
		
		// X and Y positioning
		velX += accX;
		velY += accY;
		vel = Math.hypot(velX, velY);
		x += velX;
		y += velY;
		
		// Rotation positioning
		rotVel += rotAcc;
		rotation += rotVel;
		
		// Resetting force values
		accX = accY = rotAcc = 0;
	}
	
	public boolean isOnScreen() {
		// Don't need camera height since the camera is a square
		int cameraSize = Engine.cameraX2 - Engine.cameraX;
		// The amount of pixels shown from from the player until the camera ends
		int renderDistance = cameraSize / 2; 
		
		// Finding the center of the camera
		int cameraCenterY = Engine.cameraY + renderDistance;
		int cameraCenterX = Engine.cameraX + renderDistance;
		
		// The distances (X and Y) from the camera to the Entity
		double xDisFromCamera = Math.abs(cameraCenterX - getCenterX()) - (width / 2 + renderDistance);
		double yDisFromCamera = Math.abs(cameraCenterY - getCenterY()) - (height / 2 + renderDistance);
		
		if(xDisFromCamera < 0 && yDisFromCamera < 0) return true;
		return false;
	}
	
	public CollisionEvent isCollidingWith(Entity entity) {
		return isCollidingWith(entity, 0);
	}
	
	/**
	 * Checks if this Entity is colliding with another specified Entity, and also if
	 * it is colliding within a certain distance from that entity.
	 * 
	 * @param entity
	 * @param range
	 * @returns a CollisionEvent, giving the x and y distance from the entities,
	 * and if they were colliding (true of colliding, false if not).
	 */
	public CollisionEvent isCollidingWith(Entity entity, double range) {
		double xDis = getXDisFrom(entity);
		double yDis = getYDisFrom(entity);
		
		// If the Entities' shapes are different
		if(getGeometry() != entity.getGeometry()) {
			if(getGeometry() == Geometry.CIRCLE) return isCollidingWithCirRect(this, entity, range, xDis, yDis);
			else return isCollidingWithCirRect(entity, this, range, xDis, yDis);
		}
		// If both Entities are rectangles
		else if(getGeometry() == Geometry.RECTANGLE) {
			if((Math.abs(xDis) > (boundingWidth / 2 + entity.boundingWidth / 2 + range)) || (Math.abs(yDis) > (boundingHeight / 2 + entity.boundingHeight / 2 + range)))
				return new CollisionEvent(false, xDis, yDis);
			return new CollisionEvent(true, xDis, yDis);
		}
		// If both Entities are circles
		else {
			double radiusSqr = Math.pow(boundingRad + entity.boundingRad + range, 2);
			if(getSqrDisFrom(entity) <= radiusSqr) return new CollisionEvent(true, xDis, yDis);
			return new CollisionEvent(false, xDis, yDis);
		}
	}
	
	/**
	 * Checks whether the specified circle and rectangle Entities are colliding
	 * 
	 * @param circle the circle Entity
	 * @param rect the rectangle Entity
	 * @param xDis the x distance from the Entities
	 * @param yDis the y distance from the Entities
	 * @returns a CollisionEvent, giving the x and y distance from the entities,
	 * and if they were colliding (true of colliding, false if not).
	 */
	public CollisionEvent isCollidingWithCirRect(Entity circle, Entity rect, double range, double xDis, double yDis) {
		double xDisAbs = Math.abs(xDis);
		double yDisAbs = Math.abs(yDis);
		
		// Simple false cases
		if((xDisAbs > (rect.boundingWidth / 2 + circle.boundingRad + range)) || ((yDisAbs > (rect.boundingHeight / 2 + circle.boundingRad + range))))
			return new CollisionEvent(false, xDis, yDis);
		
		// Simple true cases
		if((xDisAbs <= rect.boundingWidth / 2 + range) || (yDisAbs <= rect.boundingHeight / 2 + range)) return new CollisionEvent(true, xDis, yDis);
		
		// Corner case
		double cornerDisSqr = (Math.pow(xDisAbs - ((rect.boundingWidth / 2) + range), 2) + Math.pow(yDisAbs - ((rect.boundingHeight / 2) + range), 2));
		if(cornerDisSqr <= Math.pow(circle.boundingRad + range, 2)) return new CollisionEvent(true, xDis, yDis);
		return new CollisionEvent(false, xDis, yDis);
	}
	
	protected void respawn() {
		x = startX;
		y = startY;
		vel = velX = velY = accX = accY = rotVel = rotAcc = 0;
		rotation = (3 * Math.PI) / 2; // Facing up (North)
	}
	
	protected void applyVelDamping() {
		applyVelDamping(velDamping);
	}
	
	protected void applyVelDamping(double dampAmnt) {
		if(vel == 0) return;
		
		// Velocity X & Y damping
		velX *= dampAmnt;
		velY *= dampAmnt;
		checkVel();
	}
	
	protected void applyRotVelDamping() {
		applyRotVelDamping(rotVelDamping);
	}
	
	protected void applyRotVelDamping(double dampAmnt) {
		if(rotVel == 0) return;
		
		rotVel *= dampAmnt;
		checkRotVel();
	}
	
	/**
	 * Accelerates the Entity forward at the required force until it is moving at the
	 * required speed, and then maintains that speed if continually
	 * called.
	 * 
	 * Note: force cannot be greater than speed, or an Exception will be thrown.
	 * 
	 * @param force
	 * @param maxVel
	 * @throws Exception
	 */
	protected void moveForward(double force, double maxVel) throws Exception {
		if(force > maxVel) throw new Exception("\"force\" (" + force + ") cannot be greater than \"max velocity\"(" + maxVel + ")");
		
		accForward(force);
		
		if(vel > maxVel) {
			// Capping off the velocity to the maximum velocity allowed
			velX = (velX / vel) * maxVel;
			velY = (velY / vel) * maxVel;
		}
	}
	
	/**
	 * Continually accelerates the Entity forward with the required force.
	 * 
	 * @param force
	 */
	protected void accForward(double force) {
		accX = Math.cos(rotation) * force; // X distance
		accY = Math.sin(rotation) * force; // Y distance
	}
	
	/**
	 * Checks whether the velocity is lower than MIN_VEL, if so, x
	 * and y velocity are set to 0.
	 */
	protected void checkVel() {
		if(vel < MIN_VEL) velX = velY = 0;
	}
	
	/**
	 * Checks whether the rotation velocity is lower than MIN_ROT_VEL, if so, rotation
	 * velocity is set to 0.
	 */
	protected void checkRotVel() {
		if(Math.abs(rotVel) < MIN_ROT_VEL) rotVel = 0;
	}
	
	/**
	 * Rotates the Entity with the specified force until it is moving at the
	 * specified speed in the specified direction, and then maintains that speed if continually
	 * called.
	 * 
	 * Note: force cannot be greater than speed, and dir can only be -1 (clockwise-clockwise) or 1 (counter), or and Exception 
	 * will be thrown.
	 * 
	 * @param dir
	 * @param maxVel
	 * @param force
	 * @throws Exception
	 */
	protected void turn(Rotation rot, double maxVel, double force) throws Exception {
		if(force > maxVel) throw new Exception("\"force\" cannot be greater than \"max velocity\"");
		//if(!rot.equals(Rotation.CLOCKWISE) && rot.equals(Rotation.COUNTER_CLOCKWISE)) throw new Exception("\"dir\" can only be 1 (clockwise) or -1 (counter-clockwise)");
		
		rotAcc = force * rot.getId();
		
		if(Math.abs(rotVel) > maxVel) {
			rotVel = maxVel * rot.getId();
			rotAcc = 0;
		}
	}
	
	protected void lookAt(int x, int y) {
		double xDis = getCenterX() - x;
		rotation = Math.atan((getCenterY() - y) / xDis);
		// If entity is to the right
		if(xDis > 0) rotation += Math.PI;
	}
	
	protected void lookAt(Entity entity) {
		double xDis = getXDisFrom(entity);
		rotation = Math.atan(getYDisFrom(entity) / xDis);
		// If entity is to the right
		if(xDis > 0) rotation += Math.PI;
	}
	
	protected double randomRotation() {
		return Math.random() * 2 * Math.PI;
	}
	
	public double getXDisFrom(Entity entity) {
		return (getCenterX() - entity.getCenterX());
	}
	
	public double getYDisFrom(Entity entity) {
		return (getCenterY() - entity.getCenterY());
	}
	
	public double getSqrDisFrom(Entity entity) {
		return (Math.pow(getXDisFrom(entity), 2) + Math.pow(getYDisFrom(entity), 2));
	}
	
	protected boolean inRange(Entity entity, double range) {
		return isCollidingWith(entity, range).isColliding();
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getCenterX() {
		return (x + (width / 2));
	}
	
	public double getCenterY() {
		return (y + (height / 2));
	}
	
	public Geometry getGeometry() {
		return GEOMETRY;
	}
	
	public double getRotation() {
		return rotation;
	}
	
	public void setRotation(double value) {
		rotation = value;
	}
	
	public boolean isCollisionChecked() {
		return collisionChecked;
	}
	
	public void setCollisionChecked(boolean value) {
		collisionChecked = value;
	}
	
	public double getWidth() {
		return width;
	}
	
	public double getHeight() {
		return height;
	}
	
	public double getBoundingWidth() {
		return boundingWidth;
	}
	
	public double getBoundingHeight() {
		return boundingHeight;
	}
	
	public double getBoundingRad() {
		return boundingRad;
	}
	
	public EntityType getType() {
		return TYPE;
	}
	
	public boolean isDone() {
		return isDone;
	}
	
	/**
	 * Called when the Entity is to be removed from the game. Use
	 * this to finish up anything else the Entity needs to do before it
	 * is trashed by the Garbage Collector.
	 */
	public void onDone() {}
	
	abstract public void handleCollision(Entity entity);
}
