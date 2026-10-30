package dev.createspellbook.constructs;

/** Stable saved IDs; changing mode never resets the shared per-spell cooldown. */
public enum EmitterMode {
    PULSE,TURRET,PROCESSING;
    public static EmitterMode fromId(int id) {return id>=0 && id<values().length?values()[id]:PULSE;}
    public EmitterMode next() {return values()[(ordinal()+1)%values().length];}
}
