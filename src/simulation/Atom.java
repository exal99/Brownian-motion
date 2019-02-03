package simulation;
import processing.core.PApplet;
import processing.core.PVector;

public class Atom {
	
	private static int HASH;
	
	private int radius;
	private PVector startPos;
	private PVector vel;
	private int mass;
	private float refTime;
	
	private int hash;
	
	public Atom(PVector pos, PVector vel, int mass, int radius) {
		this.startPos = pos;
		this.vel = vel;
		this.mass = mass;
		this.radius = radius;
		refTime = 0;
		hash = HASH++;
	}
	
	
	private Atom(PVector pos, PVector vel, int mass, int radius, int hash, float refTime) {
		this.startPos = pos;
		this.vel = vel;
		this.mass = mass;
		this.radius = radius;
		this.refTime = refTime;
		this.hash = hash;
	}
	
	public PVector getVel() {
		return vel;
	}
	
	public int getMass() {
		return mass;
	}
	
	public PVector getPos(float time) {
		return PVector.add(startPos, PVector.mult(vel, time - refTime, null), null);
	}
	
	public int getRadius() {
		return radius;
	}
	
	public void setStartPos(PVector newPos) {
		startPos = newPos;
	}

	public float getColitionTime(Atom other) {
		float commonRefTime = Math.max(refTime, other.refTime);
		float diffTime = other.refTime - refTime;
		
		PVector diffPos = PVector.sub(startPos, other.startPos, null);
			
		diffPos.add(PVector.mult((diffTime > 0) ? vel : other.vel, diffTime, null));
		
		PVector diffVel = PVector.sub(vel, other.vel, null);
		float b = diffPos.dot(diffVel);
		float collisionDist = radius + other.radius;
		float c = diffPos.dot(diffPos) - collisionDist * collisionDist;
		float a = diffVel.dot(diffVel);
		
		float discriminant = b * b - a * c;
		
		if (discriminant > 0 && b < 0) {
			float sqrtDisc = PApplet.sqrt(discriminant);
			float colTime = (-b - sqrtDisc) / a;
			
			if (colTime >= 0)
				return commonRefTime + colTime;
		}
		return Float.POSITIVE_INFINITY;
	}
	
	public WallCollision getWallCollisionTime(int width, int height) {
		float xCollisionTime = Float.POSITIVE_INFINITY;
		float yCollisionTime = Float.POSITIVE_INFINITY;
		Wall xWall = null;
		Wall yWall = null;
		if (vel.x > 0) {
			xCollisionTime = (width - radius - startPos.x)/vel.x;
			xWall = Wall.RIGHT;
		} else if (vel.x < 0) {
			xCollisionTime = (radius - startPos.x)/vel.x;
			xWall = Wall.LEFT;
		}
	
		
		if (vel.y > 0) {
			yCollisionTime = (height - radius - startPos.y)/vel.y;
			yWall = Wall.BOTTOM;
		} else if (vel.y < 0) {
			yCollisionTime = (radius - startPos.y)/vel.y;
			yWall = Wall.TOP;
		}
		
		return new WallCollision(Math.min(xCollisionTime, yCollisionTime) + refTime,
								 (xCollisionTime < yCollisionTime) ? xWall : yWall);
	}
	
	public void collideAtom(float time, Atom other) {
		PVector diffPos = PVector.sub(other.startPos, startPos, null);
		diffPos.normalize();
		
		float u1 = vel.dot(diffPos);
		float u2 = other.vel.dot(diffPos);
		
		float v1 = (u1 * (mass - other.mass) + 2 * other.mass * u2)/(mass + other.mass);
		float v2 = (u2 * (other.mass - mass) + 2 * mass * u1) / (mass + other.mass);
		
		vel.add(PVector.mult(diffPos, (v1 - u1), null));
		other.vel.add(PVector.mult(diffPos, (v2 - u2), null));
		
		refTime = time;
		other.refTime = time;
	}
	
	public void collideWall(float time, Wall wall, int width, int height) {
		switch (wall) {
		case BOTTOM:
		case TOP:
			vel.y *= -1;
			break;	
		case RIGHT:
		case LEFT:
			vel.x *= -1;
			break;
		}
		
		refTime = time;
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Atom)
			return ((Atom) other).hash == hash;
		return false;
	}
	
	@Override
	public Atom clone() {
		return new Atom(startPos.copy(), vel.copy(), mass, radius, hash, refTime);
	}
	
	@Override
	public String toString() {
		return "" + hash;
	}
	
	public static class WallCollision {
		public float time;
		public Wall wall;
		
		public WallCollision(float time, Wall wall) {
			this.time = time;
			this.wall = wall;
		}
	}
	
	public enum Wall {
		TOP, BOTTOM, LEFT, RIGHT;
	}
}
