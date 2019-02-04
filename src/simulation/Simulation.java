package simulation;
import java.util.PriorityQueue;

import processing.core.PApplet;
import processing.core.PVector;

public class Simulation {
	
	private PriorityQueue<CollisionEvent> events;
	private Atom[] atoms;
	private int width, height;
	
	public Simulation(int nLightAtoms, int nHeavyAtoms, int width, int height, PApplet applet) {
		reset(nLightAtoms, nHeavyAtoms, width, height, applet);
	}
	
	public void reset(int nLightAtoms, int nHeavyAtoms, int width, int height, PApplet applet) {
		this.width = width;
		this.height = height;
		events = new PriorityQueue<CollisionEvent>();
		createAtoms(nLightAtoms, nHeavyAtoms, applet);
		createEvents();
	}
	
	private void createAtoms(int nLightAtoms, int nHeavyAtoms, PApplet applet) {
		atoms = new Atom[nLightAtoms + nHeavyAtoms];
		for (int i = 0; i < nLightAtoms; i++) {
			PVector vel = PVector.random2D();
			vel.setMag(applet.random(Constants.LIGHT_MAX_START_VEL * 1.5f));
			atoms[i] = new Atom(new PVector(width/2, height/2), vel, Constants.LIGHT_MASS, Constants.LIGHT_ATOM_RADIUS);
		}
		
		for (int i = nLightAtoms; i < nLightAtoms + nHeavyAtoms; i++) {
			PVector vel = PVector.random2D();
			vel.setMag(applet.random(Constants.HEAVY_MAX_START_VEL));
			atoms[i] = new Atom(new PVector(width/2, height/2), vel, Constants.HEAVY_MASS, Constants.HEAVY_ATOM_RADIUS);
		}
		return;
	}
	
	private void createEvents() {
		for (int i = 0; i < atoms.length; i++) {
			Atom atom = atoms[i];
			events.add(new CollisionEvent(atom, atom.getWallCollisionTime(width, height)));
			
			for (int j = i + 1; j < atoms.length; j++) {
				Atom collisionAtom = atoms[j];
				float time = atom.getColitionTime(collisionAtom);
				if (time != Float.POSITIVE_INFINITY)
					events.add(new CollisionEvent(atom, collisionAtom, time));
			}
			
		}
	}
	
	private CollisionEvent getNextEvent() {
		CollisionEvent event = events.poll();
		
		while (!event.isActive()) {
			event = events.poll();
		}
		
		return event;
	}
	
	private void dissableEvents(CollisionEvent event) {
		for (CollisionEvent e : events) {
			if (e.equals(event))
				e.dissable();
		}
	}
	
	public SimulationDiffrence advanceSimulation() {
		CollisionEvent nextEvent = getNextEvent();
		
		Atom[] eventAtoms;
		
		switch (nextEvent.getType()) {
		case PARTICLE_COLLISION:
			Atom atom1 = nextEvent.getAtom1();
			Atom atom2 = nextEvent.getAtom2();
			atom1.setStartPos(atom1.getPos(nextEvent.getTime()));
			atom2.setStartPos(atom2.getPos(nextEvent.getTime()));
			atom1.collideAtom(nextEvent.getTime(), atom2);
			eventAtoms = new Atom[] {atom1, atom2};
			break;
		case WALL_COLLISION:
			Atom atom = nextEvent.getAtom1();
			atom.setStartPos(atom.getPos(nextEvent.getTime()));
			atom.collideWall(nextEvent.getTime(), nextEvent.getWall(), width, height);
			eventAtoms = new Atom[] {atom};
			break;
		default:
			eventAtoms = new Atom[] {};
		}
		
		dissableEvents(nextEvent);
		
		for (Atom a: eventAtoms) {
			events.add(new CollisionEvent(a, a.getWallCollisionTime(width, height)));
			for (Atom other : atoms) {
				if (other != a) {
					float collisionTime = a.getColitionTime(other);
					if (collisionTime != Float.POSITIVE_INFINITY) 
						events.add(new CollisionEvent(a, other, collisionTime));
				}
			}
		}
		
		return new SimulationDiffrence(nextEvent.getTime(), copyAtoms(eventAtoms));
	}
	
	private Atom[] copyAtoms(Atom[] atoms) {
		Atom[] res = new Atom[atoms.length];
		for (int i = 0; i < atoms.length; i++) {
			res[i] = atoms[i].clone();
		}
		return res;
	}
	
	public Atom[] getAtomsClone() {
		Atom[] clones = new Atom[atoms.length];
		for (int i = 0; i < atoms.length; i++) {
			clones[i] = atoms[i].clone();
		}
		
		return clones;
	}
	
	public static class SimulationDiffrence {
		public float time;
		public Atom[] atoms;
		
		public SimulationDiffrence(float time, Atom[] atoms) {
			this.time = time;
			this.atoms = atoms;
		}
	}

}
