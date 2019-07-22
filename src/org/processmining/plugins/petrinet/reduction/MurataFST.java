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

public class MurataFST extends MurataRule {
	/**
	 * Apply the FST rule once, if possible.
	 * 
	 * @param net
	 *            The net to check the FST rule on.
	 * @param sacredNodes
	 *            Nodes in the given net to leave alone.
	 * @param transitionMap
	 *            Map from original transitions to reduced transitions.
	 * @param placeMap
	 *            Map from original places to reduced places.
	 * @param marking
	 *            Current marking of the given net.
	 * @return null if FST rule could not be applied. Otherwise short
	 *         description of how the rule was applied.
	 */
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
			/*
			 * Check whether the place is sacred. Should not be.
			 */
			if (sacredNodes.contains(place)) {
				continue;
			}
			/*
			 * Check the input arc. There should be only one and it should be
			 * regular.
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
			 * Get the input transition. No additional requirements.
			 */
			Transition inputTransition = (Transition) inputArc.getSource();
			/*
			 * Check the output arc. There should be only one, it should be
			 * regular, and its weight should be identical.
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
			 * Get the output transition. Should have only the place as input.
			 */
			Transition outputTransition = (Transition) outputArc.getTarget();
			preset = net.getInEdges(outputTransition);
			if (preset.size() != 1) {
				continue;
			}

			if (inputTransition == outputTransition) {
				continue;
			}

			/*
			 * Found a series transition. Remove if not sacred.
			 */
			if (!sacredNodes.contains(outputTransition)) {
				String log = "<fst place=\"" + place.getLabel() + "\" outputTransition=\""
						+ outputTransition.getLabel() + "\"/>";
				/*
				 * Output transition not sacred. Remove it. First, update the
				 * maps.
				 */
				for (Transition t : transitionMap.keySet()) {
					if (transitionMap.get(t) == outputTransition) {
						transitionMap.put(t, inputTransition);
					}
				}
				HashSet<Place> removePlaces = new HashSet<Place>();
				for (Place p : placeMap.keySet()) {
					if (placeMap.get(p) == place) {
						removePlaces.add(p);
					}
				}
				for (Place p : removePlaces) {
					placeMap.remove(p);
				}
				/*
				 * Transfer tokens from place to postset of output transition.
				 * Also, transfer outgoing edges from output transition to input
				 * transition.
				 */
				postset = net.getOutEdges(outputTransition);
				int tokens = marking.occurrences(place);
				int outputFirings = tokens / weight;
				MurataUtils.resetPlace(marking, place);
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> transferEdge : postset) {
					if (transferEdge instanceof Arc) {
						Arc transferArc = (Arc) transferEdge;
						MurataUtils.addArc(net, inputTransition, transferArc.getTarget(), transferArc.getWeight());
						Place outputPlace = (Place) transferArc.getTarget();
						marking.add(outputPlace, outputFirings * transferArc.getWeight());
						MurataUtils.updateLabel(place, marking);
					}
				}
				net.removePlace(place);
				net.removeTransition(outputTransition);
				return log; // Removed a place and a transition.
			} else if (!sacredNodes.contains(inputTransition)
					&& (outputTransition.isInvisible() || (net.getOutEdges(inputTransition).size() == 1))
					/*&& marking.occurrences(place) == 0*/) {
				String log = "<fst inputTransition=\"" + inputTransition.getLabel() + "\" place=\"" + place.getLabel()
						+ "\"/>";
				/*
				 * Input transition is not sacred and either the output
				 * transition is invisible or the input transition has only the
				 * place as output. Perhaps some explanation of this last
				 * requirement is in place. Assume that the output transition is
				 * visible, that is, it has a label, and that the input
				 * transition has additional outputs. Then the paths starting at
				 * these additional outputs do not include the output
				 * transition, whereas after reduction they would. Therefore, if
				 * the input transition has additional outputs, then the output
				 * transition must be invisible.
				 * 
				 * Remove the input transition. First, update the maps.
				 */
				for (Transition t : transitionMap.keySet()) {
					if (transitionMap.get(t) == inputTransition) {
						transitionMap.put(t, outputTransition);
					}
				}
				HashSet<Place> removePlaces = new HashSet<Place>();
				for (Place p : placeMap.keySet()) {
					if (placeMap.get(p) == place) {
						removePlaces.add(p);
					}
				}
				for (Place p : removePlaces) {
					placeMap.remove(p);
				}
				/*
				 * Transfer tokens from place to preset of input transition.
				 */
				preset = net.getInEdges(inputTransition);
				int tokens = marking.occurrences(place);
				int inputFirings = tokens / weight;
				MurataUtils.resetPlace(marking, place);
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> transferEdge : preset) {
					if (transferEdge instanceof Arc) {
						Arc transferArc = (Arc) transferEdge;
						Place inputPlace = (Place) transferArc.getSource();
						marking.add(inputPlace, inputFirings * transferArc.getWeight());
						MurataUtils.updateLabel(inputPlace, marking);
					}
				}
				/*
				 * Transfer incoming edges from the input transition to the
				 * output transition.
				 */
				preset = net.getInEdges(inputTransition);
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> transferEdge : preset) {
					if (transferEdge instanceof Arc) {
						Arc transferArc = (Arc) transferEdge;
						MurataUtils.addArc(net, transferArc.getSource(), outputTransition, transferArc.getWeight());
					}
				}
				/*
				 * Transfer outgoing edges from the input transition to the
				 * output transition.
				 */
				postset = net.getOutEdges(inputTransition);
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> transferEdge : postset) {
					if (transferEdge instanceof Arc) {
						Arc transferArc = (Arc) transferEdge;
						MurataUtils.addArc(net, outputTransition, transferArc.getTarget(), transferArc.getWeight());
					}
				}
				net.removePlace(place);
				net.removeTransition(inputTransition);
				return log; // Removed a place and a transition.
			}
			/*
			 * Either both are sacred, or the output transition is sacred and
			 * visible and the input transition has additional outgoing edges.
			 * Any way, the reduction rule does not apply.
			 */
		}
		return null;
	}

}
