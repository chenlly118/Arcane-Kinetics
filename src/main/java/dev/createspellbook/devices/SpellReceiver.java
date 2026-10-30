package dev.createspellbook.devices;

/** Public device integration entry point. Call on the logical server only. */
public interface SpellReceiver {
    void acceptSpell(SpellInteraction interaction);
}
