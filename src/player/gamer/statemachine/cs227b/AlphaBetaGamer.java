package player.gamer.statemachine.cs227b;

import java.util.HashMap;
import java.util.List;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class AlphaBetaGamer extends MinimaxGamer {

	private HashMap<MachineState, Integer> scoreCache;
	
	public AlphaBetaGamer() {
		super();
		scoreCache = new HashMap<MachineState, Integer>();
	}
	
	@Override
	public String getName() {
		return "AlphaBetaGamer";
	}
	
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long finishBy = timeout - 1000;
		// Search the graph
		scoreCache = new HashMap<MachineState, Integer>();
		getStateValue(getCurrentState(), finishBy, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
	
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {		
		long finishBy = timeout - 1000;
		
		StateMachine theMachine = getStateMachine();
		List<Move> myMoves = theMachine.getLegalMoves(getCurrentState(), getRole());
		// Completely relies on the assumption that this is a two-player alternating moves game.
		// Totally does the wrong thing otherwise
		if (myMoves.size() == 1) {
			return myMoves.get(0);
		} else {
			Move bestMove = myMoves.get(0);
			List<Move> jointMoves = theMachine.getLegalJointMoves(getCurrentState(), getRole(), bestMove).get(0);
			int bestMaxValue = getStateValue(theMachine.getNextState(getCurrentState(), jointMoves), finishBy, Integer.MIN_VALUE, Integer.MAX_VALUE);
			for (int i = 1; i < myMoves.size(); i++) {
				Move move = myMoves.get(i);
				jointMoves = theMachine.getLegalJointMoves(getCurrentState(), getRole(), move).get(0);
				int maxValue = getStateValue(theMachine.getNextState(getCurrentState(), jointMoves), finishBy, Integer.MIN_VALUE, Integer.MAX_VALUE);
				if (maxValue > bestMaxValue) {
					bestMove = move;
					bestMaxValue = maxValue;
				}
			}
			System.out.println("ScoreCache size" + scoreCache.values().size());
			return bestMove;
		}
		
	}
	
	public int getStateValue(MachineState state, long finishBy, int alpha, int beta) {
		if (timedOut(finishBy)) return -1;
		
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
				if (timedOut(finishBy)) return -1;
				MachineState next = theMachine.getNextState(state, moves.get(i));
				// Get cached score if possible
				Integer cachedScore = scoreCache.get(next);
				if (cachedScore != null) {
					return cachedScore.intValue();
				} else {
					int score = getStateValue(next, finishBy, alpha, beta);
					scoreCache.put(state, score);
					// If error or out of time, exit early
					if (score == -1) {
						return -1;
					}
					if (score < minScore) minScore = score;
					if (score > maxScore) maxScore = score;
					if (myMoves.size() == 1) {
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
					scoreCache.put(state, minScore);
				}
			}
			// If this is our move or an opponent's
			if (myMoves.size() == 1) {
				return minScore;
			} else {
				return maxScore;
			}

		} catch (Exception e) {
			return -1;
		}
	}	
}
