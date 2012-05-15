package player.gamer.statemachine.cs227b;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import util.gdl.grammar.Gdl;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
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
	    // List to contain the topological ordering.
	    List<Proposition> order = new LinkedList<Proposition>();
	    				
		// All of the components in the PropNet
		List<Component> components = new ArrayList<Component>(propNet.getComponents());
		
		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());
		
		List<Proposition> basePropositions = new ArrayList<Proposition>(propNet.getBasePropositions());
		
		while (!propositions.isEmpty()) {
			for ( Proposition proposition : propositions ) {
				boolean inputsSatisfied = true;
				for ( Component input : proposition.getInputs() )
				{
					if ( components.contains(input) )
					{
						inputsSatisfied = false;
						break;
					}
				}
				if (inputsSatisfied) { 
					order.add(proposition);
					propositions.remove(proposition);
					components.remove(proposition);
				}
			}
			
			for ( Component component : components ) {
				boolean inputsSatisfied = true;
				for ( Component input : component.getInputs() )
				{
					if ( components.contains(input) )
					{
						inputsSatisfied = false;
						break;
					}
				}
				if (inputsSatisfied) {
					components.remove(component);
				}
			}
		}
		
		return order;
	}
	
	@Override
	public int getGoal(MachineState state, Role role)
			throws GoalDefinitionException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isTerminal(MachineState state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Role> getRoles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MachineState getInitialState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
			throws MoveDefinitionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
			throws TransitionDefinitionException {
		// TODO Auto-generated method stub
		return null;
	}

}
