package de.buw.tmdt.plasma.services.sms.schemaanalysis;


import de.buw.tmdt.plasma.services.sms.schemaanalysis.model.exception.SchemaAnalysisException;
import de.buw.tmdt.plasma.services.sms.schemaanalysis.model.syntaxmodel.Node;

public class SchemaAnalysisAggregateRecognizedSchema {

	private int dataPointThreshold = 10;
	/**
	 * The collected RecognizedSchemas.
	 */
	private Node mergedRecognizedNode;
	private int collectionCounter = 0;

	public SchemaAnalysisAggregateRecognizedSchema(int dataPointThreshold) {
		this.dataPointThreshold = dataPointThreshold;
		mergedRecognizedNode = null;
	}

	public void addNode(Node n) throws SchemaAnalysisException {

		if (this.mergedRecognizedNode == null) {
			this.mergedRecognizedNode = n;
		} else {
			this.mergedRecognizedNode = (Node) this.mergedRecognizedNode.merge(n);
		}
		collectionCounter++;

	}

	public boolean isAggregationReady() {
		return collectionCounter >= this.dataPointThreshold;
	}


	public void setDataPointThreshold(Integer dataPointThreshold) {
		if (dataPointThreshold != null) {
			this.dataPointThreshold = dataPointThreshold;
		}
	}

	public Node getMergedRecognizedNode() {
		return mergedRecognizedNode;
	}
}

