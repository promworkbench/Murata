package org.processmining.plugins.petrinet.reduction;

import java.util.ArrayList;
import java.util.Map;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

/**
 * General output of Petri net reduction rules.
 * 
 * @author Eric Verbeek
 * @version 0.1
 */

public class MurataOutput {
	/*
	 * The reduced Petri net.
	 */
	private final Petrinet net;
	/*
	 * The marking of the reduced Petri net.
	 */
	private final Marking marking;
	/*
	 * The log of reduction actions.
	 */
	private final ArrayList<String> log;
	/**
	 * The mapping between transitions in the input net and transitions in the
	 * output net
	 */
	private Map<Transition, Transition> transitionMapping;
	/**
	 * The mapping between places in the input net and places in the output net
	 */
	private Map<Place, Place> placeMapping;

	/**
	 * Constructs a new MurataOutput, given the reduced Petri net and its
	 * marking.
	 * 
	 * @param net
	 *            The reduced Petri net.
	 * @param marking
	 *            The marking of the reduced Petri net.
	 */
	public MurataOutput(Petrinet net, Marking marking) {
		this.net = net;
		this.marking = marking;
		/*
		 * For now, the other fields have default values.
		 */
		setTransitionMapping(null);
		setPlaceMapping(null);
		log = new ArrayList<String>();
	}

	/**
	 * Gets the reduced Petri net.
	 * 
	 * @return The reduced Petri net.
	 */
	public Petrinet getNet() {
		return net;
	}

	/**
	 * Gets the marking of the reduced Petri net.
	 * 
	 * @return The marking of the reduced Petri net.
	 */
	public Marking getMarking() {
		return marking;
	}

	/**
	 * Gets the log. Note that a string S can be added to this log by calling
	 * something like "getLog().add(S)".
	 * 
	 * @return The log.
	 */
	public ArrayList<String> getLog() {
		return log;
	}

	public void setTransitionMapping(Map<Transition, Transition> transitionMapping) {
		this.transitionMapping = transitionMapping;
	}

	public Map<Transition, Transition> getTransitionMapping() {
		return transitionMapping;
	}

	/**
	 * Sets the transition mapping.
	 * 
	 * @param transitionMapping
	 *            the transition mapping from the input net to the output net
	 */
	public void setPlaceMapping(Map<Place, Place> placeMapping) {
		this.placeMapping = placeMapping;
	}

	/**
	 * Sets the place mapping.
	 * 
	 * @param placeMapping
	 *            the place mapping from the input net to the output net
	 */
	public Map<Place, Place> getPlaceMapping() {
		return placeMapping;
	}
}
