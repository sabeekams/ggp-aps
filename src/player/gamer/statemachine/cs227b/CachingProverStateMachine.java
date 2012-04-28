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

// TODO: add garbage collection

public class CachingProverStateMachine extends ProverStateMachine
{
	private static final int maxCacheSize = 100000;
	// The List key must be a tuple of MachineState, Role
	private HashMap<List<Object>, List<Move>> movesCache;
	// The List key must be a tuple of MachineState, List<Move>
	private HashMap<List<Object>, MachineState> nextStatesCache;
	// Secondary caches to switch to when the primary ones get too big
	private HashMap<List<Object>, List<Move>> secondMovesCache;
	private HashMap<List<Object>, MachineState> secondNextStatesCache;

	
	
	public CachingProverStateMachine() {
		movesCache = new HashMap<List<Object>, List<Move>>();
		nextStatesCache = new HashMap<List<Object>, MachineState>();
		secondMovesCache = new HashMap<List<Object>, List<Move>>();
		secondNextStatesCache = new HashMap<List<Object>, MachineState>();
	}
	
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException
	{
		ArrayList<Object> key = new ArrayList<Object>();
		key.add(state); key.add(role);
		
		List<Move> moves  = movesCache.get(key);
		if (moves == null) {
			moves = super.getLegalMoves(state, role);
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
			nextStatesCache.put(key, nextState);
		} else {
			secondNextStatesCache.put(key, nextState);
		}
		return nextState;
	}	

	@Override
	public void doPerMoveWork() {
		System.out.println("***");
		System.out.println(movesCache.size());
		System.out.println(secondMovesCache.size());
		System.out.println(nextStatesCache.size());
		System.out.println(secondNextStatesCache.size());
		System.out.println("***");
		if (movesCache.size() > maxCacheSize) {
			movesCache = secondMovesCache;
			secondMovesCache = new HashMap<List<Object>, List<Move>>();
		}
		if (nextStatesCache.size() > maxCacheSize) {
			nextStatesCache = secondNextStatesCache;
			secondNextStatesCache = new HashMap<List<Object>, MachineState>();
		}
		super.doPerMoveWork();
	}
}