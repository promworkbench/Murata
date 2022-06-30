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

public class MurataASM extends MurataRule {

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
		return reduce(net, sacredNodes, transitionMap, placeMap, marking, inputEdges, outputEdges,
				new MurataParameters());
	}

	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking,
			Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> inputEdges,
			Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> outputEdges,
			MurataParameters parameters) {
		//		System.out.println("[MurataASM] Start");
		for (Transition transition : net.getTransitions()) {
			if (sacredNodes.contains(transition)) {
				continue;
			}
			if (!transition.isInvisible()) {
				continue;
			}
			if (inputEdges.get(transition).size() != 1) {
				continue;
			}
			if (outputEdges.get(transition).size() != 1) {
				continue;
			}
			//			System.out.println("[MurataASM] Transition " + transition.getLabel());
			Place sourcePlace = (Place) inputEdges.get(transition).iterator().next().getSource();
			Place targetPlace = (Place) outputEdges.get(transition).iterator().next().getTarget();
			Set<Place> places = new HashSet<Place>();
			places.add(sourcePlace);
			String result = reduce(net, transitionMap, sourcePlace, places, sourcePlace, targetPlace, transition,
					inputEdges, outputEdges);
			places.remove(sourcePlace);
			if (result != null) {
				//				System.out.println("[MurataASM] End");
				return result;
			}
		}
		//		System.out.println("[MurataASM] End");
		return null;
	}

	private String reduce(Petrinet net, HashMap<Transition, Transition> transitionMap, Place currentPlace,
			Set<Place> places, Place firstPlace, Place lastPlace,
			Transition silentTransition,
			Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> inputEdges,
			Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> outputEdges) {
		for (PetrinetEdge<?, ?> edge : net.getOutEdges(currentPlace)) {
			if (!(edge instanceof Arc)) {
				continue;
			}
			Transition transition = (Transition) edge.getTarget();
			if (!transition.isInvisible()) {
				continue;
			}
			if (inputEdges.get(transition).size() != 1) {
				continue;
			}
			if (outputEdges.get(transition).size() != 1) {
				continue;
			}
			Place place = (Place) outputEdges.get(transition).iterator().next().getTarget();
			if (places.contains(place)) {
				continue;
			}
			if (place == lastPlace) {
				if (currentPlace != firstPlace) {
					return reduce(net, transitionMap, silentTransition, inputEdges, outputEdges);
				}
				continue;
			}
			places.add(place);
			String result = reduce(net, transitionMap, place, places, firstPlace, lastPlace, silentTransition,
					inputEdges, outputEdges);
			places.remove(place);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private String reduce(Petrinet net, HashMap<Transition, Transition> transitionMap, Transition removeTransition,
			Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> inputEdges,
			Map<PetrinetNode, Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>> outputEdges) {
		Set<Transition> removeTransitions = new HashSet<Transition>();
		//		System.out.println("[MurataASM] Remove " + removeTransition.getLabel() + ".");
		for (Transition transition : transitionMap.keySet()) {
			if (transitionMap.get(transition) == removeTransition) {
				removeTransitions.add(transition);
			}
		}
		for (Transition transition : removeTransitions) {
			transitionMap.remove(transition);
		}
		net.removeEdge(inputEdges.get(removeTransition).iterator().next());
		net.removeEdge(outputEdges.get(removeTransition).iterator().next());
		net.removeTransition(removeTransition);
		return "<asm transition=\"" + removeTransition.getLabel() + "\"/>";
	}
}
