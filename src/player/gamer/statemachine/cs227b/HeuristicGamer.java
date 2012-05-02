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

public class HeuristicGamer extends StateMachineGamer {
	private StateMachineCache<MachineState, Integer> terminalScoreCache;
	private StateMachineCache<MachineState, Integer> heuristicScoreCache;
	private static final int betaValue = 100;
	private static final int alphaValue = 0;
	protected static final int initialIterDepth = 4;
	protected static long useHeuristicTimeThreshold = 2000;

	public HeuristicGamer() {
		super();
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
		heuristicScoreCache = new StateMachineCache<MachineState, Integer>();
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
		HashMap<Move, Integer> movesToScore = new HashMap<Move, Integer>();

		if (myMoves.size() == 1 && !SystemCalls.isMemoryAvailable()) {
			terminalScoreCache.swapCaches();
		}

		int iterDepth = initialIterDepth;
		boolean bailout = false;
		boolean someNonTerminalScores = true;
		while (someNonTerminalScores) {
			someNonTerminalScores = false;
			for (Move move: myMoves) {
				jointMoves = theMachine.getLegalJointMoves(currentState, getRole(), move).get(0);
				MachineState next = theMachine.getNextState(currentState, jointMoves);
				boolean[] finalValue = new boolean[1];
				int score = getStateValue(next, finishBy, alphaValue, betaValue, 1, iterDepth, finalValue);
				if (score < 0) {
					bailout = true;
					break;
				}
				if (!finalValue[0]) {
					someNonTerminalScores = true;
				}
				movesToScore.put(move, score);
			}
			if (bailout) break;
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
		report(bestMove, timeout);
		return bestMove;
	}

	public void report(Move movePlayed, long timeout) {
		System.out.println("----");
		System.out.println("Gamer = " + getName());
		System.out.println("Role = " + getRole());
		System.out.println("Move played = " + movePlayed);
		System.out.println("Memory usage (bytes) = " + SystemCalls.getUsedMemoryBytes());
		System.out.println("Memory usage (ratio) = " + SystemCalls.getUsedMemoryRatio());
		System.out.println("Terminal Score Cache");
		terminalScoreCache.report();
		((CachingProverStateMachine) getStateMachine()).report();
		System.out.println("Time left = " + (timeout - System.currentTimeMillis()));
		if (timeout - System.currentTimeMillis() < 0)
			System.out.println("WARNING: OUT OF TIME");
		System.out.println("----");
	}

	public int getStateValue(MachineState state, long finishBy, int alpha, int beta, int depth, int maxDepth, boolean[] isFinalValue) {
		isFinalValue[0] = true;
		if (SystemCalls.passedTime(finishBy)) return -1;
		
		Integer cachedScore = terminalScoreCache.retrieve(state);
		if (cachedScore != null)
			return cachedScore;

		try {	
			int result;
			StateMachine theMachine = getStateMachine();
			if (theMachine.isTerminal(state)) {
				isFinalValue[0]  = true;
				result = theMachine.getGoal(state, getRole());
			} else if (depth == maxDepth) {  // || SystemCalls.passedTime(finishBy - useHeuristicTimeThreshold)) {
				isFinalValue[0]  = false;
				Integer heuristicScore = heuristicScoreCache.retrieve(state);
				if (heuristicScore == null) {
					heuristicScoreCache.cache(state, heuristicScore);
					heuristicScore = getHeuristicScore(state, finishBy);
				} 
				result = heuristicScore;
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
					boolean[] nextFinalValue = new boolean[1];
					int score = getStateValue(next, finishBy, alpha, beta, depth + 1, maxDepth, nextFinalValue);
					// If the retrieved score didn't get cached, then it's a non-terminal score.
					// The non-terminal score property bubbles up to parents.
					if (!nextFinalValue[0]) {
						isFinalValue[0] = false;
					}
					// If error or out of time, exit early
					if (score < 0) return -1;
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
			
			if (isFinalValue[0]) {
				terminalScoreCache.cache(state, result);
			}
			
			return result;
		} catch (Exception e) {
			return -1;
		}
	}

	// Caveat: Total weights for one thread of execution must equal 1.0, e.g.
	// total weights on heuristics for opponent's turn = 0.8, Monte Carlo heuristic weight 0.2
	protected double oneStepMobilityHeuristicWeight = 0.0;
	protected double oneStepFocusHeuristicWeight = 0.0;

	protected double opponentOneStepMobilityHeuristicWeight = 0.0;
	protected double opponentOneStepFocusHeuristicWeight = 0.0;

	protected double monteCarloHeuristicWeight = 1.0;
	protected double medianMinhashMonteCarloHeuristicWeight = 0.0;
	protected double timeFractionForHeuristicComputation = 0.02;
	
	public int getHeuristicScore(MachineState state, long finishBy) throws MoveDefinitionException {
		double score = 0.01;
		
		if (oneStepMobilityHeuristicWeight != 0.0 ||
			oneStepFocusHeuristicWeight != 0.0 ||
			opponentOneStepMobilityHeuristicWeight != 0.0 ||
			opponentOneStepFocusHeuristicWeight != 0.0)
		{
			StateMachine theMachine = getStateMachine();
			List<List<Move>> allMoves = theMachine.getLegalJointMoves(state);
			List<Move> myMoves = theMachine.getLegalMoves(state, getRole());
			boolean myTurn = myMoves.size() > 1;

			// TODO: make heuristic calls time out properly
			if (myTurn) {
				if (oneStepMobilityHeuristicWeight != 0.0)
					score = score + oneStepMobilityHeuristicWeight * getOneStepMobilityHeuristic(myMoves.size());
				if (oneStepFocusHeuristicWeight != 0.0)
					score = score + oneStepFocusHeuristicWeight * getOneStepFocusHeuristic(myMoves.size());
			} else {
				if (opponentOneStepMobilityHeuristicWeight != 0.0)
					score = score + opponentOneStepMobilityHeuristicWeight * getOpponentOneStepMobilityHeuristic(allMoves.size());
				if (opponentOneStepFocusHeuristicWeight != 0.0)
					score = score + opponentOneStepFocusHeuristicWeight * getOpponentOneStepFocusHeuristic(allMoves.size());
			}
		}
		
		if (monteCarloHeuristicWeight != 0.0) {
			long timeNow = System.currentTimeMillis();
			if ((finishBy - timeNow) > 5) {
				long stopTime = (long)(timeNow + (finishBy - timeNow) * timeFractionForHeuristicComputation);
				score = score + monteCarloHeuristicWeight * getMonteCarloHeuristic(state, stopTime);
			}
		}

		if (medianMinhashMonteCarloHeuristicWeight != 0.0) {
			long timeNow = System.currentTimeMillis();
			if ((finishBy - timeNow) > 5) {
				long stopTime = (long)(timeNow + (finishBy - timeNow) * timeFractionForHeuristicComputation);
				score = score + medianMinhashMonteCarloHeuristicWeight * getMedianMinhashMonteCarloHeuristic(state, stopTime);
			}
		}
		
		return (int)Math.round(Math.min(score, 0.99) * 100.0);
	}

	protected static final double oneStepMobilityHeuristicFactor = 7.0;
	protected double getOneStepMobilityHeuristic(int numMoves) {
		return numMoves / (numMoves + oneStepMobilityHeuristicFactor);
	}

	protected static final double opponentOneStepMobilityHeuristicFactor = 7.0;
	protected double getOpponentOneStepMobilityHeuristic(int numMoves) {
		return Math.max(1.0 - numMoves / (numMoves + opponentOneStepMobilityHeuristicFactor), 0.0);
	}

	protected static final double oneStepFocusHeuristicFactor = 2.0;
	protected double getOneStepFocusHeuristic(int numMoves) {
		return Math.max(1.0 - numMoves / (numMoves + oneStepFocusHeuristicFactor), 0.0);
	}

	protected static final double opponentOneStepFocusHeuristicFactor = 2.0;
	protected double getOpponentOneStepFocusHeuristic(int numMoves) {
		return numMoves / (numMoves + opponentOneStepFocusHeuristicFactor);
	}

	protected long maxMonteCarloRuntime = 100;
	protected double getMonteCarloHeuristic(MachineState state, long stopTime) {
		int totalScore = 0;
		int runs = 0;
		long start = System.currentTimeMillis();
		StateMachine theMachine = getStateMachine();
		int[] dummyDepth = new int[1];
		try {
		while (!SystemCalls.passedTime(stopTime) && !SystemCalls.passedTime(start + maxMonteCarloRuntime)) {
			MachineState terminalState = theMachine.performDepthCharge(state, dummyDepth);
			totalScore += theMachine.getGoal(terminalState, getRole());
			runs++;
		}
		} catch (Exception e) {
			// TODO: What to actually return?
			return -1.0;
		}
		return ((double)totalScore / runs) / 100.0;
	}

	protected long maxMonteCarloAttempts = 100;
	protected int victoryThreshold = 50;
	protected double getMinhashMonteCarloHeuristic(MachineState state, long stopTime) {
		int runs = 0;

		StateMachine theMachine = getStateMachine();
		int[] dummyDepth = new int[1];
		try {
			while (!SystemCalls.passedTime(stopTime) && runs < maxMonteCarloAttempts) {
				MachineState terminalState = theMachine.performDepthCharge(state, dummyDepth);
				int score = theMachine.getGoal(terminalState, getRole());
				if (score > victoryThreshold) break;
				runs++;
			}
		} catch (Exception e) {
			// TODO: What to actually return?
			return -1.0;
		}
		return 1.0 - (runs / maxMonteCarloAttempts);
	}
	
	protected double getMedianMinhashMonteCarloHeuristic(MachineState state, long stopTime) {
		double[] score = new double[3];
		for (int i = 0; i < 3; i++) {
			score[i] = getMinhashMonteCarloHeuristic(state, stopTime);
		}
		
		if (score[0] > score[1]) {
			if (score[1] > score[2]) {
				return score[1];
			} else if (score[0] > score[2]) {
				return score[2];
			} else {
				return score[0];
			}
		} else {
			if (score[2] > score[1]) {
				return score[1];
			} else if (score[0] > score[2]) {
				return score[0];
			} else {
				return score[2];
			}
		}
	}
	
	/*
	private double getMonteCarloHeuristic(MachineState state, long finishBy) {
		HashMap<MachineState, Integer[]> result = runMonteCarloSimulations(state);
		int totalScore = 0;
		int frequency = 0;
		for (Integer[] stateInfo: result.values()) {
			totalScore = totalScore + stateInfo[0] * stateInfo[2];
			frequency = frequency + stateInfo[2];
		}
		return (double)totalScore / frequency / 100;
	}
	
	private HashMap<MachineState, Integer[]> runMonteCarloSimulations(MachineState state) {
		// Integer array consists of: { score, depth, frequency }
		HashMap<MachineState, Integer[]> result = new HashMap<MachineState, Integer[]>();
		long start = System.currentTimeMillis();
		int[] depth = new int[1];
		StateMachine theMachine = getStateMachine();
		try {
			while (System.currentTimeMillis() < start + 200) {
				MachineState terminalState = theMachine.performDepthCharge(state, depth);
				Integer[] stateInfo = result.get(terminalState);
				if (stateInfo == null) {
					stateInfo = new Integer[3];
					stateInfo[0] = theMachine.getGoal(terminalState, getRole());
					stateInfo[1] = depth[0];
					stateInfo[2] = 1;
					result.put(terminalState, stateInfo);
				} else {
					stateInfo[2] = stateInfo[2] + 1;
				}
			}
		} catch (Exception e) {
			return result;
		}
		return result;
	}*/

	//private double 

	public void stateMachineStop() {
	}

	public void stateMachineAbort() {		
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachingProverStateMachine();
	}

}