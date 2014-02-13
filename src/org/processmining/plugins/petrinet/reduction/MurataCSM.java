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

public class MurataCSM extends MurataRule {

	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking) {
		for (Place place : net.getPlaces()) {
			Set<Place> places = new HashSet<Place>();
			Set<Transition> transitions = new HashSet<Transition>();
			if (sacredNodes.contains(place)) {
				continue;
			}
			String result = reduce(net, sacredNodes, transitionMap, placeMap, marking, place, place, places,
					transitions);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			Map<Transition, Transition> transitionMap, Map<Place, Place> placeMap, Marking marking,
			Place firstPlace, Place lastPlace, Set<Place> places, Set<Transition> transitions) {
		Collection<PetrinetEdge<?, ?>> edges = net.getOutEdges(lastPlace);
		for (PetrinetEdge<?, ?> edge : edges) {
			if (!(edge instanceof Arc)) {
				continue;
			}
			Transition transition = (Transition) edge.getTarget();
			if (!transition.isInvisible()) {
				continue;
			}
			if (transitions.contains(transition)) {
				continue;
			}
			if (sacredNodes.contains(transition)) {
				continue;
			}
			if (net.getInEdges(transition).size() != 1) {
				continue;
			}
			if (net.getOutEdges(transition).size() != 1) {
				continue;
			}
			PetrinetEdge<?, ?> otherEdge = net.getOutEdges(transition).iterator().next();
			if (!(otherEdge instanceof Arc)) {
				continue;
			}
			Place otherPlace = (Place) otherEdge.getTarget();
			if (sacredNodes.contains(otherPlace)) {
				continue;
			}
			if (otherPlace == firstPlace) {
				// We're round. Found a cycle of invisible transitions.
				transitions.add(transition);
				return reduce(net, transitionMap, placeMap, marking, firstPlace, places, transitions);
			}
			if (places.contains(otherPlace)) {
				continue;
			}
			places.add(otherPlace);
			transitions.add(transition);
			String result = reduce(net, sacredNodes, transitionMap, placeMap, marking, firstPlace, otherPlace, places,
					transitions);
			transitions.remove(transition);
			places.remove(otherPlace);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private String reduce(Petrinet net, Map<Transition, Transition> transitionMap, Map<Place, Place> placeMap,
			Marking marking, Place firstPlace, Set<Place> places, Set<Transition> transitions) {
		Set<Transition> removeTransitions = new HashSet<Transition>();
		for (Transition transition : transitionMap.keySet()) {
			if (transitions.contains(transitionMap.get(transition))) {
				removeTransitions.add(transition);
			}
		}
		for (Transition transition : removeTransitions) {
			transitionMap.remove(transition);
		}
		Set<Place> removePlaces = new HashSet<Place>();
		for (Place place : placeMap.keySet()) {
			if (places.contains(placeMap.get(place))) {
				removePlaces.add(place);
			}
		}
		for (Place place : removePlaces) {
			marking.add(firstPlace, marking.occurrences(place));
			MurataUtils.updateLabel(firstPlace, marking);
			MurataUtils.resetPlace(marking, place);
			placeMap.put(place, firstPlace);
		}
		Set<PetrinetEdge<?, ?>> removeEdges = new HashSet<PetrinetEdge<?, ?>>();
		Set<Transition> targetTransitions = new HashSet<Transition>();
		Set<Transition> sourceTransitions = new HashSet<Transition>();
		for (PetrinetEdge<?, ?> edge : net.getEdges()) {
			if (!(edge instanceof Arc)) {
				continue;
			}
			if (transitions.contains(edge.getSource())) {
				removeEdges.add(edge);
			} else if (transitions.contains(edge.getTarget())) {
				removeEdges.add(edge);
			} else if (places.contains(edge.getSource())) {
				targetTransitions.add((Transition) edge.getTarget());
				removeEdges.add(edge);
			} else if (places.contains(edge.getTarget())) {
				sourceTransitions.add((Transition) edge.getSource());
				removeEdges.add(edge);
			}
		}
		for (PetrinetEdge<?, ?> edge : removeEdges) {
			net.removeEdge(edge);
		}
		for (Transition transition : targetTransitions) {
			net.addArc(firstPlace, transition);
		}
		for (Transition transition : sourceTransitions) {
			net.addArc(transition, firstPlace);
		}
		String result = "<csm place=\"" + firstPlace.getLabel() + "\" transitions=\"";
		String sep = "{";
		for (Transition transition : removeTransitions) {
			result += sep + transition.getLabel();
			sep = ",";
			net.removeTransition(transition);
		}
		result += "}\" places=\"";
		sep = "{";
		for (Place place : removePlaces) {
			result += sep + place.getLabel();
			sep = ",";
			net.removePlace(place);
		}
		result += "}\"/>";
		return result;
	}
}
