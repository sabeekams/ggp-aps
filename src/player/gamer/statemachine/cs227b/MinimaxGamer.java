package player.gamer.statemachine.cs227b;

import java.util.HashMap;
import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
//import util.statemachine.implementation.prover.ProverStateMachine;

public class MinimaxGamer extends StateMachineGamer {

	private HashMap<MachineState, Integer> scoreCache;
	
	public MinimaxGamer() {
		super();
		scoreCache = new HashMap<MachineState, Integer>();
	}
	
	@Override
	public StateMachine getInitialStateMachine() {
		return new CachingProverStateMachine();
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long finishBy = timeout - 1000;
		// Search the graph
		scoreCache = new HashMap<MachineState, Integer>();
		getStateValue(getCurrentState(), finishBy);
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {		
		long finishBy = timeout - 1000;
		
		StateMachine theMachine = getStateMachine();
		List<Move> myMoves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move bestMove = myMoves.get(0);
		List<Move> jointMoves = theMachine.getLegalJointMoves(getCurrentState(), getRole(), bestMove).get(0);
		int bestMaxValue = getStateValue(theMachine.getNextState(getCurrentState(), jointMoves), finishBy);
		for (int i = 1; i < myMoves.size(); i++) {
			Move move = myMoves.get(i);
			jointMoves = theMachine.getLegalJointMoves(getCurrentState(), getRole(), move).get(0);
			int maxValue = getStateValue(theMachine.getNextState(getCurrentState(), jointMoves), finishBy);
			if (maxValue > bestMaxValue) {
				bestMove = move;
				bestMaxValue = maxValue;
			}
		}
		System.out.println("ScoreCache size" + scoreCache.values().size());
		return bestMove;
	}

	@Override
	public void stateMachineStop() {
		// do nothing
	}

	@Override
	public void stateMachineAbort() {
		// do nothing
	}
	
	@Override
	public String getName() {
		return "MinimaxGamer";
	}
	
	public boolean timedOut(long finishBy) {
		return System.currentTimeMillis() > finishBy;
	}
	
	public int getStateValue(MachineState state, long finishBy) {
		if (timedOut(finishBy)) return -1;
		
		try {
			StateMachine theMachine = getStateMachine();
			if (theMachine.isTerminal(state)) {
				// TODO: Remove this line when done testing
				scoreCache.put(state, theMachine.getGoal(state, getRole()));
				return theMachine.getGoal(state, getRole());
			}
			List<List<Move>> moves = theMachine.getLegalJointMoves(state);
			// Detect if it's our turn or opponent's
			List<Move> myMoves = theMachine.getLegalMoves(state, getRole());
			int minScore = Integer.MAX_VALUE;
			int maxScore = Integer.MIN_VALUE;
			for (int i = 0; i < moves.size(); i++) {
				if (timedOut(finishBy)) return -1;
				MachineState next = theMachine.getNextState(state, moves.get(i));
				// Get cached score if possible
				Integer cachedScore = scoreCache.get(next);
				if (cachedScore != null) {
					return cachedScore.intValue();
				} else {
					int score = getStateValue(next, finishBy);
					scoreCache.put(state, score);
					// If error or out of time, exit early
					if (score == -1) {
						return -1;
					}
					if (score < minScore) minScore = score;
					if (score > maxScore) maxScore = score;
				}
			}
			// If this is our move or an opponent's
			if (myMoves.size() == 1) {
				return minScore;
			} else {
				return maxScore;
			}

		} catch (Exception e) {
			System.out.println("ERROR");
			return -1;
		}
	}	
}
