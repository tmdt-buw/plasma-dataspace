package de.buw.tmdt.plasma.services.sas.shared.dto.syntaxmodel;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class SelectorDTO implements Serializable {

	private static final long serialVersionUID = -1942954119618039295L;

	@Override
	@SuppressWarnings("MagicCharacter")
	public String toString() {
		return "{\"@class\":\"SelectorDTO\""
		       + '}';
	}

}
