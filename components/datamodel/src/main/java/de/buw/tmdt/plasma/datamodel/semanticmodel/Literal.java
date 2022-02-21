package de.buw.tmdt.plasma.datamodel.semanticmodel;

import de.buw.tmdt.plasma.datamodel.CombinedModelIntegrityException;

public class Literal extends SemanticModelNode {

    private String value;

    public Literal(String value) {
        super(null, value); // use the value as label
        this.value = value;
    }

    @Override
    public Literal copy() {
        return new Literal(getValue());
    }

    public String getValue() {
        return value;
    }

    public void validate() throws CombinedModelIntegrityException {
        if (getValue() == null || getValue().isBlank()) {
            throw new CombinedModelIntegrityException("Literal " + getUuid() + " has no has no proper value");
        }
    }

    @Override
    public String toString() {
        return "Literal{" +
                "value='" + value + '\'' +
                '}';
    }
}
