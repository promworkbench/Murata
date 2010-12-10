package org.processmining.plugins.petrinet.reduction;

import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

public class MurataUtils {
	/**
	 * Update the label of the given arc. Note that the arc weight is displayed
	 * on the arc, hence after changing the arc weight the label needs to be
	 * updated.
	 * 
	 * @param arc
	 *            The given arc.
	 */
	public static void updateLabel(Arc arc) {
		arc.getAttributeMap().put(AttributeMap.LABEL,
				arc.getSource().getLabel() + " -[" + arc.getWeight() + "]-> " + arc.getTarget().getLabel());
	}

	/**
	 * Update the label of the given place given the current marking. Added in
	 * case the marking needs to be inserted for some reason in the label of a
	 * place.
	 * 
	 * @param place
	 *            The given place.
	 * @param marking
	 *            The current marking.
	 */
	public static void updateLabel(Place place, Marking marking) {
		place.getAttributeMap().put(AttributeMap.LABEL, place.getLabel());
	}

	/**
	 * Adds an arc with given source, target, and weight to the given net.
	 * However, if an arc from source to target already exists, the weight is
	 * added to the weight of this existing arc instead. This way, we enforce
	 * that there is only one regular arc from source to target.
	 * 
	 * @param net
	 *            The given net.
	 * @param source
	 *            The given source.
	 * @param target
	 *            The given target.
	 * @param weight
	 *            The given weight.
	 */
	public static void addArc(Petrinet net, PetrinetNode source, PetrinetNode target, int weight) {
		Arc arc = net.getArc(source, target);
		if (arc == null) {
			/*
			 * No arc yet. Add one with proper weight.
			 */
			if (source instanceof Place) {
				arc = net.addArc((Place) source, (Transition) target);
			} else {
				arc = net.addArc((Transition) source, (Place) target);
			}
			arc.setWeight(weight);
			updateLabel(arc);
		} else {
			/*
			 * An arc already exists (note that there can be at most one arc).
			 * Increment weight.
			 */
			weight += arc.getWeight();
			arc.setWeight(weight);
			updateLabel(arc);
		}
	}

	/**
	 * Remove all tokens from the given place in the given marking.
	 * 
	 * @param marking
	 *            The given marking.
	 * @param place
	 *            The given place.
	 */
	public static void resetPlace(Marking marking, Place place) {
		Marking placeMarking = new Marking();
		placeMarking.add(place, marking.occurrences(place));
		marking.removeAll(placeMarking);
		updateLabel(place, marking);
	}

}
