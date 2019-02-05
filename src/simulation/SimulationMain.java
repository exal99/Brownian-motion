package simulation;
import java.awt.CheckboxMenuItem;
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
import java.util.function.Consumer;
import java.util.function.Predicate;

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
	
	private float fpsCap;
	private CheckboxMenuItem hideSmall;
	
	
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
		
		quit.addActionListener((ActionEvent e) -> {simThread.terminate(); exit();});
		pause.addActionListener((ActionEvent e) -> paused = !paused);
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
		
		MenuItem frameRate = new MenuItem("Frame rate", new MenuShortcut(KeyEvent.VK_F));
		frameRate.setActionCommand("fps");
		
		MenuItem stepTime = new MenuItem("Step time", new MenuShortcut(KeyEvent.VK_T));
		stepTime.setActionCommand("step");
		
		hideSmall = new CheckboxMenuItem("Hide small particles", false);
		
		MenuItem resetValues = new MenuItem("Reset Settings", new MenuShortcut(KeyEvent.VK_Z));
		resetValues.addActionListener((ActionEvent e) -> {Constants.RESET(); reset();});
		
		
		MenuItemListener listener = new MenuItemListener();
		setDimentions.addActionListener(listener);
		trailLimit.addActionListener(listener);
		frameRate.addActionListener(listener);
		stepTime.addActionListener(listener);
		
		globalSettings.add(setDimentions);
		globalSettings.add(trailLimit);
		globalSettings.add(frameRate);
		globalSettings.add(stepTime);
		globalSettings.add(hideSmall);
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
		fpsCap = 60;
		frameRate(fpsCap);
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
	
	private synchronized void reset() {
		snapshotQueue = new ArrayBlockingQueue<SimulationDiffrence>(Constants.GET_QUEUE_LIMIT());
		atoms = simThread.reset(snapshotQueue, Constants.NUMBER_OF_LIGHT_ATOMS,
				Constants.NUMBER_OF_HEAVY_ATOMS, width, height, this);

		time = 0;
		
		createIndexMapping();
		selected = new Hashtable<Atom, Integer>();
		selectedTrails = new ArrayList<LinkedList<PVector>>();
		
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
					SimulationThread.LOCK.wait(3000);
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
	public synchronized void draw() {
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
			
		for (int i = (!hideSmall.getState()) ? 0 : Constants.NUMBER_OF_LIGHT_ATOMS;
		     i < atoms.length; i++) {
			Atom atom = atoms[i];
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
		text("Total Kinetic Energy: " + round(calcKeneticEnergy() * 100.0f) / 100.0f, 30, height - 40);
		fill(0,246,255);
		text("FPS: " + round(frameRate), width-150, 40);
		
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
	
	@Override
	public void frameRate(float fps) {
		fpsCap = fps;
		super.frameRate(fps);
	}
	
	class MenuItemListener implements ActionListener {
		
		private void makeInputDialog(String title, String text, String startValue, Predicate<String> condition, Consumer<String> consumer, boolean reset) {
			String s = (String) JOptionPane.showInputDialog(getFrame(), text, title,
						JOptionPane.PLAIN_MESSAGE, null, null, startValue);
			if (s != null && condition.test(s)) {
				consumer.accept(s);
				if (reset)
					reset();
			}
		}
		
		private void makeInputDialog(String title, String text, String startValue, Predicate<String> condition, Consumer<String> consumer) {
			makeInputDialog(title, text, startValue, condition, consumer, true);
		}
		
		private int parseInt(String integer) {
			try {
				return Integer.parseInt(integer);
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		
		private boolean dimentionsCondition(String string) {
			return string.split(",").length == 2 && parseInt(string.split(",")[0]) > 0 && parseInt(string.split(",")[1]) > 0 ;
		}
		
		private void updateDimensions(String string) {
			String[] dim = string.split(",");
			surface.setSize(parseInt(dim[0]), parseInt(dim[1]));
		}
		

		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			case "dim":
				makeInputDialog("New window size", "Type a new width and hight of the window.\n"
						+ "Seperate the two with a comma", width + "," + height,
						this::dimentionsCondition, this::updateDimensions);
				break;
			case "trail":
				makeInputDialog("New maximum trail length", "Type a new maximum length for the selected trails.\n"
						+ "A value of 0 means no limit.", "" + Constants.TRAIL_MAX_LENGTH,
						(String s) -> parseInt(s) > -1,
						(String s) -> Constants.TRAIL_MAX_LENGTH = parseInt(s), false);
				break;
			case "s_rad":
				makeInputDialog("New small radius", "Type a new radius for the small particles.",
						"" + Constants.LIGHT_ATOM_RADIUS,
						(String s) -> parseInt(s) > 0,
						(String s) -> Constants.LIGHT_ATOM_RADIUS = parseInt(s));
				break;
			case "l_rad":
				makeInputDialog("New large radius", "Type a new radius for the large particles.",
						"" + Constants.HEAVY_ATOM_RADIUS,
						(String s) -> parseInt(s) > 0,
						(String s) -> Constants.HEAVY_ATOM_RADIUS = parseInt(s));
				break;
			case "s_vel":
				makeInputDialog("New small particle maximum starting velocity", "Type a new maximum starting velocity for the small particles.",
						"" + round(Constants.LIGHT_MAX_START_VEL * 1000),
						(String s) -> parseInt(s) > 0,
						(String s) -> Constants.LIGHT_MAX_START_VEL = parseInt(s) / 1000.0f);
				break;
			case "l_vel":
				makeInputDialog("New large particle maximum starting velocity", "Type a new maximum starting velocity for the large particles.",
						"" + round(Constants.HEAVY_MAX_START_VEL * 1000),
						(String s) -> parseInt(s) > 0,
						(String s) -> Constants.HEAVY_MAX_START_VEL = parseInt(s) / 1000.0f);
				break;
			case "s_natoms":
				makeInputDialog("Number of small particles", "Type a new number of small particles.",
						"" + Constants.NUMBER_OF_LIGHT_ATOMS,
						(String s) -> parseInt(s) > 0,
						(String s) -> Constants.NUMBER_OF_LIGHT_ATOMS = parseInt(s));
				break;
			case "l_natoms":
				makeInputDialog("Number of large particles", "Type a new number of large particles.",
						"" + Constants.HEAVY_ATOM_RADIUS,
						(String s) -> parseInt(s) > 0,
						(String s) -> Constants.HEAVY_ATOM_RADIUS = parseInt(s));
				break;
			case "s_mass":
				makeInputDialog("New small particle mass", "Type a new 'mass' for the small particles.",
						"" + Constants.LIGHT_MASS,
						(String s) -> parseInt(s) > 0,
						(String s) -> Constants.LIGHT_MASS = parseInt(s));
				break;
			case "l_mass":
				makeInputDialog("New large particle mass", "Type a new 'mass' for the large particles.",
						"" + Constants.HEAVY_MASS,
						(String s) -> parseInt(s) > 0,
						(String s) -> Constants.HEAVY_MASS = parseInt(s));
				break;
			case "fps":
				makeInputDialog("New max fps", "Type a new maximum frames per second (fps).", "" + round(fpsCap),
						(String s) -> parseInt(s) >= 10, 
						(String s) -> frameRate(parseInt(s)), false);
				break;
			case "step":
				makeInputDialog("New simulation time step", "Type a new time step in ms. The time step is how much time passes each frame.\n"
						+ "Note: The time step is only used for the animation and has no effect on the accuracy of the simulation.\n"
						+ "An optimal time step would be 1/fps * 1000", "" + Constants.SIMULATION_TIME_STEPS,
						(String s) -> parseInt(s) > 0,
						(String s) -> Constants.SIMULATION_TIME_STEPS = parseInt(s), false);
				break;
			}
		}
		
		
		
	}

	public static void main(String[] args) {
		PApplet.main("simulation.SimulationMain");
	}

}
