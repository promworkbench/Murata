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

public class MurataESP extends MurataRule {

	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking) {
		return reduce(net, sacredNodes, transitionMap, placeMap, marking, new MurataParameters());
	}
	
	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking, MurataParameters parameters) {
		/*
		 * Iterate over all places.
		 */
		for (Place place : net.getPlaces()) {
			if (sacredNodes.contains(place)) {
				continue;
			}
			long tokens = marking.occurrences(place);
			/*
			 * Check input arc.
			 */
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> preset = net.getInEdges(place);
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
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = net.getOutEdges(place);
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
			 * Check whether tokens exceed weight.
			 */
			if (weight <= tokens) {
				String log = "<elp place=\"" + place.getLabel() + "\"/>";
				/*
				 * We have a self loop for a marked place. Remove the place from
				 * the copy net. First, update the place map.
				 */
				HashSet<Place> removePlaces = new HashSet<Place>();
				for (Place p : placeMap.keySet()) {
					if (placeMap.get(p) == place) {
						removePlaces.add(p);
					}
				}
				for (Place p : removePlaces) {
					placeMap.remove(p);
				}
				MurataUtils.resetPlace(marking, place);
				net.removePlace(place);
				return log; // A place has been removed.
			}
		}
		return null;
	}

}
