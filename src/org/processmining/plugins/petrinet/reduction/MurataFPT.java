package org.processmining.plugins.petrinet.reduction;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

public class MurataFPT extends MurataRule {

	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking) {
		HashMap<Transition, HashSet<Arc>> inputMap = new HashMap<Transition, HashSet<Arc>>();
		HashMap<Transition, HashSet<Arc>> outputMap = new HashMap<Transition, HashSet<Arc>>();
		/*
		 * Iterate over all transitions. Build inputMap and outputMap if all
		 * incident edges regular.
		 */
		for (Transition transition : net.getTransitions()) {
			boolean ok;
			/*
			 * Get input edges. Should all be regular.
			 */
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> preset = net
					.getInEdges(transition);
			HashSet<Arc> inputArcs = new HashSet<Arc>();
			ok = true;
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : preset) {
				if (edge instanceof Arc) {
					inputArcs.add((Arc) edge);
				} else {
					ok = false;
				}
			}
			if (!ok) {
				continue;
			}
			/*
			 * Get output edges. Should all be regular.
			 */
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = net
					.getOutEdges(transition);
			HashSet<Arc> outputArcs = new HashSet<Arc>();
			ok = true;
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : postset) {
				if (edge instanceof Arc) {
					outputArcs.add((Arc) edge);
				} else {
					ok = false;
				}
			}
			if (!ok) {
				continue;
			}
			inputMap.put(transition, inputArcs);
			outputMap.put(transition, outputArcs);
		}
		/*
		 * Iterate over all transitions with only regular incident edges.
		 */
		for (Transition transition : inputMap.keySet()) {
			HashSet<Arc> inputArcs = inputMap.get(transition);
			HashSet<Arc> outputArcs = outputMap.get(transition);
			/*
			 * Checking for matching transitions.
			 */
			for (Transition siblingTransition : inputMap.keySet()) {
				if (siblingTransition == transition) {
					continue;
				}
				HashSet<Arc> siblingInputArcs = inputMap.get(siblingTransition);
				HashSet<Arc> siblingOutputArcs = outputMap.get(siblingTransition);
				if (siblingInputArcs.size() != inputArcs.size()) {
					continue;
				}
				if (siblingOutputArcs.size() != outputArcs.size()) {
					continue;
				}
				boolean equal = true;
				boolean found;
				for (Arc arc : inputArcs) {
					if (equal) {
						found = false;
						for (Arc siblingArc : siblingInputArcs) {
							if ((arc.getSource() == siblingArc.getSource())
									&& (arc.getWeight() == siblingArc.getWeight())) {
								found = true;
							}
						}
						if (!found) {
							equal = false;
						}
					}
				}
				for (Arc arc : outputArcs) {
					if (equal) {
						found = false;
						for (Arc siblingArc : siblingOutputArcs) {
							if ((arc.getTarget() == siblingArc.getTarget())
									&& (arc.getWeight() == siblingArc.getWeight())) {
								found = true;
							}
						}
						if (!found) {
							equal = false;
						}
					}
				}
				if (equal) {
					/*
					 * Found a sibling with identical inputs and outputs. Remove
					 * either the sibling or the transition.
					 */
					if (!sacredNodes.contains(siblingTransition)) {
						String log = "<fpt siblingTransition=\"" + siblingTransition.getLabel() + "\"/>";
						/*
						 * The sibling is not sacred. Remove it. First, update
						 * the transition map.
						 */
						for (Transition t : transitionMap.keySet()) {
							if (transitionMap.get(t) == siblingTransition) {
								transitionMap.put(t, transition);
							}
							net.removeTransition(siblingTransition);
						}
						return log; // The sibling has been removed.
					} else if (!sacredNodes.contains(transition)) {
						String log = "<fpt transition=\"" + transition.getLabel() + "\"/>";
						/*
						 * The place is not sacred. Remove it. First, update the
						 * transition map.
						 */
						for (Transition t : transitionMap.keySet()) {
							if (transitionMap.get(t) == transition) {
								transitionMap.put(t, siblingTransition);
							}
							net.removeTransition(transition);
						}
						return log; // The transition has been removed.
					}
				}
			}
		}
		return null;
	}

}
