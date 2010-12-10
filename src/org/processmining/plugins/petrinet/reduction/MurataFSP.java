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

public class MurataFSP extends MurataRule {

	public String reduce(Petrinet net, Collection<PetrinetNode> sacredNodes,
			HashMap<Transition, Transition> transitionMap, HashMap<Place, Place> placeMap, Marking marking) {
		/*
		 * Iterate over all transitions.
		 */
		for (Transition transition : net.getTransitions()) {
			if (sacredNodes.contains(transition)) {
				continue; // The transition is sacred.
			}
			/*
			 * Check the input arc. There should be only one, it should be
			 * regular, and it weight should be one.
			 */
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> preset = net
					.getInEdges(transition);
			if (preset.size() != 1) {
				continue;
			}
			PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge = preset.iterator().next();
			if (!(edge instanceof Arc)) {
				continue;
			}
			Arc inputArc = (Arc) edge;
			if (inputArc.getWeight() != 1) {
				continue;
			}
			/*
			 * Get the input place. Should have only the place as output.
			 */
			Place inputPlace = (Place) inputArc.getSource();
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = net
					.getOutEdges(inputPlace);
			if (postset.size() != 1) {
				continue;
			}
			/*
			 * Check the output arc. There should be only one, it should be
			 * regular, and its weight should be one.
			 */
			postset = net.getOutEdges(transition);
			if (postset.size() != 1) {
				continue;
			}
			edge = postset.iterator().next();
			if (!(edge instanceof Arc)) {
				continue;
			}
			Arc outputArc = (Arc) edge;
			if (outputArc.getWeight() != 1) {
				continue;
			}
			/*
			 * Get the output transition. No additional requirements.
			 */
			Place outputPlace = (Place) outputArc.getTarget();

			if (inputPlace == outputPlace) {
				continue;
			}

			/*
			 * Found a series place. Remove a place (input or output) that is
			 * not sacred.
			 */
			if (!sacredNodes.contains(inputPlace)) {
				String log = "<fsp inputPlace=\"" + inputPlace.getLabel() + "\" transition=\"" + transition.getLabel()
						+ "\"/>";
				/*
				 * The input place is not sacred. Remove it. First, update the
				 * mappings.
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
				for (Place p : placeMap.keySet()) {
					if (placeMap.get(p) == inputPlace) {
						placeMap.put(p, outputPlace);
					}
				}
				/*
				 * Move tokens from input place to output place.
				 */
				int tokens = marking.occurrences(inputPlace);
				marking.add(outputPlace, tokens);
				MurataUtils.updateLabel(outputPlace, marking);
				MurataUtils.resetPlace(marking, inputPlace);
				/*
				 * Also, transfer any input edge from the input place to the
				 * output place.
				 */
				preset = net.getInEdges(inputPlace);
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> transferEdge : preset) {
					if (transferEdge instanceof Arc) {
						Arc transferArc = (Arc) transferEdge;
						MurataUtils.addArc(net, transferArc.getSource(), outputPlace, transferArc.getWeight());
					}
				}
				net.removeTransition(transition);
				net.removePlace(inputPlace);
				return log; // A place and a transition have been removed.
			} else if (!sacredNodes.contains(outputPlace)) {
				String log = "<fsp transition=\"" + transition.getLabel() + "\" outputPlace" + outputPlace.getLabel()
						+ "\"/>";
				/*
				 * The output place is not sacred. Remove it. First, update the
				 * mappings.
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
				for (Place p : placeMap.keySet()) {
					if (placeMap.get(p) == outputPlace) {
						placeMap.put(p, inputPlace);
					}
				}
				/*
				 * Move tokens form output place to input place.
				 */
				int tokens = marking.occurrences(outputPlace);
				marking.add(inputPlace, tokens);
				MurataUtils.updateLabel(inputPlace, marking);
				MurataUtils.resetPlace(marking, outputPlace);
				/*
				 * Also, transfer any input edge from the output place to the
				 * input place, and any output edge from the output place to the
				 * input place.
				 */
				preset = net.getInEdges(outputPlace);
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> transferEdge : preset) {
					if (transferEdge instanceof Arc) {
						Arc transferArc = (Arc) transferEdge;
						MurataUtils.addArc(net, transferArc.getSource(), inputPlace, transferArc.getWeight());
					}
				}
				postset = net.getOutEdges(outputPlace);
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> transferEdge : postset) {
					if (transferEdge instanceof Arc) {
						Arc transferArc = (Arc) transferEdge;
						MurataUtils.addArc(net, inputPlace, transferArc.getTarget(), transferArc.getWeight());
					}
				}
				net.removeTransition(transition);
				net.removePlace(outputPlace);
				return log; // A place and a transition have been removed.
			}
			/*
			 * Both are sacred. Leave them.
			 */
		}
		return null;
	}

}
