package de.buw.tmdt.plasma.services.sms.schemaanalysis;

import de.buw.tmdt.plasma.datamodel.syntaxmodel.SyntaxModel;

import java.time.ZonedDateTime;
import java.util.*;


public class Analysis {

	protected String id;

	protected ZonedDateTime creation;

	private List<SyntaxModel> subAnalyses;

	private SyntaxModel result;

	protected Analysis() {
		this.creation = ZonedDateTime.now();
	}

	public Analysis(String id) {
		this();
		this.id = id;
		this.subAnalyses = new ArrayList<>();
	}

	public void addSubAnalysis(SyntaxModel model) {
		subAnalyses.add(model);
	}

	public void setResult(SyntaxModel result) {
		this.result = result;
	}

	public List<SyntaxModel> getSampleModels() {
		return Collections.unmodifiableList(subAnalyses);
	}

	public SyntaxModel getResult() {
		return result;
	}

	public String getUuid() {
		return id;
	}

	public ZonedDateTime getCreation() {
		return creation;
	}

}
