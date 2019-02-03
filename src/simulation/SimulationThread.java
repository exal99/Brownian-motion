package simulation;
import simulation.Simulation.SimulationDiffrence;


import java.util.concurrent.BlockingQueue;

import processing.core.PApplet;

public class SimulationThread extends Thread {
	
	public static final Object LOCK = new Object();
	
	private BlockingQueue<SimulationDiffrence> resultQueue;
	private Simulation simulation;
	private volatile boolean running;
	
	public SimulationThread(BlockingQueue<SimulationDiffrence> queue, int nLightAtoms, int nHeavyAtoms, int width, int height, PApplet applet) {
		super("Simulation Thread");
		resultQueue = queue;
		simulation = new Simulation(nLightAtoms, nHeavyAtoms, width, height, applet);
		running = false;
	}
	
	public boolean doneLoading() {
		return resultQueue.size() == Constants.GET_QUEUE_LIMIT();
	}
	
	public Atom[] getAtoms() {
		return simulation.getAtomsClone();
	}
	
	@Override
	public void start() {
		running = true;
		super.start();
	}
	
	@Override
	public void run() {
		while (running && !isInterrupted()) {
			synchronized(LOCK) {
				LOCK.notifyAll();
			}
			try {
				resultQueue.put(simulation.advanceSimulation());
			} catch (InterruptedException e) {
				interrupt();
				return;
			}
			
		}
	}
		
	public void terminate() {
		running = false;
	}

}
