package de.buw.tmdt.plasma.services.sms.schemaanalysis.model.syntaxmodel;

import java.util.List;

public interface RawDataContainer {
	String getCleansingPattern();

	List<String> getExamples();
}
