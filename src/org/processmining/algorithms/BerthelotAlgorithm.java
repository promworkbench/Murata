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
		 * Construct easy-lookup for arc weights.
		 */
		Map<Place, Integer> pMap = new HashMap<Place, Integer>();
		int ctr = 0;
		for (Place place : reducedPN.getPlaces()) {
			pMap.put(place,  ctr);
			ctr++;
		}
		Map<Transition, Integer> tMap = new HashMap<Transition, Integer>();
		ctr = 0;
		for (Transition transition : reducedPN.getTransitions()) {
			tMap.put(transition,  ctr);
			ctr++;
		}
		int[][] ptWeights = new int[reducedPN.getPlaces().size()][reducedPN.getTransitions().size()];
		for (int p = reducedPN.getPlaces().size() - 1; p >= 0; p--) {
			for (int t = reducedPN.getTransitions().size() - 1; t >= 0; t--) {
				ptWeights[p][t] = 0;
			}
		}
		int[][] tpWeights = new int[reducedPN.getTransitions().size()][reducedPN.getPlaces().size()];
		for (int t = reducedPN.getTransitions().size() - 1; t >= 0; t--) {
			for (int p = reducedPN.getPlaces().size() - 1; p >= 0; p--) {
				tpWeights[t][p] = 0;
			}
		}
		for (PetrinetEdge<?, ?> edge : reducedPN.getEdges()) {
			if (edge instanceof Arc) {
				Arc arc = (Arc) edge;
				if (arc.getSource() instanceof Place) {
					ptWeights[pMap.get(arc.getSource())][tMap.get(arc.getTarget())] = arc.getWeight();
				} else {
					tpWeights[tMap.get(arc.getSource())][pMap.get(arc.getTarget())] = arc.getWeight();
				}
			}
		}
		
		/*
		 * redudantPlaces holds the set of redundant places found so far. These
		 * places will be ignored when checking whether the candidate place is
		 * redundant.
		 */
		Set<Place> redundantPlaces = new HashSet<Place>();
		for (Place candidatePlace : reducedPN.getPlaces()) {
			parameters.displayMessage("[BerthelotAlgorithm] Place " + candidatePlace.getLabel());
			if (fastCheckFails(reducedPN, candidatePlace)) {
				continue;
			}

			/*
			 * Checking whether candidatePlace is structurally redundant. First
			 * create LPEngine, with a variable for every place and one
			 * additional variable for the initial marking.
			 */
			parameters.displayMessage("[BerthelotAlgorithm] Create engine");
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
			parameters.displayMessage("[BerthelotAlgorithm] First constraint");
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
			parameters.displayMessage("[BerthelotAlgorithm] Second constraint");
			for (Transition transition : reducedPN.getTransitions()) {
				constraint = new HashMap<Integer, Double>();
				for (Place place : reducedPN.getPlaces()) {
					if (!redundantPlaces.contains(place)) {
						//Arc arc = reducedPN.getArc(place, transition);
						int weight = ptWeights[pMap.get(place)][tMap.get(transition)]; //(arc == null ? 0 : arc.getWeight());
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
			parameters.displayMessage("[BerthelotAlgorithm] Third constraint");
			for (Transition transition : reducedPN.getTransitions()) {
				constraint = new HashMap<Integer, Double>();
				for (Place place : reducedPN.getPlaces()) {
					if (!redundantPlaces.contains(place)) {
						//Arc arc = reducedPN.getArc(transition, place);
						int weight = tpWeights[tMap.get(transition)][pMap.get(place)]; //(arc == null ? 0 : arc.getWeight());
						//arc = reducedPN.getArc(place, transition);
						weight -= ptWeights[pMap.get(place)][tMap.get(transition)]; //(arc == null ? 0 : arc.getWeight());
						constraint.put(placeIndex.get(place), (place == candidatePlace ? 1.0 : -1.0) * weight);
					}
				}
				engine.addConstraint(constraint, Operator.GREATER_EQUAL, 0.0);
			}

			/*
			 * Places cannot have a non-negative solution, and candidatePlace
			 * should have a positive solution.
			 */
			parameters.displayMessage("[BerthelotAlgorithm] Fourth constraint");
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

			parameters.displayMessage("[BerthelotAlgorithm] Solve");
			if (engine.isFeasible()) {
				/*
				 * Found a solution: candidatePlace is structurally redundant.
				 */
				//				System.out.println("Place " + candidatePlace + " is structurally redundant");
				redundantPlaces.add(candidatePlace);
			}
		}
		parameters.displayMessage("[BerthelotAlgorithm] Done");
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
					clonedNet.addArc(placeMap.get(arc.getSource()), transitionMap.get(arc.getTarget()),
							arc.getWeight());
				} else {
					clonedNet.addArc(transitionMap.get(arc.getSource()), placeMap.get(arc.getTarget()),
							arc.getWeight());
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
					for (PetrinetEdge<?, ?> outOutEdge : net.getOutEdges(outArc.getTarget())) {
						if (outOutEdge instanceof Arc) {
							Arc outOutArc = (Arc) outOutEdge;
							if (!outOutArc.getTarget().equals(place)) {
								return true;
							}
						}
					}
				}
			}
		}
		for (PetrinetEdge<?, ?> inEdge : net.getInEdges(place)) {
			if (inEdge instanceof Arc) {
				Arc inArc = (Arc) inEdge;
				if (net.getInEdges(inArc.getSource()).size() == 1) {
					for (PetrinetEdge<?, ?> inInEdge : net.getInEdges(inArc.getSource())) {
						if (inInEdge instanceof Arc) {
							Arc inInArc = (Arc) inInEdge;
							if (!inInArc.getSource().equals(place)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
}
