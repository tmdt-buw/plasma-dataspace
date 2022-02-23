package de.buw.tmdt.plasma.datamodel.semanticmodel;

import de.buw.tmdt.plasma.datamodel.AsTemplate;
import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;

import java.util.Objects;

public interface URIDefinedElement extends AsTemplate {

    String getURI();

    String getLabel();

    String getDescription();

    /**
     * Identifies if two {@link URIDefinedElement}s represent the same entity.
     * Needed as there might exist multiple {@link URIDefinedElement} instances that actually represent the same concept
     * or class but have different uuids.
     *
     * @param ec The {@link URIDefinedElement} to check
     * @return true of the {@code uri}s of both instances match
     */
    default boolean sameAs(URIDefinedElement ec) {
        return Objects.equals(getURI(), ec.getURI());
    }

    URIDefinedElement copy();

    void validate() throws CombinedModelIntegrityException;


}