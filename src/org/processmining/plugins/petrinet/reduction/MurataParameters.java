package org.processmining.plugins.petrinet.reduction;

public class MurataParameters {

	public MurataParameters() {
		setAllowFPTSacredNode(true);
	}
	
	/*
	 * Whether the FPT rule may reduce a non sacred node while the sibling is sacred.
	 */
	private boolean allowFPTSacredNode;

	public boolean isAllowFPTSacredNode() {
		return allowFPTSacredNode;
	}

	public void setAllowFPTSacredNode(boolean allowFPTSacredNode) {
		this.allowFPTSacredNode = allowFPTSacredNode;
	}
}
