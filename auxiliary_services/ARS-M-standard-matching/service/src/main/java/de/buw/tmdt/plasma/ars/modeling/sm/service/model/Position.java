package de.buw.tmdt.plasma.ars.modeling.sm.service.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Position {
    @Column
    private final double xCoordinate;
    @Column
    private final double yCoordinate;

    //Hibernate constructor
    //creates invalid state if not properly initialized afterwards
    public Position() {
        xCoordinate = yCoordinate = 0;
    }

    public Position(double xCoordinate, double yCoordinate) {
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public double getXCoordinate() {
        return xCoordinate;
    }

    public double getYCoordinate() {
        return yCoordinate;
    }

    @Override
    @SuppressWarnings("MagicCharacter")
    public String toString() {
        return "{\"@class\":\"Position\""
                + ", \"xCoordinate\":\"" + xCoordinate + '"'
                + ", \"yCoordinate\":\"" + yCoordinate + '"'
                + '}';
    }
}
