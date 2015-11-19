package org.processmining.algorithms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.lpengines.factories.LPEngineFactory;
import org.processmining.lpengines.interfaces.LPEngine;
import org.processmining.lpengines.interfaces.LPEngine.EngineType;
import org.processmining.lpengines.interfaces.LPEngine.Operator;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.parameters.BerthelotParameters;

public class BerthelotAlgorithm {

	public Petrinet apply(PluginContext context, Petrinet pn, BerthelotParameters parameters) {
		Map<Place, Place> placeMap = new HashMap<Place, Place>();
		Map<Transition, Transition> transitionMap = new HashMap<Transition, Transition>();
		Petrinet reducedPN = cloneNet(pn, placeMap, transitionMap);
		Marking initialMarking = apply(parameters.getInitialMarking(), placeMap);
		Set<Marking> finalMarkings = apply(parameters.getFinalMarkings(), placeMap);

		/*
		 * redudantPlaces holds the set of redundant places found so far. These
		 * places will be ignored when checking whether the candidate place is
		 * redundant.
		 */
		Set<Place> redundantPlaces = new HashSet<Place>();
		for (Place candidatePlace : reducedPN.getPlaces()) {
			if (fastCheckFails(reducedPN, candidatePlace)) {
				continue;
			}
			/*
			 * Checking whether candidatePlace is structurally redundant. First
			 * create LPEngine, with a variable for every place and one
			 * additional variable for the initial marking.
			 */
			Map<Place, Integer> placeIndex = new HashMap<Place, Integer>();
			int mIndex;
			LPEngine engine = LPEngineFactory.createLPEngine(EngineType.LPSOLVE, 0, 0);
			for (Place place : reducedPN.getPlaces()) {
				if (!redundantPlaces.contains(place)) {
					placeIndex.put(place,
							engine.addVariable(new HashMap<Integer, Double>(), LPEngine.VariableType.INTEGER));
				}
			}
			//			System.out.println(placeIndex);
			mIndex = engine.addVariable(new HashMap<Integer, Double>(), LPEngine.VariableType.INTEGER);

			/*
			 * Let Q be the set of places in the net excluding the
			 * candidatePlace p. Let I be the 'selected' subset of Q, that is, I
			 * = { i in Q | V(q) > 0 }. Note that in the definitions below we
			 * have replaced I by Q, as V(q) = 0 if q not in I.
			 * 
			 * Let Add the first constraint: for the initial marking M0, p has a
			 * weighted marking grater than the sum of weighted marking of
			 * places belonging to I.
			 * 
			 * V(p).M0(p) - Sum_{q in Q}{V(q).M0(q) - bM0 = 0
			 */
			Map<Integer, Double> constraint = new HashMap<Integer, Double>();
			for (Place place : reducedPN.getPlaces()) {
				if (!redundantPlaces.contains(place)) {
					constraint.put(placeIndex.get(place),
							(place == candidatePlace ? 1.0 : -1.0) * initialMarking.occurrences(place));
				}
			}
			constraint.put(mIndex, -1.0);
			engine.addConstraint(constraint, Operator.EQUAL, 0.0);

			/*
			 * Add the second constraint: the difference between the weighted
			 * marking of p and those of places belonging to I necessary to give
			 * concession to t must be less than or equal to this difference in
			 * the initial marking (bM0).
			 * 
			 * Let T be the set of transitions in the net. Let W(m,n) denote the
			 * arc weight of the arc from node m to node n. If there is no such
			 * arc, W(m,n) = 0.
			 * 
			 * For all t in T: V(p).W(p,t) - Sum_{q in Q}{V(q).W(q.t) - bM0 <= 0
			 */
			for (Transition transition : reducedPN.getTransitions()) {
				constraint = new HashMap<Integer, Double>();
				for (Place place : reducedPN.getPlaces()) {
					if (!redundantPlaces.contains(place)) {
						Arc arc = reducedPN.getArc(place, transition);
						int weight = (arc == null ? 0 : arc.getWeight());
						constraint.put(placeIndex.get(place), (place == candidatePlace ? 1.0 : -1.0) * weight);
					}
				}
				constraint.put(mIndex, -1.0);
				engine.addConstraint(constraint, Operator.LESS_EQUAL, 0.0);
			}

			/*
			 * Add the third constraint: when a transition t occurs, the growth
			 * of the weighted marking of p is greater than that one of I.
			 * 
			 * Let C(m,n) = W(n,m) - W(m,n), that is C(m,n) is the net effect on
			 * place m of firing transition n.
			 * 
			 * For all t in T: V(p).C(p,t) - Sum_{q in Q}{V(q).C(q,t) >= 0
			 */
			for (Transition transition : reducedPN.getTransitions()) {
				constraint = new HashMap<Integer, Double>();
				for (Place place : reducedPN.getPlaces()) {
					if (!redundantPlaces.contains(place)) {
						Arc arc = reducedPN.getArc(transition, place);
						int weight = (arc == null ? 0 : arc.getWeight());
						arc = reducedPN.getArc(place, transition);
						weight -= (arc == null ? 0 : arc.getWeight());
						constraint.put(placeIndex.get(place), (place == candidatePlace ? 1.0 : -1.0) * weight);
					}
				}
				engine.addConstraint(constraint, Operator.GREATER_EQUAL, 0.0);
			}

			/*
			 * Places cannot have a non-negative solution, and candidatePlace
			 * should have a positive solution.
			 */
			for (Place place : reducedPN.getPlaces()) {
				if (!redundantPlaces.contains(place)) {
					constraint = new HashMap<Integer, Double>();
					constraint.put(placeIndex.get(place), 1.0);
					if (place == candidatePlace) {
						engine.addConstraint(constraint, Operator.GREATER_EQUAL, 1.0);
					} else {
						engine.addConstraint(constraint, Operator.GREATER_EQUAL, 0.0);
					}
				}
			}
			/*
			 * bM0 should be at least 0.
			 */
			constraint = new HashMap<Integer, Double>();
			constraint.put(mIndex, 1.0);
			engine.addConstraint(constraint, Operator.GREATER_EQUAL, 0.0);

			//			engine.print();

			if (engine.isFeasible()) {
				/*
				 * Found a solution: candidatePlace is structurally redundant.
				 */
				//				System.out.println("Place " + candidatePlace + " is structurally redundant");
				redundantPlaces.add(candidatePlace);
			}
		}
		/*
		 * Now remove all redundant places from the net.
		 */
		//		System.out.println("Redundant = " + redundantPlaces);
		for (Place redundantPlace : redundantPlaces) {
			boolean remove = true;
			if (reducedPN.getOutEdges(redundantPlace).isEmpty()) {
				/*
				 * Sink place, special case. Do not remove last place.
				 */
				for (PetrinetEdge<?, ?> edge : reducedPN.getInEdges(redundantPlace)) {
					if (edge instanceof Arc) {
						Arc arc = (Arc) edge;
						if (reducedPN.getOutEdges(arc.getSource()).size() == 1) {
							remove = false;
						}
					}
				}
			}
			if (remove) {
				reducedPN.removePlace(redundantPlace);
				while (initialMarking.contains(redundantPlace)) {
					initialMarking.remove(redundantPlace);
				}
				Set<Marking> newFinalMarkings = new HashSet<Marking>();
				for (Marking finalMarking : finalMarkings) {
					Marking newFinalMarking = new Marking(finalMarking);
					while (newFinalMarking.contains(redundantPlace)) {
						newFinalMarking.remove(redundantPlace);
					}
					newFinalMarkings.add(newFinalMarking);
				}
				finalMarkings = newFinalMarkings;
			}
		}

		parameters.setInitialBerthelotMarking(initialMarking);
		parameters.setFinalBerthelotMarkings(finalMarkings);
		if (context != null) {
			context.getConnectionManager().addConnection(new InitialMarkingConnection(reducedPN, initialMarking));
			for (Marking finalMarking : finalMarkings) {
				context.getConnectionManager().addConnection(new FinalMarkingConnection(reducedPN, finalMarking));
			}
		}
		return reducedPN;
	}

	private Petrinet cloneNet(Petrinet net, Map<Place, Place> placeMap, Map<Transition, Transition> transitionMap) {
		Petrinet clonedNet = PetrinetFactory.newPetrinet(net.getLabel());
		for (Place place : net.getPlaces()) {
			Place clonedPlace = clonedNet.addPlace(place.getLabel());
			placeMap.put(place, clonedPlace);
		}
		for (Transition transition : net.getTransitions()) {
			Transition clonedTransition = clonedNet.addTransition(transition.getLabel());
			clonedTransition.setInvisible(transition.isInvisible());
			transitionMap.put(transition, clonedTransition);
		}
		for (PetrinetEdge<?, ?> edge : net.getEdges()) {
			if (edge instanceof Arc) {
				Arc arc = (Arc) edge;
				if (placeMap.containsKey(arc.getSource())) {
					clonedNet
							.addArc(placeMap.get(arc.getSource()), transitionMap.get(arc.getTarget()), arc.getWeight());
				} else {
					clonedNet
							.addArc(transitionMap.get(arc.getSource()), placeMap.get(arc.getTarget()), arc.getWeight());
				}
			}
		}
		return clonedNet;
	}

	private Marking apply(Marking marking, Map<Place, Place> map) {
		Marking appliedMarking = new Marking();
		for (Place place : marking.baseSet()) {
			appliedMarking.add(map.get(place), marking.occurrences(place));
		}
		return appliedMarking;
	}

	private Set<Marking> apply(Set<Marking> markings, Map<Place, Place> map) {
		Set<Marking> appliedMarkings = new HashSet<Marking>();
		for (Marking marking : markings) {
			appliedMarkings.add(apply(marking, map));
		}
		return appliedMarkings;
	}
	
	private boolean fastCheckFails(Petrinet net, Place place) {
		for (PetrinetEdge<?, ?> outEdge : net.getOutEdges(place)) {
			if (outEdge instanceof Arc) {
				Arc outArc = (Arc) outEdge;
				if (net.getInEdges(outArc.getTarget()).size() == 1) {
					/*
					 * One of the transitions in the postset of this place has only this place as input.
					 * As a result, this place cannot be structurally redundant.
					 */
					return true;
				}
			}
		}
		return false;
	}
}
