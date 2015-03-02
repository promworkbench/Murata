package org.processmining.plugins;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.processmining.algorithms.BerthelotAlgorithm;
import org.processmining.connections.BerthelotConnection;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.parameters.BerthelotParameters;

@Plugin(name = "Remove Structural Redundant Places from Petri Net", parameterLabels = { "Petri Net", "Parameters" }, returnLabels = { "Petri Net" }, returnTypes = { Petrinet.class })
public class BerthelotPlugin extends BerthelotAlgorithm {

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "H.M.W. Verbeek", email = "h.m.w.verbeek@tue.nl")
	@PluginVariant(variantLabel = "Remove Structural Redundant Places, Default", requiredParameterLabels = { 0 })
	public Petrinet runDefault(PluginContext context, Petrinet net) {
		BerthelotParameters parameters = new BerthelotParameters();
		if (context != null) {
			try {
				Collection<InitialMarkingConnection> connections = context.getConnectionManager().getConnections(
						InitialMarkingConnection.class, context, net);
				if (connections.size() == 1) {
					parameters.setInitialMarking((Marking) connections.iterator().next()
							.getObjectWithRole(InitialMarkingConnection.MARKING));
				}
			} catch (ConnectionCannotBeObtained e) {
			}
			try {
				Collection<FinalMarkingConnection> connections = context.getConnectionManager().getConnections(
						FinalMarkingConnection.class, context, net);
				Set<Marking> finalMarkings = new HashSet<Marking>();
				for (FinalMarkingConnection connection : connections) {
					finalMarkings.add((Marking) connection.getObjectWithRole(FinalMarkingConnection.MARKING));
				}
				parameters.setFinalMarkings(finalMarkings);
			} catch (ConnectionCannotBeObtained e) {
			}
		}
		return runConnection(context, net, parameters);
	}

	@Deprecated
	public Petrinet reduceDefault(PluginContext context, Petrinet net) {
		return runDefault(context, net);
	}

	@PluginVariant(variantLabel = "Remove Structural Redundant Places, Parameters", requiredParameterLabels = { 0, 1 })
	public Petrinet run(PluginContext context, Petrinet net, BerthelotParameters parameters) {
		return runConnection(context, net, parameters);
	}

	@Deprecated
	public Petrinet reduceParameters(PluginContext context, Petrinet net, BerthelotParameters parameters) {
		return run(context, net, parameters);
	}

	private Petrinet runConnection(PluginContext context, Petrinet net,
			BerthelotParameters parameters) {
		if (parameters.isTryConnections()) {
			Collection<BerthelotConnection> connections;
			try {
				connections = context.getConnectionManager().getConnections(
						BerthelotConnection.class, context, net);
				for (BerthelotConnection connection : connections) {
					if (connection.getObjectWithRole(BerthelotConnection.NET).equals(net)
							&& connection.getParameters().equals(parameters)) {
						return connection.getObjectWithRole(BerthelotConnection.REDUCEDNET);
					}
				}
			} catch (ConnectionCannotBeObtained e) {
			}
		}
		Petrinet reducedNet = apply(context, net, parameters);
		if (parameters.isTryConnections()) {
			context.getConnectionManager().addConnection(
					new BerthelotConnection(net, reducedNet, parameters));
		}
		return reducedNet;
	}
}
