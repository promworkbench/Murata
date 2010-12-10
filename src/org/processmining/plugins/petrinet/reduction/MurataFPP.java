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

public class MurataFPP extends MurataRule {

	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking) {
		HashMap<Place, HashSet<Arc>> inputMap = new HashMap<Place, HashSet<Arc>>();
		HashMap<Place, HashSet<Arc>> outputMap = new HashMap<Place, HashSet<Arc>>();
		/*
		 * Iterate over all places. Build inputMap and outputMap if all incident
		 * edges regular.
		 */
		for (Place place : net.getPlaces()) {
			boolean ok;
			/*
			 * Get input edges. Should all be regular.
			 */
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> preset = net.getInEdges(place);
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
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = net.getOutEdges(place);
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
			inputMap.put(place, inputArcs);
			outputMap.put(place, outputArcs);
		}
		/*
		 * Iterate over all places with only regular incident edges.
		 */
		for (Place place : inputMap.keySet()) {
			HashSet<Arc> inputArcs = inputMap.get(place);
			HashSet<Arc> outputArcs = outputMap.get(place);
			/*
			 * Checking for matching transitions.
			 */
			for (Place siblingPlace : inputMap.keySet()) {
				if (siblingPlace == place) {
					continue;
				}
				HashSet<Arc> siblingInputArcs = inputMap.get(siblingPlace);
				HashSet<Arc> siblingOutputArcs = outputMap.get(siblingPlace);
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
					 * either the sibling or the place.
					 */
					if (!sacredNodes.contains(siblingPlace)
							&& (marking.occurrences(siblingPlace) >= marking.occurrences(place))) {
						String log = "<fpp siblingPlace=\"" + siblingPlace.getLabel() + "\"/>";
						/*
						 * Sibling is not sacred. remove it. First, update the
						 * place map.
						 */
						for (Place p : placeMap.keySet()) {
							if (placeMap.get(p) == siblingPlace) {
								placeMap.put(p, place);
							}
							MurataUtils.resetPlace(marking, siblingPlace);
							net.removePlace(siblingPlace);
						}
						return log; // The sibling has been removed.
					} else if (!sacredNodes.contains(place)
							&& (marking.occurrences(place) >= marking.occurrences(siblingPlace))) {
						String log = "<fpp place=\"" + place.getLabel() + "\"/>";
						/*
						 * Place is not sacred. Remove it. First, update the
						 * place map.
						 */
						for (Place p : placeMap.keySet()) {
							if (placeMap.get(p) == place) {
								placeMap.put(p, siblingPlace);
							}
							MurataUtils.resetPlace(marking, place);
							net.removePlace(place);
						}
						return log; // The place has been removed.
					}
					/*
					 * Both are sacred. Leave them.
					 */
				}
			}
		}
		return null;
	}

}
