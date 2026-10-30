package dev.createspellbook.mixin;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import dev.createspellbook.processing.SpellTransportHistory;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

/** Preserve deduplication across native belt movement, processing copies and save/load. */
@Mixin(value=TransportedItemStack.class,remap=false)
public abstract class SpellTransportHistoryMixin implements SpellTransportHistory {
    @Unique private final Set<UUID> createSpellbook$casts=new LinkedHashSet<>();
    @Override public Set<UUID> createSpellbook$casts(){return createSpellbook$casts;}
    @Inject(method={"copy","getSimilar"},at=@At("RETURN"))
    private void createSpellbook$copy(CallbackInfoReturnable<TransportedItemStack> callback){
        ((SpellTransportHistory)callback.getReturnValue()).createSpellbook$casts().addAll(createSpellbook$casts);
    }
    @Inject(method="serializeNBT",at=@At("RETURN"))
    private void createSpellbook$write(HolderLookup.Provider lookup,CallbackInfoReturnable<CompoundTag> callback){
        if(createSpellbook$casts.isEmpty())return;var list=new ListTag();
        for(var id:createSpellbook$casts){var entry=new CompoundTag();entry.putUUID("Cast",id);list.add(entry);}
        callback.getReturnValue().put("CreateSpellbookProcessingCasts",list);
    }
    @Inject(method="read",at=@At("RETURN"))
    private static void createSpellbook$read(CompoundTag tag,HolderLookup.Provider lookup,CallbackInfoReturnable<TransportedItemStack> callback){
        var history=(SpellTransportHistory)callback.getReturnValue();
        for(var value:tag.getList("CreateSpellbookProcessingCasts",Tag.TAG_COMPOUND))if(value instanceof CompoundTag entry && entry.hasUUID("Cast"))history.createSpellbook$mark(entry.getUUID("Cast"));
    }
}
