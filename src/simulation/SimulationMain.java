package simulation;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JOptionPane;

import processing.awt.PSurfaceAWT;
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
	
	private Hashtable<Atom, Integer> selected;
	private ArrayList<LinkedList<PVector>> selectedTrails;
	
	private Hashtable<Atom, Integer> indexMapping;
	
	
	@Override
	public void settings() {
		size(800,600);

	}
	
	private MenuBar createMenuBar() {
		MenuBar mb = new MenuBar();
		Menu simulation = new Menu("Simulation");
		MenuItem quit = new MenuItem("Quit", new MenuShortcut(KeyEvent.VK_Q));
		MenuItem pause = new MenuItem("Pause", new MenuShortcut(KeyEvent.VK_P));
		MenuItem reset = new MenuItem("Reset", new MenuShortcut(KeyEvent.VK_SPACE));
		
		quit.addActionListener((ActionEvent e) -> {simThread.interrupt(); exit();});
		pause.addActionListener((ActionEvent e) -> {paused = !paused;});
		reset.addActionListener((ActionEvent e) -> reset()); 
		
		simulation.add(quit);
		simulation.add(pause);
		simulation.add(reset);
		mb.add(simulation);
		
		Menu setting = new Menu("Settings");
		Menu globalSettings = new Menu("Global Settings");
		Menu particleSettings = new Menu("Particle Settings");	
		
		MenuItem setDimentions = new MenuItem("Window size", new MenuShortcut(KeyEvent.VK_D));
		setDimentions.setActionCommand("dim");
		
		MenuItem trailLimit = new MenuItem("Trail length", new MenuShortcut(KeyEvent.VK_L));
		trailLimit.setActionCommand("trail");
		
		MenuItem resetValues = new MenuItem("Reset Settings", new MenuShortcut(KeyEvent.VK_Z));
		resetValues.addActionListener((ActionEvent e) -> {Constants.RESET(); reset();});
		
		MenuItemListener listener = new MenuItemListener();
		setDimentions.addActionListener(listener);
		trailLimit.addActionListener(listener);
		
		globalSettings.add(setDimentions);
		globalSettings.add(trailLimit);
		globalSettings.add(resetValues);
		
		Menu radius = new Menu("Set radisus");
		Menu speed = new Menu("Maximum starting speed");
		Menu natoms = new Menu("Number of particles");
		Menu mass = new Menu("Mass");
		
		MenuItem[] rad = getMenuItems("radius", "rad", KeyEvent.VK_R, listener);
		MenuItem[] vel = getMenuItems("speed", "vel", KeyEvent.VK_S, listener);
		MenuItem[] atoms = getMenuItems("atoms", "natoms", KeyEvent.VK_N, listener);
		MenuItem[] m = getMenuItems("mass", "mass", KeyEvent.VK_M, listener);
		
		radius.add(rad[0]);
		radius.add(rad[1]);
		speed.add(vel[0]);
		speed.add(vel[1]);
		natoms.add(atoms[0]);
		natoms.add(atoms[1]);
		mass.add(m[0]);
		mass.add(m[1]);
		
		particleSettings.add(radius);
		particleSettings.add(speed);
		particleSettings.add(natoms);
		particleSettings.add(mass);
		
		setting.add(globalSettings);
		setting.add(particleSettings);
		mb.add(setting);
		
		return mb;
	}
	
	private MenuItem[] getMenuItems(String text, String command, int shortcut, ActionListener a) {
		MenuItem[] items = new MenuItem[2];
		items[0] = new MenuItem("Small " + text, new MenuShortcut(shortcut));
		items[0].setActionCommand("s_" + command);
		items[0].addActionListener(a);
		items[1] = new MenuItem("Large " + text, new MenuShortcut(shortcut, true));
		items[1].setActionCommand("l_" + command);
		items[1].addActionListener(a);
		return items;
	}
	
	private Frame getFrame() {
		PSurfaceAWT awtSurface = (PSurfaceAWT) surface;
		PSurfaceAWT.SmoothCanvas smoothCanvas = (PSurfaceAWT.SmoothCanvas)awtSurface.getNative();
		return smoothCanvas.getFrame();
	}
	
	@Override
	public void setup() {
		frameRate(60);
		surface.setTitle("Brownian Motion Simulation");
		getFrame().setMenuBar(createMenuBar());
		
		surface.setResizable(true);
		snapshotQueue = new ArrayBlockingQueue<SimulationDiffrence>(Constants.GET_QUEUE_LIMIT());
		simThread = new SimulationThread(snapshotQueue, Constants.NUMBER_OF_LIGHT_ATOMS, Constants.NUMBER_OF_HEAVY_ATOMS, width, height, this);
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		atoms = simThread.getAtoms();
			
		paused = false;
		time = 0;
		
		createIndexMapping();
		selected = new Hashtable<Atom, Integer>();
		selectedTrails = new ArrayList<LinkedList<PVector>>();
		
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
		selectedTrails = new ArrayList<LinkedList<PVector>>();
		
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
		
		for (LinkedList<PVector> trail : selectedTrails) {
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
				if (!paused)
					selectedTrails.get(selected.get(atom)).add(pos);
				while (Constants.TRAIL_MAX_LENGTH > 0 && selectedTrails.get(selected.get(atom)).size() > Constants.TRAIL_MAX_LENGTH) {
					selectedTrails.get(selected.get(atom)).pop();
				}
			} else 
				fill(255,255,255);			
			stroke(0);
			strokeWeight(1);
			ellipse(pos.x, pos.y, atom.getRadius() * 2, atom.getRadius() * 2);
		}
		
		
		fill(255, 0, 0);
		textSize(30);
		text("Total Kinetic Energy: " + Math.round(calcKeneticEnergy() * 100.0f) / 100.0f, 30, height - 40);
		fill(0,246,255);
		text("FPS: " + Math.round(frameRate), width-150, 40);
		
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
					selectedTrails.add(new LinkedList<PVector>());
			}
		}
	}
	
	class MenuItemListener implements ActionListener {
		
		private void makeInputDialog(String title, String text, String startValue, InputHandeler i) {
			String s = (String) JOptionPane.showInputDialog(getFrame(), text, title,
						JOptionPane.PLAIN_MESSAGE, null, null, startValue);
			if (s != null)
				i.handleInput(s);
		}
		
		private int parseInt(String integer) {
			try {
				return Integer.parseInt(integer);
			} catch (NumberFormatException e) {
				return -1;
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			case "dim":
				makeInputDialog("New window size", "Type a new width and hight of the window.\n"
						+ "Seperate the two with a comma", width + "," + height, (String s) -> {
							String[] dim = s.split(",");
							if (dim.length == 2) {
								int w = parseInt(dim[0]);
								int h = parseInt(dim[1]);
								if (w != -1 && h != -1) {
									surface.setSize(w, h);
									reset();
								}
							}
						});
				break;
			case "trail":
				makeInputDialog("New maximum trail length", "Type a new maximum length for the selected trails.\n"
						+ "A value of 0 means no limit.", "" + Constants.TRAIL_MAX_LENGTH,
						(String s) -> Constants.TRAIL_MAX_LENGTH = (parseInt(s) != -1) ? parseInt(s) : Constants.TRAIL_MAX_LENGTH);
				break;
			case "s_rad":
				makeInputDialog("New small radius", "Type a new radius for the small particles.",
						"" + Constants.LIGHT_ATOM_RADIUS,
						(String s) -> {Constants.LIGHT_ATOM_RADIUS = (parseInt(s) > 0) ? parseInt(s) : Constants.LIGHT_ATOM_RADIUS;
									   reset();});
				break;
			case "l_rad":
				makeInputDialog("New large radius", "Type a new radius for the large particles.",
						"" + Constants.HEAVY_ATOM_RADIUS,
						(String s) -> {Constants.HEAVY_ATOM_RADIUS = (parseInt(s) > 0) ? parseInt(s) : Constants.HEAVY_ATOM_RADIUS;
									   reset();});
				break;
			case "s_vel":
				makeInputDialog("New small particle maximum starting velocity", "Type a new maximum starting velocity for the small particles.",
						"" + Math.round(Constants.LIGHT_MAX_START_VEL * 1000),
						(String s) -> {Constants.LIGHT_MAX_START_VEL = (parseInt(s) > 0) ? parseInt(s) / 1000.0f: Constants.LIGHT_MAX_START_VEL;
									   reset();});
				break;
			case "l_vel":
				makeInputDialog("New large particle maximum starting velocity", "Type a new maximum starting velocity for the large particles.",
						"" + Math.round(Constants.HEAVY_MAX_START_VEL * 1000),
						(String s) -> {Constants.HEAVY_MAX_START_VEL = (parseInt(s) > 0) ? parseInt(s) / 1000.0f : Constants.HEAVY_MAX_START_VEL;
									   reset();});
				break;
			case "s_natoms":
				makeInputDialog("Number of small particles", "Type a new number of small particles.",
						"" + Constants.NUMBER_OF_LIGHT_ATOMS,
						(String s) -> {Constants.NUMBER_OF_LIGHT_ATOMS = (parseInt(s) > 0) ? parseInt(s) : Constants.NUMBER_OF_LIGHT_ATOMS;
									   reset();});
				break;
			case "l_natoms":
				makeInputDialog("Number of large particles", "Type a new number of large particles.",
						"" + Constants.HEAVY_ATOM_RADIUS,
						(String s) -> {Constants.HEAVY_ATOM_RADIUS = (parseInt(s) > 0) ? parseInt(s) : Constants.HEAVY_ATOM_RADIUS;
									   reset();});
				break;
			case "s_mass":
				makeInputDialog("New small particle mass", "Type a new 'mass' for the small particles.",
						"" + Constants.LIGHT_MASS,
						(String s) -> {Constants.LIGHT_MASS = (parseInt(s) > 0) ? parseInt(s) : Constants.LIGHT_MASS;
									   reset();});
				break;
			case "l_mass":
				makeInputDialog("New large particle mass", "Type a new 'mass' for the large particles.",
						"" + Constants.HEAVY_MASS,
						(String s) -> {Constants.HEAVY_MASS = (parseInt(s) > 0) ? parseInt(s) : Constants.HEAVY_MASS;
									   reset();});
				break;
			}
		}
		
		
		
	}
	
	private interface InputHandeler {
		public void handleInput(String input);
	}

	public static void main(String[] args) {
		PApplet.main("simulation.SimulationMain");
	}

}
