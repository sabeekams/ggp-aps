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

public class AlphaBetaIterativeDeepeningGamer extends StateMachineGamer {
	private HashMap<MachineState, Integer> scoreCache;
	
	public AlphaBetaIterativeDeepeningGamer() {
		super();
		scoreCache = new HashMap<MachineState, Integer>();
	}
	
	@Override
	public String getName() {
		return "AlphaBetaIterativeDeepeningGamer";
	}
	
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long finishBy = timeout - 2000;
		scoreCache = new HashMap<MachineState, Integer>();
		// Search the graph
		getStateValue(getCurrentState(), finishBy, 0, 100);
	}
	
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {		
		long finishBy = timeout - 2000;
		
		StateMachine theMachine = getStateMachine();
		List<Move> myMoves = theMachine.getLegalMoves(getCurrentState(), getRole());
		
		Move bestMove = myMoves.get(0);
		List<Move> jointMoves = theMachine.getLegalJointMoves(getCurrentState(), getRole(), bestMove).get(0);
		int bestMaxValue = getStateValue(theMachine.getNextState(getCurrentState(), jointMoves), finishBy, 0, 100);
		for (int i = 1; i < myMoves.size(); i++) {
			Move move = myMoves.get(i);
			jointMoves = theMachine.getLegalJointMoves(getCurrentState(), getRole(), move).get(0);
			Integer maxValue = getStateValue(theMachine.getNextState(getCurrentState(), jointMoves), finishBy, 0, 100);
			if (maxValue == null) break;
			if (maxValue > bestMaxValue) {
				bestMove = move;
				bestMaxValue = maxValue;
			}
		}
		return bestMove;
	}
	
	public void dumpScoreCache() {
		for (MachineState s: scoreCache.keySet()) {
			System.out.println(s.toString() + ": " + scoreCache.get(s));
		}
	}
	
	public Integer getStateValue(MachineState state, long finishBy, int alpha, int beta) {
		if (SystemCalls.passedTime(finishBy)) return -1;
		Integer cachedScore = scoreCache.get(state);
		if (cachedScore != null)
			return cachedScore;
		
		try {
			StateMachine theMachine = getStateMachine();
			if (theMachine.isTerminal(state)) {
				scoreCache.put(state, theMachine.getGoal(state, getRole()));
				return theMachine.getGoal(state, getRole());
			}
			List<List<Move>> moves = theMachine.getLegalJointMoves(state);
			// Detect if it's our turn or opponent's
			List<Move> myMoves = theMachine.getLegalMoves(state, getRole());
			int minScore = Integer.MAX_VALUE;
			int maxScore = Integer.MIN_VALUE;
			for (int i = 0; i < moves.size(); i++) {
				if (SystemCalls.passedTime(finishBy)) return -1;
				MachineState next = theMachine.getNextState(state, moves.get(i));
				// Get cached score if possible
				Integer score = getStateValue(next, finishBy, alpha, beta);
				// If error or out of time, exit early
				if (score == null) {
					return null;
				}
				if (score < minScore) minScore = score;
				if (score > maxScore) maxScore = score;
				if (myMoves.size() > 1) {
					if (maxScore > alpha) {
						alpha = maxScore;
						if (alpha >= beta) {
							return alpha;
						}
					}
				} else {
					if (maxScore < beta) {
						beta = maxScore;
						if (beta <= alpha) {
							return beta;
						}
					}
				}
			}
			// If this is our move or an opponent's
			if (myMoves.size() == 1) {
				scoreCache.put(state, minScore);
				return minScore;
			} else {
				scoreCache.put(state, maxScore);
				return maxScore;
			}

		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachingProverStateMachine();
	}

	@Override
	public void stateMachineStop() {
		
	}

	@Override
	public void stateMachineAbort() {
		
		
	}	
}
