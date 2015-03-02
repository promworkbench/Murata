package org.processmining.connections;

import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.parameters.BerthelotParameters;

public class BerthelotConnection extends AbstractConnection {

	public final static String NET = "Net";
	public final static String REDUCEDNET = "Reduced net";

	private BerthelotParameters parameters;

	public BerthelotConnection(Petrinet net, Petrinet reducedNet,
			BerthelotParameters parameters) {
		super("Berthelot Reduction Connection");
		put(NET, net);
		put(REDUCEDNET, reducedNet);
		this.parameters = new BerthelotParameters(parameters);
	}

	public BerthelotParameters getParameters() {
		return parameters;
	}
}
