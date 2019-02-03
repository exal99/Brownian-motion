package simulation;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import processing.core.PApplet;
import processing.core.PVector;
import simulation.Simulation.SimulationDiffrence;

public class SimulationMain extends PApplet {
	
	private BlockingQueue<SimulationDiffrence> snapshotQueue;
	private SimulationThread simThread;
	private Atom[] atoms;
	private SimulationDiffrence nextUpdate;
	private boolean paused;
	private int time;
	
	private int numWritten;
	private int newWidth;
	private int newHeight;
	
	private Hashtable<Atom, Integer> selected;
	private ArrayList<ArrayList<PVector>> selectedTrails;
	
	private Hashtable<Atom, Integer> indexMapping;
	
	
	@Override
	public void settings() {
		size(1600,1200);

	}
	
	@Override
	public synchronized void setup() {
		surface.setResizable(true);
		snapshotQueue = new ArrayBlockingQueue<SimulationDiffrence>(Constants.GET_QUEUE_LIMIT());
		simThread = new SimulationThread(snapshotQueue, Constants.NUMBER_OF_LIGHT_ATOMS, Constants.NUMBER_OF_HEAVY_ATOMS, width, height, this);
		
		atoms = simThread.getAtoms();
			
		paused = false;
		time = 0;
		
		createIndexMapping();
		selected = new Hashtable<Atom, Integer>();
		selectedTrails = new ArrayList<ArrayList<PVector>>();
		
		simThread.start();
		waitForLoad();
		
		try {
			nextUpdate = snapshotQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void createIndexMapping() {
		indexMapping = new Hashtable<Atom, Integer>();
		for (int i = 0; i < atoms.length; i++) {
			indexMapping.put(atoms[i], i);
		}
		return;
	}
	
	private void reset() {
		simThread.terminate();
		simThread.interrupt();
		
		snapshotQueue = new ArrayBlockingQueue<SimulationDiffrence>(Constants.GET_QUEUE_LIMIT());
		simThread = new SimulationThread(snapshotQueue, Constants.NUMBER_OF_LIGHT_ATOMS, Constants.NUMBER_OF_HEAVY_ATOMS, width, height, this);
		
		atoms = simThread.getAtoms();
		
		time = 0;
		
		createIndexMapping();
		selected = new Hashtable<Atom, Integer>();
		selectedTrails = new ArrayList<ArrayList<PVector>>();
		
		simThread.start();
		waitForLoad();
		
		try {
			nextUpdate = snapshotQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	private void waitForLoad() {
		System.out.print("Loading... ");
		int start = millis();
		while (!simThread.doneLoading()) {
			synchronized(SimulationThread.LOCK) {
				try {
					SimulationThread.LOCK.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("Done! [" + (millis() - start) + " ms]");
	}
	
	private float calcKeneticEnergy() {
		float energy = 0;
		for (Atom atom : atoms) {
			energy += (atom.getMass() * atom.getVel().magSq())/2;
		}
		return energy;
	}
	
	@SuppressWarnings("unused")
	private PVector calcTotalMomentum() {
		PVector tot = new PVector(0,0);
		for (Atom atom : atoms) {
			tot.add(PVector.mult(atom.getVel(), atom.getMass(), null));
		}
		return tot;
	}
	
	@Override
	public void draw() {
		background(51);
		
		
		if (!paused) {
			time += Constants.SIMULATION_TIME_STEPS;
		}
		
		for (ArrayList<PVector> trail : selectedTrails) {
			for (PVector pos : trail) {
				stroke(0, 255, 0);
				strokeWeight(3);
				point(pos.x, pos.y);
			}
		}		
			
		for (Atom atom : atoms) {
			PVector pos = atom.getPos(time);
			if (selected.containsKey(atom)) {
				fill(244, 200, 66);
				selectedTrails.get(selected.get(atom)).add(pos);
			} else 
				fill(255,255,255);			
			stroke(0);
			strokeWeight(1);
			ellipse(pos.x, pos.y, atom.getRadius() * 2, atom.getRadius() * 2);
		}
		
		if (numWritten != 0) {
			textSize(50);
			fill(0, 255, 0);
			text(numWritten, 30, 60);
		}
		
		fill(255, 0, 0);
		textSize(30);
		text("Total Kinetic Energy: " + Math.round(calcKeneticEnergy() * 100.0f) / 100.0f, 30, height - 40);
		
		while (time > nextUpdate.time) {
			updateAtoms(nextUpdate.atoms);
			try {
				nextUpdate = snapshotQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void updateAtoms(Atom[] atomsToUpdate) {
		for(Atom atom : atomsToUpdate) {
			atoms[indexMapping.get(atom)] = atom;
		}
	}
	
	@Override
	public void mousePressed() {
		PVector mousePos = new PVector(mouseX, mouseY);
		for (Atom atom : atoms) {
			if (PVector.sub(mousePos, atom.getPos(time), null).magSq() < atom.getRadius() * atom.getRadius()) {
				if (selected.containsKey(atom)) {
					int index = selected.remove(atom);
					selectedTrails.remove(index);
				} else
					selected.put(atom, selectedTrails.size());
					selectedTrails.add(new ArrayList<PVector>());
			}
		}
	}
	
	@Override
	public void keyPressed() {
		if (key == 'q') {
			simThread.terminate();
			exit();
		} if (key == 'p') {
			paused = !paused;
		} if (key == ' ') {
			reset();
		}
		
		if ('0' <= key && key <= '9') {
			numWritten *= 10;
			numWritten += key - '0';
		}
		
		if (key == 'n') {
			if (numWritten != 0) {
				Constants.NUMBER_OF_LIGHT_ATOMS = numWritten;
				numWritten = 0;
				reset();
			}
		}
		
		if (key == 'N') {
			if (numWritten != 0) {
				Constants.NUMBER_OF_HEAVY_ATOMS = numWritten;
				numWritten = 0;
				reset();
			}
		}
		
		if (key == 'W') {
			if (numWritten != 0 && newHeight != 0) {
				surface.setSize(numWritten, newHeight);
				reset();
			} else if (numWritten != 0) {
				newWidth = numWritten;
			}
			
			if (numWritten != 0) {
				numWritten = 0;
				newHeight = 0;
			}
		} 
		
		if (keyCode == BACKSPACE) {
			numWritten /= 10;
		}
		
		if (key == 's') {
			if (numWritten != 0) 
				Constants.LIGHT_MAX_START_VEL = numWritten / 1000.0f;
			numWritten = 0;	
		}
		
		if (key == 'S') {
			if (numWritten != 0) 
				Constants.HEAVY_MAX_START_VEL = numWritten / 1000.0f;
			numWritten = 0;	
		}
		
		if (key == 'H') {
			if (numWritten != 0 && newWidth != 0) {
				surface.setSize(newWidth, numWritten);
				reset();
			} else if (numWritten != 0) {
				newHeight = numWritten;
			}
			if (numWritten != 0) {
				numWritten = 0;
				newWidth = 0;
			}
		}
		
		if (key == 'r') {
			if (numWritten != 0) {
				Constants.LIGHT_ATOM_RADIUS = numWritten;
				reset();
			}
			numWritten = 0;
		}
		
		if (key == 'R') {
			if (numWritten != 0) {
				Constants.HEAVY_ATOM_RADIUS = numWritten;
				reset();
			}
			numWritten = 0;
		}
		
		if (key == 't') {
			if (numWritten != 0) 
				Constants.SIMULATION_TIME_STEPS = numWritten;
			numWritten = 0;
		}
		
		if (key == 'z') {
			Constants.RESET();
			reset();
		}
		
		if (key == 'm') {
			if (numWritten != 0) {
				Constants.LIGHT_MASS = numWritten;
				reset();
			}
			numWritten = 0;
		}
		
		if (key == 'M') {
			if (numWritten != 0) {
				Constants.HEAVY_MASS = numWritten;
				reset();
			}
			numWritten = 0;
		}
		
		if (keyCode == KeyEvent.VK_F1) {
			printHelp();
		}
		
	}
	
	private void printHelp() {
		System.out.println("\nAll available key bindings:\n");
		System.out.println("Global simulation settings");
		System.out.println("------------------------------------------------------");
		System.out.println("F1\t\tPrints this help message");
		System.out.println("W, H\t\tSets the width and hight of the window");
		System.out.println("q\t\tQuits the simulation");
		System.out.println("p\t\tPuses the simulation");
		System.out.println("SPACE\t\tResets the simulation");
		System.out.println("z\t\tResets all values to their default value");
		System.out.println("t\t\tSets the time increment on each frame");
		
		System.out.println("\nSpecific particle settings");
		System.out.println("------------------------------------------------------");
		System.out.println("Lowercase bindings are allways for light particles\nand uppercase are for heavy\n");
		System.out.println("r, R\t\tSets the radius of the particles");
		System.out.println("s, S\t\tSets the starting maximu speed of the particles");
		System.out.println("n, N\t\tSets the number of particles in the simulation");
		System.out.println("m, M\t\tSets the mass of the particles");
		
		System.out.flush();
	}
	
	

	public static void main(String[] args) {
		PApplet.main("simulation.SimulationMain");
	}
	
	public class Test extends Thread {
		public Test() {
			super("Test thread");
		}
		
		@Override
		public void run() {
			synchronized(SimulationThread.LOCK) {
				while (true) {
				
					System.out.println("Running");
					try {
						SimulationThread.LOCK.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

}
