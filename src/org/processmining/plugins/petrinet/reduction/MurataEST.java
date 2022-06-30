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

public class MurataEST extends MurataRule {

	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking) {
		return reduce(net, sacredNodes, transitionMap, placeMap, marking, new MurataParameters());
	}
	
	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking, MurataParameters parameters) {
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

		/*
		 * Iterate over all transitions.
		 */
		for (Transition transition : net.getTransitions()) {
			if (sacredNodes.contains(transition)) {
				continue;
			}
			/*
			 * Check input arc.
			 */
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> preset = inputEdges.get(transition);
			if (preset.size() != 1) {
				continue;
			}
			PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge = preset.iterator().next();
			if (!(edge instanceof Arc)) {
				continue;
			}
			Arc inputArc = (Arc) edge;
			int weight = inputArc.getWeight();
			/*
			 * Check output arc.
			 */
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = outputEdges.get(transition);
			if (postset.size() != 1) {
				continue;
			}
			edge = postset.iterator().next();
			if (!(edge instanceof Arc)) {
				continue;
			}
			Arc outputArc = (Arc) edge;
			if (outputArc.getWeight() != weight) {
				continue;
			}
			/*
			 * Check whether self loop.
			 */
			if (inputArc.getSource() != outputArc.getTarget()) {
				continue;
			}
			/*
			 * Check whether place has other output transitions that needs at
			 * least as much tokens as this transition.
			 */
			Place place = (Place) inputArc.getSource();
			postset = outputEdges.get(place);
			if (postset.size() < 2) {
				continue;
			}
			boolean ok = false;
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> postEdge : postset) {
				if (ok) {
					continue;
				}
				if (!(postEdge instanceof Arc)) {
					continue;
				}
				Arc arc = (Arc) postEdge;
				if (arc == inputArc) {
					continue;
				}
				if (arc.getWeight() >= weight) {
					ok = true;
				}
			}
			if (ok) {
				String log = "<elt transition=\"" + transition.getLabel() + "\"/>";
				/*
				 * We have a self loop for a transition. Remove the transition.
				 * First, update the transition map.
				 */
				HashSet<Transition> removeTransitions = new HashSet<Transition>();
				for (Transition t : transitionMap.keySet()) {
					if (transitionMap.get(t) == transition) {
						removeTransitions.add(t);
					}
				}
				for (Transition t : removeTransitions) {
					transitionMap.remove(t);
				}
				net.removeTransition(transition);
				return log; // A transition has been removed.
			}
		}
		return null;
	}

}
