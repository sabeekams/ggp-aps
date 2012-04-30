package player.gamer.statemachine.cs227b;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;

import java.lang.Runtime;

public class CachingProverStateMachine extends ProverStateMachine
{
	private static final double memoryThreshold = 0.4;
	// The List key must be a tuple of MachineState, Role
	private HashMap<List<Object>, List<Move>> movesCache;
	// The List key must be a tuple of MachineState, List<Move>
	private HashMap<List<Object>, MachineState> nextStatesCache;
	// Cache terminal states as true and non-terminal states as false;	
	private HashMap<MachineState, Boolean> terminalStatesCache;
	//Secondary caches to switch to when the primary ones get too big
	private HashMap<List<Object>, List<Move>> secondMovesCache;
	private HashMap<List<Object>, MachineState> secondNextStatesCache;
	private HashMap<MachineState, Boolean> secondTerminalStatesCache;

	public boolean moreThanPercentMemoryAvailable(double threshold) {
		double totalMemory = (double)Runtime.getRuntime().totalMemory();
		double freeMemory = (double)Runtime.getRuntime().freeMemory();
		
		return (freeMemory / totalMemory) > threshold;
	}
	
	public CachingProverStateMachine() {
		movesCache = new HashMap<List<Object>, List<Move>>();
		secondMovesCache = new HashMap<List<Object>, List<Move>>();
		nextStatesCache = new HashMap<List<Object>, MachineState>();
		secondNextStatesCache = new HashMap<List<Object>, MachineState>();
		terminalStatesCache = new HashMap<MachineState, Boolean>();
		secondTerminalStatesCache = new HashMap<MachineState, Boolean>();
	}
	
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException
	{
		ArrayList<Object> key = new ArrayList<Object>();
		key.add(state); key.add(role);
		
		List<Move> moves  = movesCache.get(key);
		if (moves == null) {
			moves = super.getLegalMoves(state, role);
			if (moreThanPercentMemoryAvailable(memoryThreshold))
				movesCache.put(key, moves);
		} else {
			secondMovesCache.put(key, moves);
		}
		return moves;
	}
	
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException
	{
		ArrayList<Object> key = new ArrayList<Object>();
		key.add(state); key.add(moves);
		
		MachineState nextState  = nextStatesCache.get(key);
		if (nextState == null) {
			nextState = super.getNextState(state, moves);
			if (moreThanPercentMemoryAvailable(memoryThreshold))
				nextStatesCache.put(key, nextState);
		} else {
			secondNextStatesCache.put(key, nextState);
		}
		return nextState;
	}
	
	@Override
	public boolean isTerminal(MachineState state)
	{
		Boolean cachedBool = terminalStatesCache.get(state);
		if (cachedBool == null) {
			cachedBool = new Boolean(super.isTerminal(state));
			if (moreThanPercentMemoryAvailable(memoryThreshold))
				terminalStatesCache.put(state, cachedBool);
		} else {
			secondTerminalStatesCache.put(state, cachedBool);
		}
		return cachedBool.booleanValue();
	}
	
	
	public void printMemoryUsage(){
		System.out.println("Next State Cache Size = " + nextStatesCache.size());
		System.out.println("Terminal State Cache Size = " + terminalStatesCache.size());
		System.out.println("Moves Cache Size = " + movesCache.size());
	}

	@Override
	public void doPerMoveWork() {
		if (!moreThanPercentMemoryAvailable(memoryThreshold)) {
			movesCache = secondMovesCache;
			secondMovesCache = new HashMap<List<Object>, List<Move>>();
			nextStatesCache = secondNextStatesCache;
			secondNextStatesCache = new HashMap<List<Object>, MachineState>();
			terminalStatesCache = secondTerminalStatesCache;
			secondTerminalStatesCache = new HashMap<MachineState, Boolean>();
		}
		super.doPerMoveWork();
	}
}