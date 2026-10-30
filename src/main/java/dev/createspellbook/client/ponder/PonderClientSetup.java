package dev.createspellbook.client.ponder;

import net.createmod.ponder.foundation.PonderIndex;

/**
 * Adds the chapter to Ponder's plugin list during client mod construction,
 * before Ponder's own client setup runs its single registration pass.
 */
public final class PonderClientSetup {
    private static boolean registered;

    private PonderClientSetup() {}

    public static void init() {
        if (registered) return;
        registered = true;
        PonderIndex.addPlugin(new CreateSpellbookPonderPlugin());
    }
}
