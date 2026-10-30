package dev.createspellbook.constructs;

import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Absolute per-spell deadlines survive scroll swaps, unloading and physical recovery. */
public final class CastLedger {
    private final Map<String,Long> readyAt=new HashMap<>();
    public enum Status {NO_SCROLL,UNSUPPORTED,DISABLED,NO_MANA,UNPOWERED,COOLDOWN,CASTING,READY,INVALID_TARGET,NO_ROTATION,OVERSTRESSED,WAITING_PULSE,NO_RECIPE}
    private Status status=Status.NO_SCROLL;
    public long nextAttempt,lastRelease=Long.MIN_VALUE;
    public int releases;
    public Status status() {return status;}
    public boolean status(Status value) {boolean changed=status!=value;status=value;return changed;}
    public long remaining(String spell,long now) {return Math.max(0,readyAt.getOrDefault(spell,0L)-now);}
    public void hold(String spell,long until) {readyAt.merge(spell,until,Math::max);}
    public CompoundTag save() {
        var tag=new CompoundTag();var cooldowns=new CompoundTag();readyAt.forEach(cooldowns::putLong);
        tag.put("Cooldowns",cooldowns);tag.putLong("NextAttempt",nextAttempt);tag.putLong("LastRelease",lastRelease);tag.putInt("Releases",releases);return tag;
    }
    public void load(CompoundTag tag) {
        readyAt.clear();var cooldowns=tag.getCompound("Cooldowns");
        for(String id:cooldowns.getAllKeys())if(readyAt.size()<4096)readyAt.put(id,Math.max(0,cooldowns.getLong(id)));
        nextAttempt=tag.getLong("NextAttempt");lastRelease=tag.contains("LastRelease")?tag.getLong("LastRelease"):Long.MIN_VALUE;releases=Math.max(0,tag.getInt("Releases"));
        status=Status.NO_SCROLL;
    }
}
