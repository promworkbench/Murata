package org.processmining.plugins.petrinet.reduction;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
		return reduce(net, sacredNodes, transitionMap, placeMap, marking, new MurataParameters());
	}

	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking,
			MurataParameters parameters) {
		Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> inputEdges = new HashMap<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>>();
		Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> outputEdges = new HashMap<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>>();
		for (PetrinetNode node : net.getNodes()) {
			inputEdges.put(node, new HashSet<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>());
			outputEdges.put(node, new HashSet<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>());
		}
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getEdges()) {
			outputEdges.get(edge.getSource()).add(edge);
			inputEdges.get(edge.getTarget()).add(edge);
		}

		return reduce(net, sacredNodes, transitionMap, placeMap, marking, inputEdges, outputEdges, new MurataParameters());
	}

	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking,
			Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> inputEdges, 
			Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> outputEdges, 
			MurataParameters parameters) {
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
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> preset = inputEdges.get(transition);
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
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = outputEdges.get(transition);
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
			 * If transition has inputs nor outputs, select all transitions as sibling transitions.
			 */
			Set<Transition> siblingTransitions = inputMap.keySet();
			if (!inputArcs.isEmpty()) {
				/*
				 * Transition has an input. Select sibling transitions as those transitions that share this input.
				 */
				siblingTransitions = new HashSet<Transition>();
				Place place = (Place) inputArcs.iterator().next().getSource();
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : outputEdges.get(place)) {
					if (edge instanceof Arc) {
						siblingTransitions.add((Transition) ((Arc) edge).getTarget());
					}
				}
			} else if (outputArcs.isEmpty()) {
				/*
				 * Transition has an output. Select sibling transitions as those transitions that share this output.
				 */
				siblingTransitions = new HashSet<Transition>();
				Place place = (Place) outputArcs.iterator().next().getTarget();
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : inputEdges.get(place)) {
					if (edge instanceof Arc) {
						siblingTransitions.add((Transition) ((Arc) edge).getSource());
					}
				}
			}
			
			/*
			 * Checking for matching transitions.
			 */
			for (Transition siblingTransition : siblingTransitions) {
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
					 * either the sibling or the transition, if allowed.
					 */
					if (!parameters.isAllowFPTSacredNode()) {
						/*
						 * Check whether a sacred nodes is involved.
						 */
						if (sacredNodes.contains(siblingTransition) || sacredNodes.contains(transition)) {
							/*
							 * Yes, it is. Not allowed. 
							 */
							continue;
						}
						/*
						 * No, it is not. Proceed to remove one.
						 */
					}
					if (!sacredNodes.contains(siblingTransition)) {
						if (!sacredNodes.contains(transition)) {
							String log = "<fpt siblingTransition=\"" + siblingTransition.getLabel() + "\"/>";
							/*
							 * The sibling is not sacred. Remove it. First,
							 * update the transition map.
							 */
							for (Transition t : transitionMap.keySet()) {
								if (transitionMap.get(t) == siblingTransition) {
									transitionMap.put(t, transition);
								}
								net.removeTransition(siblingTransition);
							}
							return log; // The sibling has been removed.
						}
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
