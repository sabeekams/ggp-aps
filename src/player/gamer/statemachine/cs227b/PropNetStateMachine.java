                      package player.gamer.statemachine.cs227b;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.factory.OptimizingPropNetFactory;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class PropNetStateMachine extends StateMachine {
	/** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;
    
	@Override
	public void initialize(List<Gdl> description) {
		// TODO Auto-generated method stub
		try {
			propNet = OptimizingPropNetFactory.create(description);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        roles = propNet.getRoles();
        ordering = getOrdering();
	}

	public List<Proposition> getOrdering()
	{	
		List<Proposition> order = new LinkedList<Proposition>();
		List<Component> components = new ArrayList<Component>(propNet.getComponents());
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());
		
		for (Proposition proposition : propNet.getBasePropositions().values()) {
			components.remove(proposition);
		}
		for (Proposition proposition : propNet.getInputPropositions().values()) {
			components.remove(proposition);
		}
		components.remove(propNet.getInitProposition());
		
		while (!components.isEmpty()) {
			List<Component> componentsCopy = new ArrayList<Component>(components);
			for ( Component component : components ) {
				boolean inputsSatisfied = true;
				for ( Component input : component.getInputs() ) {
					if ( components.contains(input) ) {
						inputsSatisfied = false;
						break;
					}
				}
				if (inputsSatisfied) {
					componentsCopy.remove(component);
					if (propositions.contains(component))
						order.add((Proposition) component);
				}
			}
			components = componentsCopy;
		}
		return order;
	}
	
	@Override
	public int getGoal(MachineState state, Role role)
			throws GoalDefinitionException {
		SetBasePropositions(state);
		Propagate();
		for (Proposition p: propNet.getGoalPropositions().get(role)) {
			if (p.getValue()) {
				GdlFunction func = (GdlFunction)p.getName();
				return Integer.parseInt(func.getBody().get(1).toString());
			}
		}
		return 0;
	}

	@Override
	public boolean isTerminal(MachineState state) {
		SetBasePropositions(state);
		Propagate();
		return propNet.getTerminalProposition().getValue();
	}

	@Override
	public List<Role> getRoles() {
		return roles;
	}

	@Override
	public MachineState getInitialState() {
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		propNet.getInitProposition().setValue(true);
		for (Proposition p: propNet.getBasePropositions().values()) {
			if (p.getSingleInput().getValue()) {
				contents.add(p.getName().toSentence());
			}
			p.setValue(p.getSingleInput().getValue());
		}
		MachineState result = new MachineState(contents);
		return result;
	}

	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
			throws MoveDefinitionException {
		List<GdlSentence> moves = new ArrayList<GdlSentence>();
		SetBasePropositions(state);
		Propagate();
				
		for (Proposition p: propNet.getLegalPropositions().get(role)) {
			if (p.getValue()) {
				GdlFunction func = (GdlFunction)p.getName();
				moves.add(func.getBody().get(1).toSentence());
			}
		}
		return Move.gdlToMoves(moves);
	}

	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
			throws TransitionDefinitionException {
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		SetBasePropositions(state);
		SetInputPropositions(state, moves);
		Propagate();
		
		for (Proposition p: propNet.getBasePropositions().values()) {
			if (p.getSingleInput().getValue()) {
				contents.add(p.getName().toSentence());
			}
		}
		return new MachineState(contents);
	}
	
	private void SetBasePropositions(MachineState state) {
		Set<GdlSentence> contents = state.getContents();

		for (Proposition p: propNet.getBasePropositions().values()) {
			if (contents.contains(p.getName().toSentence())) {
				p.setValue(true); 
			} else {
				p.setValue(false);
			}
		}
	}
	
	private void SetInputPropositions(MachineState state, List<Move> moves) {
		// Make a map of the player roles to each of the passed moves. This works under
		// the assumption that the passed moves and getRoles() are ordered in the same way.
		Map<GdlTerm, GdlTerm> roleToMoves = new HashMap<GdlTerm, GdlTerm>();
		for (int i = 0; i < moves.size(); i++) {
			GdlTerm roleTerm = getRoles().get(i).getName().toTerm();
			GdlTerm moveTerm = moves.get(i).getContents().toTerm();
			roleToMoves.put(roleTerm, moveTerm);
		}
		Map<GdlTerm, Proposition> inputPropositions = propNet.getInputPropositions();
		for (GdlTerm term: inputPropositions.keySet()) {
			GdlFunction func = (GdlFunction)term;
			List<GdlTerm> body = func.getBody();
			inputPropositions.get(term).setValue(roleToMoves.get(body.get(0)) == body.get(1));
		}
	}
	
	private void Propagate() {
		for (Proposition p: ordering) {
			p.setValue(p.getSingleInput().getValue());
		}		
	}

	// TODO: (Julius) We might not need to use this.
	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */	
	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName().toSentence());
			}

		}
		return new MachineState(contents);
	}
}
