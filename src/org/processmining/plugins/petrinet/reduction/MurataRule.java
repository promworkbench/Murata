package org.processmining.plugins.petrinet.reduction;

import java.util.Collection;
import java.util.HashMap;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

public abstract class MurataRule {
	public abstract String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking);
}
