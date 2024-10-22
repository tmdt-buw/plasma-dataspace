package de.buw.tmdt.plasma.datamodel;

import java.util.List;

public class PreferredServices {
    private List<String> preferredLabeling;
    private List<String> preferredModeling;
    private List<String> preferredRefinement;

    public PreferredServices() {
    }

    public PreferredServices(List<String> preferredLabeling, List<String> preferredModeling, List<String> preferredRefinement) {
        this.preferredLabeling = preferredLabeling;
        this.preferredModeling = preferredModeling;
        this.preferredRefinement = preferredRefinement;
    }

    public List<String> getPreferredLabeling() {
        return preferredLabeling;
    }

    public void setPreferredLabeling(List<String> preferredLabeling) {
        this.preferredLabeling = preferredLabeling;
    }

    public List<String> getPreferredModeling() {
        return preferredModeling;
    }

    public void setPreferredModeling(List<String> preferredModeling) {
        this.preferredModeling = preferredModeling;
    }

    public List<String> getPreferredRefinement() {
        return preferredRefinement;
    }

    public void setPreferredRefinement(List<String> preferredRefinement) {
        this.preferredRefinement = preferredRefinement;
    }
}
