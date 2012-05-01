package player.gamer.statemachine.cs227b;

public class SystemCalls {
	private static final double freeMemoryThreshold = 0.4;
	
	public static long getFreeMemoryBytes() {
		return Runtime.getRuntime().freeMemory();
	}
	
	public static double getFreeMemoryRatio() {
		return Runtime.getRuntime().freeMemory() / (double)Runtime.getRuntime().totalMemory();
	}
	
	public static boolean isMemoryAvailable() {
		return getFreeMemoryRatio() > freeMemoryThreshold;
	}
	
	public static long getUsedMemoryBytes() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}
	
	public static double getUsedMemoryRatio() {
		double totalMemory = Runtime.getRuntime().totalMemory();
		return (totalMemory - Runtime.getRuntime().freeMemory()) / totalMemory;
	}
	
	public static boolean passedTime(long finishBy) {
		return System.currentTimeMillis() > finishBy;
	}
}
