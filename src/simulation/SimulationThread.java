package simulation;
import simulation.Simulation.SimulationDiffrence;


import java.util.concurrent.BlockingQueue;

import processing.core.PApplet;

public class SimulationThread extends Thread {
	
	public static final Object LOCK = new Object();
	
	private BlockingQueue<SimulationDiffrence> resultQueue;
	private Simulation simulation;
	private volatile boolean running;
	private volatile boolean reseting;
	
	public SimulationThread(BlockingQueue<SimulationDiffrence> queue, int nLightAtoms, int nHeavyAtoms, int width, int height, PApplet applet) {
		super("Simulation Thread");
		resultQueue = queue;
		simulation = new Simulation(nLightAtoms, nHeavyAtoms, width, height, applet);
		running = false;
		reseting = false;
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
	
	public Atom[] reset(BlockingQueue<SimulationDiffrence> queue, int nLightAtoms, int nHeavyAtoms, int width, int height, PApplet applet) {
		reseting = true;
		interrupt();
		synchronized(simulation) {
			resultQueue = queue;
			simulation.reset(nLightAtoms, nHeavyAtoms, width, height, applet);
			reseting = false;
			simulation.notify();
			return getAtoms();
		}
	}
	
	@Override
	public void run() {
		synchronized(simulation) {
			while (running) {
				if (!reseting) {
					try {
						synchronized(LOCK) {
							LOCK.notifyAll();
						}
						resultQueue.put(simulation.advanceSimulation());
					} catch (InterruptedException e) {
						if (!running)
							return;
					}
				} else {
					try {
						simulation.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} 
				}
				
			}
		}
	}
		
	public void terminate() {
		running = false;
		interrupt();
	}

}
