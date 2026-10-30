package dev.createspellbook.reactor;

/** Structural identities only. Resource handling and power generation are a later phase. */
public enum ReactorRole {
    CASING("arcane_casing"),GLASS("arcane_reinforced_glass"),OUTPUT("reactor_output"),MANA("reactor_mana_interface"),
    CONTROLLER("reactor_controller"),STABILIZER("reactor_port"),REACTION_CORE("black_hole_reaction_core"),
    ENERGY_CORE("black_hole_energy_core"),SHAFT("black_hole_reaction_shaft");
    public final String id;
    ReactorRole(String id){this.id=id;}
    public boolean shell(){return this==CASING || this==GLASS || this==OUTPUT || this==MANA || this==CONTROLLER || this==STABILIZER;}
    public boolean linked(){return this!=CASING && this!=GLASS && this!=CONTROLLER;}
}
