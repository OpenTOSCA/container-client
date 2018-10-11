package org.opentosca.container.client.model;

public enum PlanInstanceOutputType {

    // TODO add types here.
    ;

    private String name;

    PlanInstanceOutputType(String name) {
        this.name = name;
    }

    public static PlanInstanceOutputType fromString(String name) {
        if (name != null) {
            for (final PlanInstanceOutputType type : PlanInstanceOutputType.values()) {
                if (name.equalsIgnoreCase(type.name)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Parameter 'name' does not match an Enum type");
    }
}
