package org.processmining.parameters;

import java.util.HashSet;
import java.util.Set;

import org.processmining.basicutils.parameters.impl.PluginParametersImpl;
import org.processmining.models.semantics.petrinet.Marking;

public class BerthelotParameters extends PluginParametersImpl {

	private Marking initialMarking;
	private Set<Marking> finalMarkings;
	private Marking initialBerthelotMarking;
	private Set<Marking> finalBerthelotMarkings;
	
	public BerthelotParameters() {
		super();
		setInitialMarking(new Marking());
		setFinalMarkings(new HashSet<Marking>());
		setInitialBerthelotMarking(null);
		setFinalBerthelotMarkings(null);
	}
	
	public BerthelotParameters(BerthelotParameters parameters) {
		super(parameters);
		setInitialMarking(parameters.getInitialMarking());
		setFinalMarkings(parameters.getFinalMarkings());
		setInitialBerthelotMarking(parameters.getInitialBerthelotMarking());
		setFinalBerthelotMarkings(parameters.getFinalBerthelotMarkings());
	}
	
	public Marking getInitialMarking() {
		return initialMarking;
	}

	public void setInitialMarking(Marking initialMarking) {
		this.initialMarking = initialMarking;
	}

	public Set<Marking> getFinalMarkings() {
		return finalMarkings;
	}

	public void setFinalMarkings(Set<Marking> finalMarkings) {
		this.finalMarkings = finalMarkings;
	}

	public Marking getInitialBerthelotMarking() {
		return initialBerthelotMarking;
	}

	public void setInitialBerthelotMarking(Marking initialBerthelotMarking) {
		this.initialBerthelotMarking = initialBerthelotMarking;
	}

	public Set<Marking> getFinalBerthelotMarkings() {
		return finalBerthelotMarkings;
	}

	public void setFinalBerthelotMarkings(Set<Marking> finalBerthelotMarkings) {
		this.finalBerthelotMarkings = finalBerthelotMarkings;
	}
}
