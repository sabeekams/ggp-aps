package player.gamer.statemachine.cs227b;

public class AdvancedGamer extends HeuristicGamer {
	
	@Override
	public String getName() {
		return "AdvancedGamer";
	}
	
	protected double oneStepMobilityHeuristicWeight = 0.0;
	protected double oneStepFocusHeuristicWeight = 0.5;
	protected double opponentOneStepMobilityHeuristicWeight = 0.0;
	protected double opponentOneStepFocusHeuristicWeight = 0.5;
	protected double monteCarloHeuristicWeight = 0.0;
	protected double medianMinhashMonteCarloHeuristicWeight = 0.5;

}
