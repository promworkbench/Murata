package org.processmining.plugins.petrinet.reduction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.connections.petrinets.PetrinetGraphConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;

/**
 * Petri net reduction rules, based on Murata rules.
 * 
 * @author Eric Verbeek
 * @version 0.1
 */

public class Murata {

	/**
	 * Apply the Murata reduction rules until no further reductions are
	 * possible.
	 */
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W Verbeek", email = "h.m.w.verbeek@tue.nl")
	@Plugin(name = "Petrinet reduction on marked Petri net", parameterLabels = { "Petri net", "Marking" }, returnLabels = {
			"Petri net", "Marking" }, returnTypes = { Petrinet.class, Marking.class }, userAccessible = true)
	public Object[] run(final PluginContext context, final Petrinet net, final Marking marking)
			throws ConnectionCannotBeObtained {
		/*
		 * Create the set of sacred nodes. By default, every visible transition
		 * will be sacred.
		 */
		MurataInput input = new MurataInput(net, marking);
		input.setVisibleSacred(net);
		MurataOutput output = run(context, input);
		Object objects[] = new Object[2];
		objects[0] = output.getNet();
		objects[1] = output.getMarking();
		return objects;
	}

	/**
	 * Apply the Murata reduction rules until no further reductions are
	 * possible.
	 */
	public MurataOutput run(final PluginContext context, final MurataInput input) throws ConnectionCannotBeObtained {
		/*
		 * See if a proper connection exists between the net and the marking.
		 */
		if (context != null) {
			context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, input.getNet(),
					input.getMarking());
		}
		/*
		 * Yes, it exists. From here, we can ignore it (it's empty any way, the
		 * fact that it exists has value, not the thing itself).
		 */

		/*
		 * First, copy the net.
		 */
		HashMap<Transition, Transition> transitionMap = new HashMap<Transition, Transition>();
		HashMap<Place, Place> placeMap = new HashMap<Place, Place>();
		final Petrinet net = copyPetrinet(input.getNet(), transitionMap, placeMap);
		Marking marking = copyMarking(input.getMarking(), placeMap);
		if (context != null) {
			context.getFutureResult(0).setLabel(net.getLabel());
		}
		MurataOutput output = new MurataOutput(net, marking);
		/*
		 * Second, create the set of sacred nodes.
		 */
		HashSet<PetrinetNode> sacredNodes = new HashSet<PetrinetNode>();
		for (Transition transition : input.getNet().getTransitions()) {
			if (input.isSacred(transition)) {
				sacredNodes.add(transitionMap.get(transition));
			}
		}
		for (Place place : input.getNet().getPlaces()) {
			if (input.isSacred(place)) {
				sacredNodes.add(placeMap.get(place));
			}
		}

		/*
		 * Third, collect all applicable rules.
		 */
		Collection<MurataRule> reductionRules = new ArrayList<MurataRule>();
		if (input.isAllowedRule(MurataInput.FST)) {
			reductionRules.add(new MurataFST());
		}
		if (input.isAllowedRule(MurataInput.FSP)) {
			reductionRules.add(new MurataFSP());
		}
		if (input.isAllowedRule(MurataInput.FPT)) {
			reductionRules.add(new MurataFPT());
		}
		if (input.isAllowedRule(MurataInput.FPP)) {
			reductionRules.add(new MurataFPP());
		}
		if (input.isAllowedRule(MurataInput.ELT)) {
			reductionRules.add(new MurataEST());
		}
		if (input.isAllowedRule(MurataInput.ELP)) {
			reductionRules.add(new MurataESP());
		}

		/**
		 * It is not clear how many reductions will take place. However, every
		 * reduction effectively removes a place and/or a transition. Therefore,
		 * the progress indicator will track how many places and/or transitions
		 * have been removed so far. If all have been removed, reduction was
		 * very successful :-).
		 */
		int size = net.getPlaces().size() + net.getTransitions().size();
		if (context != null) {
			context.getProgress().setMinimum(0);
			context.getProgress().setMaximum(size);
			context.getProgress().setCaption("Reducing Petri net");
			context.getProgress().setIndeterminate(false);
		}
		/*
		 * Fourth, apply the reductions rules until no rule can be applied any
		 * more.
		 */
		String log;
		do {
			log = null;
			for (MurataRule reductionRule : reductionRules) {
				if (log == null) {
					log = reductionRule.reduce(net, sacredNodes, transitionMap, placeMap, marking);
				}
			}
			if (log != null) {
				/**
				 * Increment progress indicator for every place/transition
				 * removed.
				 */
				int newSize = net.getPlaces().size() + net.getTransitions().size();
				while (size > newSize) {
					if (context != null) {
						context.getProgress().inc();
					}
					size--;
				}
				output.getLog().add(log);
			}
		} while (log != null);
		/**
		 * Flush the progress indicator.
		 */
		while (size > 0) {
			if (context != null) {
				context.getProgress().inc();
			}
			size--;
		}

		/*
		 * Fourth, create an update for the initial marking for the reduced net,
		 * and connect that update with the reduced net. Also, provide
		 * transition and place connections.
		 */

		if (context != null) {
			// register the initial marking of the reduced net
			context.addConnection(new InitialMarkingConnection(net, marking));

			// register the mapping between Petrinets.
			context.addConnection(new PetrinetGraphConnection(input.getNet(), net, transitionMap, placeMap));
		}

		// Connection markingConnection = new Connection(
		// ResetInhibitorNet.MARKINGCONNECTION,
		// new Connection.WeakTuple("Places of " + net.getLabel(), net),
		// new Connection.WeakTuple("Marking of " + net.getLabel(), marking));
		// context.addConnection(markingConnection);
		// Connection transitionConnection = new Connection(
		// new Connection.StrongTuple("Transition mapping", transitionMap),
		// new Connection.WeakTuple("Transitions of " +
		// input.getNet().getLabel(), input.getNet()),
		// new Connection.WeakTuple("Transitions of " + net.getLabel(), net));
		// context.addConnection(transitionConnection);
		// Connection placeConnection = new Connection(
		// new Connection.StrongTuple("Place mapping", placeMap),
		// new Connection.WeakTuple("Places of " + input.getNet().getLabel(),
		// input.getNet()),
		// new Connection.WeakTuple("Places of " + net.getLabel(), net));
		// context.addConnection(placeConnection);
		// context.getFutureResult(1).setLabel("Marking of "+ net.getLabel());

		output.setTransitionMapping(transitionMap);
		output.setPlaceMapping(placeMap);

		/*
		 * Fifth, return the created net and marking.
		 */
		return output;
	}

	/**
	 * Dump the given marked net in TPN format to the context log. For debug
	 * purposes only.
	 * 
	 * @param context
	 *            The context with the log.
	 * @param net
	 *            The given net.
	 * @param marking
	 *            The marking of the given net.
	 */
	@SuppressWarnings("unused")
	private void dump(PluginContext context, Petrinet net, Marking marking) {
		for (Place p : net.getPlaces()) {
			context.log("place \"" + p.getLabel()
					+ (marking.occurrences(p) > 0 ? "\" init " + marking.occurrences(p) : "\"") + ";");
		}
		for (Transition t : net.getTransitions()) {
			String s = "trans \"" + t.getLabel() + "\"";
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> preset = net.getInEdges(t);
			if (preset.size() > 0) {
				s += " in";
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : preset) {
					if (edge instanceof Arc) {
						Arc arc = (Arc) edge;
						for (int i = 0; i < arc.getWeight(); i++) {
							s += " \"" + arc.getSource().getLabel() + "\"";
						}
					}
				}
			}
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> postset = net.getOutEdges(t);
			if (postset.size() > 0) {
				s += " out";
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : postset) {
					if (edge instanceof Arc) {
						Arc arc = (Arc) edge;
						for (int i = 0; i < arc.getWeight(); i++) {
							s += " \"" + arc.getTarget().getLabel() + "\"";
						}
					}
				}
				;
			}
			context.log(s + ";");
		}
	}

	/**
	 * Copy the marking.
	 * 
	 * @param marking
	 *            The marking.
	 * @param placeMap
	 *            The map from the original marking to the copy marking.
	 * @return The copy marking.
	 */
	private Marking copyMarking(Marking marking, HashMap<Place, Place> placeMap) {

		/*
		 * Create a new marking.
		 */
		Marking copyMarking = new Marking();
		/*
		 * Iterate over all places in the original marking.
		 */
		Iterator<Place> placeIterator = marking.iterator();
		while (placeIterator.hasNext()) {
			Place place = placeIterator.next();
			/*
			 * Add to the copy marking.
			 */
			copyMarking.add(placeMap.get(place), marking.occurrences(place));
		}
		return copyMarking;
	}

	/**
	 * Copy the net. Update maps (both from the original net to the copy net).
	 * 
	 * @param net
	 *            The net.
	 * @param transitionMap
	 *            The transition map.
	 * @param placeMap
	 *            The place map.
	 * @return The copy net.
	 */
	private Petrinet copyPetrinet(Petrinet net, HashMap<Transition, Transition> transitionMap,
			HashMap<Place, Place> placeMap) {
		HashMap<PetrinetNode, PetrinetNode> nodeMap = new HashMap<PetrinetNode, PetrinetNode>();
		/*
		 * Create the copy net, empty for the time being.
		 */
		Petrinet netCopy = PetrinetFactory.newPetrinet(net.getLabel() + " [Reduced]");
		/*
		 * Copy the transitions. Update transition map.
		 */
		for (Transition transition : net.getTransitions()) {
			Transition transitionCopy = netCopy.addTransition(transition.getLabel());
			transitionCopy.setInvisible(transition.isInvisible());
			transitionMap.put(transition, transitionCopy);
			nodeMap.put(transition, transitionCopy);
		}
		/*
		 * Copy the places. Update place map.
		 */
		for (Place place : net.getPlaces()) {
			Place placeCopy = netCopy.addPlace(place.getLabel());
			placeMap.put(place, placeCopy);
			nodeMap.put(place, placeCopy);
		}
		/*
		 * Copy the edges. Looks a bit murky.
		 */
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getEdges()) {
			if (!(edge instanceof Arc)) {
				continue;
			}
			Arc arc = (Arc) edge;
			PetrinetNode sourceNode = nodeMap.get(arc.getSource());
			PetrinetNode targetNode = nodeMap.get(arc.getTarget());
			MurataUtils.addArc(netCopy, sourceNode, targetNode, arc.getWeight());
		}
		/*
		 * Return the copy.
		 */
		return netCopy;
	}
}
