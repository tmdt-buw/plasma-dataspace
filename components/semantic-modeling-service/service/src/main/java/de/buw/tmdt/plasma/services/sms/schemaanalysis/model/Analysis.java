package de.buw.tmdt.plasma.services.sms.schemaanalysis.model;

import de.buw.tmdt.plasma.services.sms.schemaanalysis.model.syntaxmodel.Node;

import javax.persistence.*;
import java.util.*;


public class Analysis {

	protected String id;

	protected Date creation;

	private List<Node> subAnalyses;

	private Node result;

	protected Analysis() {
		this.creation = new Date();
	}

	public Analysis(String id) {
		this();
		this.id = id;
		this.subAnalyses = new ArrayList<>();
	}

	public void addSubAnalysis(Node node) {
		subAnalyses.add(node);
	}

	public void setResult(Node result) {
		this.result = result;
	}

	public List<Node> getSubAnalyses() {
		return Collections.unmodifiableList(subAnalyses);
	}

	public Node getResult() {
		return result;
	}

	public String getUuid() {
		return id;
	}
}
