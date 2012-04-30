package player.gamer.statemachine.cs227b;

import java.util.HashMap;
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
	protected HashMap<MachineState, Integer> scoreCache;
	protected int maxMobilityObserved;
	protected int numPlayers;
	protected static final int depthThreshold = 10;
	protected static final int timeThreshold = 4000;
	protected static final double focusHeuristicFactor = 5.0;
	protected static final double mobilityHeuristicFactor = 12.0;
	protected static final int timeoutThreshold = 2000;
	protected static final double memoryThreshold = 0.4;
	
	public HeuristicGamer() {
		super();
	}
	
	@Override
	public String getName() {
		return "HeuristicGamer";
	}
	
	public StateMachine getInitialStateMachine() {
		return new CachingProverStateMachine();
	}
	
	@Override
	public void stateMachineStop() {
		// do nothing
	}

	@Override
	public void stateMachineAbort() {
		// do nothing
	}
	
	public boolean timedOut(long finishBy) {
		return System.currentTimeMillis() > finishBy;
	}
	
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long finishBy = timeout - timeoutThreshold;
		scoreCache = new HashMap<MachineState, Integer>();
		maxMobilityObserved = 2;
		numPlayers = getStateMachine().getRoles().size();
		// Search the graph
		getStateValue(getCurrentState(), finishBy, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
	}
	
	
	public boolean moreThanPercentMemoryAvailable(double threshold) {
		double totalMemory = (double)Runtime.getRuntime().totalMemory();
		double freeMemory = (double)Runtime.getRuntime().freeMemory();
		
		return (freeMemory / totalMemory) > threshold;
	}
	
	/*Keeps track of memory and cache
	 */
	public void logUsage(Move movePlayed, Role role, long timeout) {
		System.out.println("----");
		System.out.println("Roll = " + role);
		System.out.println("Move played = " + movePlayed.toString());
		
		long totalMemory = Runtime.getRuntime().totalMemory();
		long freeMemory = Runtime.getRuntime().freeMemory();
		
		long memoryUsage = (totalMemory - freeMemory);
		System.out.println("Memory usage (bytes) = " + memoryUsage);
		double percentUsage = 100.0*memoryUsage/(double)totalMemory;
		System.out.println("Memory usage (percent) = " + percentUsage);
		
		System.out.println("ScoreCache size = " + scoreCache.values().size());
		((CachingProverStateMachine) getStateMachine()).printMemoryUsage();
		
		System.out.println("Time left = " + (timeout - System.currentTimeMillis()));
		System.out.println("----");
	}
	
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {		
		long finishBy = timeout - timeoutThreshold;
		
		StateMachine theMachine = getStateMachine();
		List<Move> myMoves = theMachine.getLegalMoves(getCurrentState(), getRole());
		
		Move bestMove = myMoves.get(0);
		List<Move> jointMoves = theMachine.getLegalJointMoves(getCurrentState(), getRole(), bestMove).get(0);
		int bestMaxValue = getStateValue(theMachine.getNextState(getCurrentState(), jointMoves), finishBy, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
		for (int i = 1; i < myMoves.size(); i++) {
			Move move = myMoves.get(i);
			jointMoves = theMachine.getLegalJointMoves(getCurrentState(), getRole(), move).get(0);
			int maxValue = getStateValue(theMachine.getNextState(getCurrentState(), jointMoves), finishBy, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
			if (maxValue > bestMaxValue) {
				bestMove = move;
				bestMaxValue = maxValue;
			}
			if (timedOut(finishBy)) return bestMove;
		}
		logUsage(bestMove, getRole(), timeout);
		
		return bestMove;
	}
	
	/*
	 * 
	 */
	public int getStateValue(MachineState state, long finishBy, int alpha, int beta, int depth) {
		if (timedOut(finishBy)) return -1;
		
		try {
			StateMachine theMachine = getStateMachine();
			if (theMachine.isTerminal(state)) {
				
				if (moreThanPercentMemoryAvailable(memoryThreshold)) {
					scoreCache.put(state, theMachine.getGoal(state, getRole()));
				}
				return theMachine.getGoal(state, getRole());
			}
			List<List<Move>> moves = theMachine.getLegalJointMoves(state);
			// Detect if it's our turn or opponent's
			List<Move> myMoves = theMachine.getLegalMoves(state, getRole());
			if (maxMobilityObserved < myMoves.size()) maxMobilityObserved = myMoves.size();
			int minScore = Integer.MAX_VALUE;
			int maxScore = Integer.MIN_VALUE;
			for (int i = 0; i < moves.size(); i++) {
				if (timedOut(finishBy)) return -1;
				MachineState next = theMachine.getNextState(state, moves.get(i));
				if (timedOut(finishBy)) return -1;
				// Get cached score if possible
				Integer cachedScore = scoreCache.get(next);
				if (cachedScore != null) {
					return cachedScore.intValue();
				} else {
					int score;
					boolean usedHeuristic = false;
					if (useHeuristic(finishBy, depth)) {
						score = getHeuristicForState(state, finishBy);
						usedHeuristic = true;
						//System.out.printf("heuristic score: %d\n", score);
					} else {
						score = getStateValue(next, finishBy, alpha, beta, depth+1);
					}
					// If error or out of time, exit early
					if (score < 0) {
						return -1;
					}
					
					if (!usedHeuristic) {
						if (moreThanPercentMemoryAvailable(memoryThreshold)) {
							scoreCache.put(state, score);
						}
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
	
	public boolean useHeuristic(long finishBy, int depth) {
		return (System.currentTimeMillis() > (finishBy - timeThreshold)) || (depth > depthThreshold);
	}
	
	public int getHeuristic(int numMoves, boolean myTurn, MachineState state, long timeLimit) {
		if(myTurn) {
			return (int)(getMobilityHeuristicScore(numMoves) * 100.0);
		} else {
			return (int)(getFocusHeuristicScore(numMoves) * 100.0);
		}
	}
	
	public int getHeuristicForState(MachineState state, long finishBy) {
		StateMachine theMachine = getStateMachine();
		try {
			if (timedOut(finishBy)) return -1;
			List<Move> myMoves = theMachine.getLegalMoves(state, getRole());
			int numMoves = myMoves.size();
			if (maxMobilityObserved < numMoves) maxMobilityObserved = numMoves;
			if (timedOut(finishBy)) return -1;
			
			boolean myTurn = true;
			if (numMoves == 1) {
				int totalOpponentMoves = 0;
				for (Role role : theMachine.getRoles()) {
					if (timedOut(finishBy)) return -1;
					int opponentMoves = theMachine.getLegalMoves(state, role).size();
					if (opponentMoves > 1) {
						myTurn = false;
						totalOpponentMoves += opponentMoves;
					}
				}
				if (myTurn == true) return (int)(100.0 / numPlayers); // everyone has only one move.
				else numMoves = totalOpponentMoves;
			}
			if (timedOut(finishBy)) return -1;
			return getHeuristic(numMoves, myTurn, state, Math.min(System.currentTimeMillis() + 50, finishBy));
			
		} catch (MoveDefinitionException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	// returns a score from zero to one.
	public double getMobilityHeuristicScore(int moves) {
		return moves / (moves + mobilityHeuristicFactor);
	}
	
	// returns a score from zero to one.
	public double getFocusHeuristicScore(int moves) {
		return 1.0 - (moves / (moves + focusHeuristicFactor));
	}
	
	// returns a score from zero to one.
	private int[] returnedDepth = new int[1];
	double getMonteCarloHeuristicScore(MachineState state, long timeLimit) {
		if (System.currentTimeMillis() > timeLimit) return 0;
		
	    StateMachine theMachine = getStateMachine();
	    
	    List<Move> moves;
		try {
			moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		} catch (MoveDefinitionException e1) {
			e1.printStackTrace();
			return 0;
		}
	    
	    int[] moveTotalPoints = new int[moves.size()];
		int[] moveTotalAttempts = new int[moves.size()];
		
		for (int i = 0; true; i = (i+1) % moves.size()) {
		    if (System.currentTimeMillis() > timeLimit)
		        break;

		    try {
		    	MachineState finalState = theMachine.performDepthCharge(theMachine.getRandomNextState(state, getRole(), moves.get(i)), returnedDepth);
		    	moveTotalPoints[i] += theMachine.getGoal(finalState, getRole());
		    	moveTotalAttempts[i] += 1;
		    } catch (Exception e) {
		    	e.printStackTrace();
		    	return 0;
		    }
		}
		
		// TODO: use cache to load initial values for total points and attempts
		double totalPoints = 0;
		double numAttempts = 0;
		for (int i = 0; i < moves.size(); i++){
			totalPoints += moveTotalPoints[i];
			numAttempts += moveTotalAttempts[i];
		}
		// TODO: check memory before saving to cache
		// TODO: save total points and attempts to cache by state
		System.out.println("MonteCarlo number of attempts = " + (int) numAttempts);
		return totalPoints / (numAttempts * 100.0);
	}
}
