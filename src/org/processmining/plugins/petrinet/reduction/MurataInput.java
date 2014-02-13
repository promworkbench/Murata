package org.processmining.plugins.petrinet.reduction;

import java.util.HashSet;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

/**
 * General input for Petri net reduction rules.
 * 
 * @author Eric Verbeek
 * @version 0.1
 */

public class MurataInput {
	/*
	 * Bit mask for the Fusion of Series Transitions rule.
	 */
	static public final int FST = 1;
	/*
	 * Bit mask for the Fusion of Series Places rule.
	 */
	static public final int FSP = 2;
	/*
	 * Bit mask for the Fusion of Parallel Transitions rule.
	 */
	static public final int FPT = 4;
	/*
	 * Bit mask for the Fusion of Parallel Places rule.
	 */
	static public final int FPP = 8;
	/*
	 * Bit mask for the Elimination of Self-Loop Transitions rule.
	 */
	static public final int ELT = 16;
	/*
	 * Bit mask for the Elimination of Self-Loop Places rule.
	 */
	static public final int ELP = 32;
	/*
	 * Bit mask for the Cyclic State Machine rule.
	 */
	static public final int CSM = 64;

	/*
	 * The Petri net to reduce.
	 */
	private Petrinet net;
	/*
	 * The sacred nodes, that is, the nodes not to reduce.
	 */
	private final HashSet<PetrinetNode> sacredNodes;
	/*
	 * The combined bit masks of the reduction rules which may be applied.
	 */
	private int allowedRules;
	/*
	 * The marking of the Petri net.
	 */
	private Marking marking;

	/**
	 * Constructs a MurataInput given a Petri net and its marking. All nodes may
	 * be reduced, all rules may be applied.
	 * 
	 * @param net
	 *            The Petri net.
	 * @param marking
	 *            The marking.
	 */
	public MurataInput(Petrinet net, Marking marking) {
		sacredNodes = new HashSet<PetrinetNode>();
		allowedRules = FST | FSP | FPT | FPP | ELT | ELP | CSM;
		this.net = net;
		this.marking = marking;
	}

	/**
	 * Adds the set of transitions with a visible label to the sacred nodes.
	 * 
	 * @param net
	 *            The Petri net at hand.
	 */
	public void setVisibleSacred(Petrinet net) {
		for (PetrinetNode node : net.getNodes()) {
			if (node instanceof Transition) {
				Transition transition = (Transition) node;
				if (!transition.isInvisible()) {
					sacredNodes.add(transition);
				}
			}
		}
	}

	/**
	 * Adds a node to the sacred nodes.
	 * 
	 * @param node
	 *            The node to add.
	 */
	public void addSacred(PetrinetNode node) {
		sacredNodes.add(node);
	}

	/**
	 * Removes a node form the sacred nodes.
	 * 
	 * @param node
	 *            The node to remove.
	 */
	public void removeSacred(PetrinetNode node) {
		sacredNodes.remove(node);
	}

	/**
	 * Checks whether a node is sacred.
	 * 
	 * @param node
	 *            The node to check.
	 * @return Whether it is sacred.
	 */
	public boolean isSacred(PetrinetNode node) {
		return sacredNodes.contains(node);
	}

	/**
	 * Allows a rule to be applied.
	 * 
	 * @param rule
	 *            The bit mask of the rule to allow.
	 */
	public void allowRule(int rule) {
		allowedRules |= rule;
	}

	/**
	 * Disallows a rule to be applied.
	 * 
	 * @param rule
	 *            The bit mask of the rule to disallow.
	 */
	public void disallowRule(int rule) {
		allowedRules &= ~rule;
	}

	/**
	 * Checks whether some rule may be applied.
	 * 
	 * @param rule
	 *            The bit mask of the rule to check.
	 * @return Whether the rule may be applied.
	 */
	public boolean isAllowedRule(int rule) {
		return (allowedRules & rule) != 0;
	}

	/**
	 * Sets the Petri net to reduce.
	 * 
	 * @param net
	 *            The Petri net to reduce.
	 */
	public void setNet(Petrinet net) {
		this.net = net;
	}

	/**
	 * Gets the Petri net to reduce.
	 * 
	 * @return The Petri net to reduce.
	 */
	public Petrinet getNet() {
		return net;
	}

	/**
	 * Sets the marking (of the Petri net to reduce).
	 * 
	 * @param marking
	 *            The marking (of the Petri net to reduce).
	 */
	public void setMarking(Marking marking) {
		this.marking = marking;
	}

	/**
	 * Gets the marking (of the Petri net to reduce).
	 * 
	 * @return The marking (of the Petri net to reduce).
	 */
	public Marking getMarking() {
		return marking;
	}
}
