package de.buw.tmdt.plasma.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Base class for all elements that can be used inside a combined model that have an explicit placement.
 */
public class PositionedCombinedModelElement extends CombinedModelElement implements Serializable {

    private static final long serialVersionUID = -3619721225687115309L;

    private Double xCoordinate;
    private Double yCoordinate;

    public PositionedCombinedModelElement(
            @NotNull String label
    ) {
        super(label);
        this.xCoordinate = null;
        this.yCoordinate = null;
    }

    public PositionedCombinedModelElement(
            @NotNull String label,
            @Nullable Double xCoordinate,
            @Nullable Double yCoordinate
    ) {
        super(label);
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    @JsonCreator
    public PositionedCombinedModelElement(
            @Nullable @JsonProperty String uuid,
            @NotNull @JsonProperty String label,
            @Nullable @JsonProperty Double xCoordinate,
            @Nullable @JsonProperty Double yCoordinate
    ) {
        super(uuid, label);
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    @Nullable
    @JsonProperty
    public Double getXCoordinate() {
        return xCoordinate;
    }

    public void setXCoordinate(Double xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    @Nullable
    @JsonProperty
    public Double getYCoordinate() {
        return yCoordinate;
    }

    public void setYCoordinate(Double yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    @Override
    public PositionedCombinedModelElement copy() {
        return new PositionedCombinedModelElement(
                getUuid(),
                getLabel(),
                getXCoordinate(),
                getYCoordinate()
        );
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"PositionedCombinedModelElement\""
                + ",\"parent\":\"" + super.toString() + '"'
                + ",\"xCoordinate\":\"" + xCoordinate + '"'
                + ",\"yCoordinate\":\"" + yCoordinate + '"'
                + '}';
    }
}
