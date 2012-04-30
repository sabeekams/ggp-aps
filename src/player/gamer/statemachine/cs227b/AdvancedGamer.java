package player.gamer.statemachine.cs227b;

import util.statemachine.MachineState;

public class AdvancedGamer extends HeuristicGamer {
	

	@Override
	public String getName() {
		return "AdvancedGamer";
	}
	
	
	

	public int getHeuristic(int numMoves, boolean myTurn, MachineState state, long timeLimit) {
		if(myTurn) {
			
			return (int)(getMobilityHeuristicScore(numMoves) * 10.0 + getMonteCarloHeuristicScore(state, timeLimit - 50)* 90);
			
		} else {
			
			return (int)(getFocusHeuristicScore(numMoves) * 10.0 + getMonteCarloHeuristicScore(state,timeLimit - 50)* 90);
		}
		
	}

}
