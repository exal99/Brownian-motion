package simulation;

public class Constants {
	
	private static final int N_LIGHT_ATOMS = 500;
	private static final int N_HEAVY_ATOMS = 10;
	
	private static final int L_MASS = 10;
	private static final int H_MASS = 100;
	
	private static final int L_RADIUS_ATOM = 5;
	private static final int H_RADIUS_ATOM = 10;
	
	private static final float L_START_VEL = 0.3f;
	private static final float H_START_VEL = 0.1f;
	
	private static final int TIME_STEP = 17;
	
	
	public static int NUMBER_OF_LIGHT_ATOMS = N_LIGHT_ATOMS;
	public static int NUMBER_OF_HEAVY_ATOMS = N_HEAVY_ATOMS;
	
	public static int LIGHT_ATOM_RADIUS = L_RADIUS_ATOM;
	public static int HEAVY_ATOM_RADIUS = H_RADIUS_ATOM;
	
	public static int LIGHT_MASS = L_MASS;
	public static int HEAVY_MASS = H_MASS;
	
	public static float LIGHT_MAX_START_VEL = L_START_VEL;
	public static float HEAVY_MAX_START_VEL = H_START_VEL;
	
	public static int SIMULATION_TIME_STEPS = TIME_STEP;
	
	public static final int GET_QUEUE_LIMIT() {
		return (NUMBER_OF_LIGHT_ATOMS + NUMBER_OF_HEAVY_ATOMS) * 2;
	}
	
	public static final void RESET() {
		NUMBER_OF_LIGHT_ATOMS = N_LIGHT_ATOMS;
		NUMBER_OF_HEAVY_ATOMS = N_HEAVY_ATOMS;
		
		LIGHT_ATOM_RADIUS = L_RADIUS_ATOM;
		HEAVY_ATOM_RADIUS = H_RADIUS_ATOM;
		
		LIGHT_MASS = L_MASS;
		HEAVY_MASS = H_MASS;
		
		LIGHT_MAX_START_VEL = L_START_VEL;
		HEAVY_MAX_START_VEL = H_START_VEL;
		
		SIMULATION_TIME_STEPS = TIME_STEP;
	}

}
