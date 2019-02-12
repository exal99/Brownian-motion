package simulation;

import java.util.Hashtable;

import simulation.Atom.Wall;
import simulation.Atom.WallCollision;

public class CollisionEvent implements Comparable<CollisionEvent>{
	
	public static CollisionEventType WALL_COLLISION = CollisionEventType.WALL_COLLISION;
	public static CollisionEventType PARTICLE_COLLISION = CollisionEventType.PARTICLE_COLLISION;
	
	private CollisionEventType type;
	private Atom atom1, atom2;
	private float occuranceTime;
	private float createdTime;
	private Wall wall;
	
	public CollisionEvent(Atom atom1, WallCollision collision, float currentTime) {
		type = WALL_COLLISION;
		this.atom1 = atom1;
		this.occuranceTime = collision.time;
		this.wall = collision.wall;
		createdTime = currentTime;
	}
	
	public CollisionEvent(Atom atom1, Atom atom2, float occuranceTime, float currentTime) {
		this.atom1 = atom1;
		this.atom2 = atom2;
		this.occuranceTime = occuranceTime;
		createdTime = currentTime;
		type = PARTICLE_COLLISION;
	}
	
	public boolean isActive(Hashtable<Atom, Float> lastUpdate) {
		switch(type) {
		case WALL_COLLISION:
			return createdTime >= lastUpdate.get(atom1);
		case PARTICLE_COLLISION:
			return createdTime >= lastUpdate.get(atom1) && createdTime > lastUpdate.get(atom2);
		default:
			throw new Error("ERROR!");
		}
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
		return occuranceTime;
	}

	@Override
	public int compareTo(CollisionEvent other) {
		return Float.compare(occuranceTime, other.occuranceTime);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof CollisionEvent) {
			CollisionEvent event = (CollisionEvent) other;
			if (type == WALL_COLLISION) {
				if (event.type == WALL_COLLISION)
					return atom1.equals(event.atom1);
				else
					return atom1.equals(event.atom1) || atom1.equals(event.atom2);
			} else {
				if (event.type == WALL_COLLISION)
					return event.atom1.equals(atom1) || event.atom1.equals(atom2);
				else
					return atom1.equals(event.atom1) || atom1.equals(event.atom2) ||
						   atom2.equals(event.atom1) || atom2.equals(event.atom2);
			}
		}
		return false;
	}
	
	public boolean contains(Atom atom) {
		return (type == WALL_COLLISION) ? atom.equals(atom1) : atom.equals(atom1) || atom.equals(atom2);
	}
	
	@Override
	public String toString() {
		return type.name();
	}
		
	
	public enum CollisionEventType {
		WALL_COLLISION, PARTICLE_COLLISION;
	}
	

}
