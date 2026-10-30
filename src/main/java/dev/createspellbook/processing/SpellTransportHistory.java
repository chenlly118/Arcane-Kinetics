package dev.createspellbook.processing;

import java.util.Set;
import java.util.UUID;

/** Per transported stack, never a component on ordinary inventory items. */
public interface SpellTransportHistory {
    Set<UUID> createSpellbook$casts();
    default boolean createSpellbook$seen(UUID cast){return cast!=null && createSpellbook$casts().contains(cast);}
    default void createSpellbook$mark(UUID cast){
        if(cast==null)return;var casts=createSpellbook$casts();casts.add(cast);
        while(casts.size()>16)casts.remove(casts.iterator().next());
    }
}
