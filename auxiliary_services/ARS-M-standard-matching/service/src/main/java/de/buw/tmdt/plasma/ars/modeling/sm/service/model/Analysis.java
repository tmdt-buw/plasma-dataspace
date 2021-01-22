package de.buw.tmdt.plasma.ars.modeling.sm.service.model;

import de.buw.tmdt.plasma.datamodel.CombinedModel;

import javax.persistence.*;

@Entity
public class Analysis {

	/**
	 * Identifies the analysis.
	 * Will mostly be equal to the data source id.
	 */
	@Id
	protected String uuid;

	/**
	 * The input as received.
	 */
	@Lob
	@Convert(converter = CombinedModelPersistenceConverter.class)
	private CombinedModel input;

	/**
	 * Generated result.
	 */
	@Lob
	@Convert(converter = CombinedModelPersistenceConverter.class)
	private CombinedModel result;

	@ManyToOne
	private Standard standard;

	protected Analysis() {
	}

	public Analysis(String uuid) {
		this.uuid = uuid;
	}

	public CombinedModel getInput() {
		return input;
	}

	public void setInput(CombinedModel input) {
		this.input = input;
	}

	public CombinedModel getResult() {
		return result;
	}

	public void setResult(CombinedModel semanticModel) {
		this.result = semanticModel;
	}

	public void setStandard(Standard standard) {
		this.standard = standard;
	}

	public Standard getStandard() {
		return this.standard;
	}
}
