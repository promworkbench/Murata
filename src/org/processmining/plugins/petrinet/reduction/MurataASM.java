package org.processmining.plugins.petrinet.reduction;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

public class MurataASM extends MurataRule {

	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking) {
		for (Transition transition : net.getTransitions()) {
			if (sacredNodes.contains(transition)) {
				continue;
			}
			if (!transition.isInvisible()) {
				continue;
			}
			if (net.getInEdges(transition).size() != 1) {
				continue;
			}
			if (net.getOutEdges(transition).size() != 1) {
				continue;
			}
			Place sourcePlace = (Place) net.getInEdges(transition).iterator().next().getSource();
			Place targetPlace = (Place) net.getOutEdges(transition).iterator().next().getTarget();
			Set<Place> places = new HashSet<Place>();
			places.add(sourcePlace);
			String result = reduce(net, transitionMap, sourcePlace, places, sourcePlace, targetPlace, transition);
			places.remove(sourcePlace);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private String reduce(Petrinet net, HashMap<Transition, Transition> transitionMap, Place currentPlace, Set<Place> places, Place firstPlace,
			Place lastPlace, Transition silentTransition) {
		for (PetrinetEdge<?, ?> edge : net.getOutEdges(currentPlace)) {
			if (!(edge instanceof Arc)) {
				continue;
			}
			Transition transition = (Transition) edge.getTarget();
			if (!transition.isInvisible()) {
				continue;
			}
			if (net.getInEdges(transition).size() != 1) {
				continue;
			}
			if (net.getOutEdges(transition).size() != 1) {
				continue;
			}
			Place place = (Place) net.getOutEdges(transition).iterator().next().getTarget();
			if (places.contains(place)) {
				continue;
			}
			if (currentPlace != firstPlace && place == lastPlace) {
				return reduce(net, transitionMap, silentTransition);
			}
			places.add(place);
			String result = reduce(net, transitionMap, place, places, firstPlace, lastPlace, silentTransition);
			places.remove(place);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
	
	private String reduce(Petrinet net, HashMap<Transition, Transition> transitionMap, Transition removeTransition) {
		Set<Transition> removeTransitions = new HashSet<Transition>();
		System.out.println("[MurataASM] Remove " + removeTransition.getLabel() + ".");
		for (Transition transition : transitionMap.keySet()) {
			if (transitionMap.get(transition) == removeTransition) {
				removeTransitions.add(transition);
			}
		}
		for (Transition transition : removeTransitions) {
			transitionMap.remove(transition);
		}
		net.removeEdge(net.getInEdges(removeTransition).iterator().next());
		net.removeEdge(net.getOutEdges(removeTransition).iterator().next());
		net.removeTransition(removeTransition);
		return "<asm transition=\"" + removeTransition.getLabel() + "\"/>";
	}
}
