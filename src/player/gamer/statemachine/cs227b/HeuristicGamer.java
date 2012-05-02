package player.gamer.statemachine.cs227b;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class HeuristicGamer extends StateMachineGamer {
	private StateMachineCache<MachineState, Integer> terminalScoreCache;
	private static final int betaValue = 100;
	private static final int alphaValue = 0;
	private static final int initialIterDepth = 2;
	
	public HeuristicGamer() {
		super();
		terminalScoreCache = new StateMachineCache<MachineState, Integer>();
	}
	
	@Override
	public String getName() {
		return "HeuristicGamer";
	}
	
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		terminalScoreCache = new StateMachineCache<MachineState, Integer>();		
		// Search the graph
		stateMachineSelectMove(timeout);
	}
	
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {	
		long finishBy = timeout - 2000;
		
		StateMachine theMachine = getStateMachine();
		MachineState currentState = getCurrentState();
		List<Move> myMoves = theMachine.getLegalMoves(currentState, getRole());		
		Move bestMove = myMoves.get(0);
		int bestScore = Integer.MIN_VALUE;
		List<Move> jointMoves = theMachine.getLegalJointMoves(currentState, getRole(), bestMove).get(0);
		int iterDepth = initialIterDepth;
		boolean someNonTerminalScores = true;
		HashMap<Move, Integer> movesToScore = new HashMap<Move, Integer>();
		
		if (myMoves.size() == 1 && !SystemCalls.isMemoryAvailable()) {
			terminalScoreCache.swapCaches();
		}
		
		while (someNonTerminalScores) {
			someNonTerminalScores = false;
			for (Move move: myMoves) {
				jointMoves = theMachine.getLegalJointMoves(currentState, getRole(), move).get(0);
				MachineState next = theMachine.getNextState(currentState, jointMoves);
				int score = getStateValue(next, finishBy, alphaValue, betaValue, 1, iterDepth);
				if (score < 0) break;
				if (terminalScoreCache.retrieve(next) == null) someNonTerminalScores = true;
				movesToScore.put(move, score);
			}
			iterDepth++;
		}
				
		for (Move move: movesToScore.keySet()) {
			int score = movesToScore.get(move);
			if (score > bestScore) {
				bestMove = move;
				bestScore = score;
			}
			System.out.println("bestMove: " + bestMove + " bestScore: " + bestScore + " move: " + move + " score: " + score);
		}
		System.out.println("Iteration depth: " + iterDepth);
		report(bestMove, getRole(), timeout);
		return bestMove;
	}
	
	public void report(Move movePlayed, Role role, long timeout) {
		System.out.println("----");
		System.out.println("Role = " + role);
		System.out.println("Move played = " + movePlayed);
		System.out.println("Memory usage (bytes) = " + SystemCalls.getUsedMemoryBytes());
		System.out.println("Memory usage (ratio) = " + SystemCalls.getUsedMemoryRatio());
		System.out.println("Terminal Score Cache");
		terminalScoreCache.report();
		((CachingProverStateMachine) getStateMachine()).report();
		System.out.println("Time left = " + (timeout - System.currentTimeMillis()));
		System.out.println("----");
	}
	
	public int getStateValue(MachineState state, long finishBy, int alpha, int beta, int depth, int maxDepth) {
		if (SystemCalls.passedTime(finishBy)) return -1;
		Integer cachedScore = terminalScoreCache.retrieve(state);
		if (cachedScore != null)
			return cachedScore;
		
		try {
			boolean isTerminalScore = true;
			int result;
			StateMachine theMachine = getStateMachine();
			if (theMachine.isTerminal(state)) {
				isTerminalScore = true;
				result = theMachine.getGoal(state, getRole());
			} else if (depth == maxDepth) {
				isTerminalScore = false;
				// TODO: heuristics stuff
				result = getHeuristicScore(state);
			} else {
				List<List<Move>> allMoves = theMachine.getLegalJointMoves(state);
				// Detect if it's our turn or opponent's
				List<Move> myMoves = theMachine.getLegalMoves(state, getRole());
				int minScore = Integer.MAX_VALUE;
				int maxScore = Integer.MIN_VALUE;
				for (int i = 0; i < allMoves.size(); i++) {
					if (SystemCalls.passedTime(finishBy)) return -1;
					MachineState next = theMachine.getNextState(state, allMoves.get(i));
					// Get cached score if possible
					int score = getStateValue(next, finishBy, alpha, beta, depth + 1, maxDepth);
					// If the retrieved score didn't get cached, then it's a non-terminal score.
					// The non-terminal score property bubbles up to parents.
					if (terminalScoreCache.retrieveNoCache(next) == null) isTerminalScore = false;
					// If error or out of time, exit early
					if (score == -1) return -1;
					if (score < minScore) minScore = score;
					if (score > maxScore) maxScore = score;
					// Alpha beta pruning
					if (myMoves.size() > 1) {
						if (maxScore > alpha) {
							alpha = maxScore;
							if (alpha >= beta) {
								result = alpha;
								break;
							}
						}
					} else {
						if (minScore < beta) {
							beta = minScore;
							if (beta <= alpha) {
								result = beta;
								break;
							}
						}
					}
				}
				// If this is our move or an opponent's
				if (myMoves.size() == 1) {
					result = minScore;
				} else {
					result = maxScore;
				}
			}
			if (isTerminalScore) terminalScoreCache.cache(state, result);
			return result;
		} catch (Exception e) {
			return -1;
		}
	}

	
	private double oneStepMobilityHeuristicWeight = 1.0;
	private double oneStepFocusHeuristicWeight = 0.0;
	private double opponentOneStepMobilityHeuristicWeight = 1.0;
	private double opponentOneStepFocusHeuristicWeight = 0.0;
	public int getHeuristicScore(MachineState state) throws MoveDefinitionException {
		StateMachine theMachine = getStateMachine();
		List<List<Move>> allMoves = theMachine.getLegalJointMoves(state);
		List<Move> myMoves = theMachine.getLegalMoves(state, getRole());
		boolean myTurn = myMoves.size() > 1;
		double score = 0.0;
		// TODO: make heuristic calls time out properly
		if (myTurn) {
			score = score + oneStepMobilityHeuristicWeight * getOneStepMobilityHeuristic(myMoves.size());
			score = score + oneStepFocusHeuristicWeight * getOneStepFocusHeuristic(myMoves.size());
		} else {
			score = score + opponentOneStepMobilityHeuristicWeight * getOpponentOneStepMobilityHeuristic(allMoves.size());
			score = score + opponentOneStepFocusHeuristicWeight * getOpponentOneStepFocusHeuristic(allMoves.size());
		}
		
		return (int)(score * 100);
	}
	
	private static final double oneStepMobilityHeuristicFactor = 12.0;
	private double getOneStepMobilityHeuristic(int numMoves) {
		return numMoves / (numMoves + oneStepMobilityHeuristicFactor);
	}

	private static final double opponentOneStepMobilityHeuristicFactor = 12.0;
	private double getOpponentOneStepMobilityHeuristic(int numMoves) {
		return 1 - (numMoves / (numMoves + opponentOneStepMobilityHeuristicFactor));
	}
	
	private static final double oneStepFocusHeuristicFactor = 5.0;
	private double getOneStepFocusHeuristic(int numMoves) {
		return 1 - numMoves / (numMoves + oneStepFocusHeuristicFactor);
	}

	private static final double opponentOneStepFocusHeuristicFactor = 5.0;
	private double getOpponentOneStepFocusHeuristic(int numMoves) {
		return numMoves / (numMoves + opponentOneStepFocusHeuristicFactor);
	}
		
	public void stateMachineStop() {
	}

	public void stateMachineAbort() {		
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachingProverStateMachine();
	}
	
}
