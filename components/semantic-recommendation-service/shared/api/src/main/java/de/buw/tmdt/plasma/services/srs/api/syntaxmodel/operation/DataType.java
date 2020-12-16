package de.buw.tmdt.plasma.services.srs.api.syntaxmodel.operation;

public enum DataType {
	UNKNOWN("Unknown"),
	STRING("String"),
	BOOLEAN("Boolean"),
	NUMBER("Number"),
	BINARY("Binary");

	public final String identifier;

	DataType(String identifier) {
		this.identifier = identifier;
	}

	public static DataType fromIdentifier(String string) {
		for (DataType dataType : DataType.values()) {
			if (dataType.identifier.equals(string)) {
				return dataType;
			}
		}
		throw new IllegalArgumentException("Unknown identifier for DataType `" + string + "`.");
	}
}
