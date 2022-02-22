package de.buw.tmdt.plasma.datamodel.semanticmodel;

import de.buw.tmdt.plasma.datamodel.AsTemplate;

public interface URIDefinedElement extends AsTemplate {

    String getURI();

    String getLabel();

    String getDescription();

}