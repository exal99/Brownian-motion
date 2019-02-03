package simulation;

import simulation.Atom.Wall;
import simulation.Atom.WallCollision;

public class CollisionEvent implements Comparable<CollisionEvent>{
	
	public static CollisionEventType WALL_COLLISION = CollisionEventType.WALL_COLLISION;
	public static CollisionEventType PARTICLE_COLLISION = CollisionEventType.PARTICLE_COLLISION;
	
	private CollisionEventType type;
	private Atom atom1, atom2;
	private float time;
	private Wall wall;
	private boolean active;
	
	public CollisionEvent(Atom atom1, WallCollision collision) {
		type = CollisionEventType.WALL_COLLISION;
		this.atom1 = atom1;
		this.time = collision.time;
		this.wall = collision.wall;
		active = true;
	}
	
	public CollisionEvent(Atom atom1, Atom atom2, float time) {
		this.atom1 = atom1;
		this.atom2 = atom2;
		this.time = time;
		type = CollisionEventType.PARTICLE_COLLISION;
		active = true;
	}
	
	public void dissable() {
		active = false;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public Wall getWall() {
		return wall;
	}
	
	public CollisionEventType getType() {
		return type;
	}

	public Atom getAtom1() {
		return atom1;
	}

	public Atom getAtom2() {
		return atom2;
	}
	
	public float getTime() {
		return time;
	}

	@Override
	public int compareTo(CollisionEvent other) {
		return Float.compare(time, other.time);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof CollisionEvent) {
			CollisionEvent event = (CollisionEvent) other;
			if (type == CollisionEventType.WALL_COLLISION) {
				if (event.type == CollisionEventType.WALL_COLLISION)
					return atom1.equals(event.atom1);
				else
					return atom1.equals(event.atom1) || atom1.equals(event.atom2);
			} else {
				if (event.type == CollisionEventType.WALL_COLLISION)
					return event.atom1.equals(atom1) || event.atom1.equals(atom2);
				else
					return atom1.equals(event.atom1) || atom1.equals(event.atom2) ||
						   atom2.equals(event.atom1) || atom2.equals(event.atom2);
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		if (active) {
			return type.name();
		} else {
			return "INACTIVE";
		}
	}
		
	
	public enum CollisionEventType {
		WALL_COLLISION, PARTICLE_COLLISION;
	}
	

}
