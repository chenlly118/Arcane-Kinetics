package dev.createspellbook.constructs;

import dev.createspellbook.Balance;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** A finite non-player cast using Iron's public lifecycle, shared by both hosts. */
public final class CastingSession {
    private final ConstructCaster caster;
    private AbstractSpell spell;
    private ItemStack template=ItemStack.EMPTY;
    private int level;
    private LivingEntity target;
    private boolean releasing;
    private int elapsed;
    public CastingSession(ConstructCaster caster) {this.caster=caster;}
    public boolean active() {return spell!=null;}
    public boolean releasing() {return releasing;}
    public LivingEntity target() {return target;}
    public int level() {return level;}
    public AbstractSpell spell() {return spell;}
    public boolean begin(LivingEntity attacker) {
        try{return beginChecked(attacker);}catch(RuntimeException | LinkageError error){
            var selected=caster.host()==null?java.util.Optional.<SpellData>empty():ScrollTemplate.read(caster.host().scroll().getStackInSlot(0));
            selected.ifPresent(data->MachineSpellCompatibility.failed(data.getSpell(),error));
            if(active())finish(true);else{caster.getMagicData().resetCastingState();target=null;caster.setTarget(null);}
            if(caster.host()!=null)caster.host().status(CastLedger.Status.UNSUPPORTED);return false;
        }
    }
    private boolean beginChecked(LivingEntity attacker) {
        var host=caster.host();if(host==null || active() || !host.operating())return false;
        var selected=ScrollTemplate.read(host.scroll().getStackInSlot(0));
        if(selected.isEmpty()){host.status(CastLedger.Status.NO_SCROLL);return false;}
        var data=selected.get();var candidate=data.getSpell();
        if(!ScrollTemplate.supported(candidate,data.getLevel())){host.status(CastLedger.Status.UNSUPPORTED);return false;}
        int effective=candidate.getLevelFor(data.getLevel(),caster);
        if(!ScrollTemplate.supported(candidate,effective)){host.status(CastLedger.Status.UNSUPPORTED);return false;}
        if(!candidate.isEnabled()){host.status(CastLedger.Status.DISABLED);return false;}
        long now=caster.level().getGameTime();
        if(host.ledger().remaining(candidate.getSpellId(),now)>0){host.status(CastLedger.Status.COOLDOWN);return false;}
        int cost=candidate.getManaCost(effective),duration=candidate.getEffectiveCastTime(effective,caster);
        if(cost<0 || duration<0 || duration>1200){host.status(CastLedger.Status.UNSUPPORTED);return false;}
        if(!host.canPay(cost,false)){host.status(CastLedger.Status.NO_MANA);return false;}
        if(!host.validCastTarget(attacker,candidate,effective)){host.status(CastLedger.Status.INVALID_TARGET);return false;}
        target=attacker;caster.setTarget(attacker);caster.aim();elapsed=0;
        var magic=caster.getMagicData();magic.setAdditionalCastData(attacker==null?null:new io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData(attacker));magic.setPlayerCastingItem(host.scroll().getStackInSlot(0).copy());
        if(attacker!=null && candidate.shouldAIStopCasting(effective,caster,attacker) || !candidate.checkPreCastConditions(caster.level(),effective,caster,magic)){
            target=null;caster.setTarget(null);host.status(CastLedger.Status.INVALID_TARGET);return false;
        }
        spell=candidate;level=effective;template=host.scroll().getStackInSlot(0).copy();
        magic.initiateCast(spell,level,duration,CastSource.MOB,"mainhand");
        dev.createspellbook.devices.SpellDeviceBridge.beginMachineCast(caster,spell,level);
        // Reserve even while unloaded. A half-finished cast is never replayed on load.
        host.ledger().hold(spell.getSpellId(),now+duration+cooldownTicks());host.status(CastLedger.Status.CASTING);host.constructChanged();
        ConstructSafety.duringCast(caster,()->spell.onServerPreCast(caster.level(),level,caster,magic));return true;
    }
    private long cooldownTicks() {return Math.max(1L,(long)Math.max(0,spell.getSpellCooldown())*caster.host().cooldownMultiplier());}
    public void tick() {
        try{tickChecked();}catch(RuntimeException | LinkageError error){
            var failed=spell;if(failed!=null)MachineSpellCompatibility.failed(failed,error);finish(true);
            if(caster.host()!=null)caster.host().status(CastLedger.Status.UNSUPPORTED);
        }
    }
    private void tickChecked() {
        if(!active())return;var host=caster.host();
        if(host==null){finish(true);return;}
        if(!host.operating() || !ItemStack.isSameItemSameComponents(template,host.scroll().getStackInSlot(0))) {finish(true);return;}
        if(!host.validCastTarget(target,spell,level)){host.status(CastLedger.Status.INVALID_TARGET);finish(true);return;}
        caster.aim();var magic=caster.getMagicData();magic.handleCastDuration();elapsed++;
        ConstructSafety.duringCast(caster,()->spell.onServerCastTick(caster.level(),level,caster,magic));
        if(!active())return;
        if(spell.getCastType()==CastType.CONTINUOUS){
            if((elapsed-1)%10==0 && magic.getCastDurationRemaining()>0)release();
            if(active() && magic.getCastDurationRemaining()<=0)finish(false);
            return;
        }
        if(magic.getCastDurationRemaining()>0)return;
        release();
        if(active())finish(false);
    }
    private void release(){
        var host=caster.host();var magic=caster.getMagicData();
        if(!spell.isEnabled() || !spell.checkPreCastConditions(caster.level(),level,caster,magic)) {host.status(CastLedger.Status.INVALID_TARGET);finish(true);return;}
        if(!host.canPay(spell.getManaCost(level),true)){host.status(CastLedger.Status.NO_MANA);finish(true);return;}
        releasing=true;
        try {
            host.ledger().lastRelease=caster.level().getGameTime();host.ledger().releases++;
            caster.released();host.constructChanged();ConstructSafety.duringCast(caster,()->spell.onCast(caster.level(),level,caster,CastSource.MOB,magic));
        } finally {releasing=false;}
    }
    public void finish(boolean cancelled) {
        if(!active())return;var ended=spell;int endedLevel=level;var host=caster.host();
        if(host!=null){host.ledger().hold(ended.getSpellId(),caster.level().getGameTime()+cooldownTicks());host.constructChanged();}
        spell=null;template=ItemStack.EMPTY;
        try {ConstructSafety.duringCast(caster,()->ended.onServerCastComplete(caster.level(),endedLevel,caster,caster.getMagicData(),cancelled));}
        catch(RuntimeException | LinkageError error){MachineSpellCompatibility.failed(ended,error);if(host!=null)host.status(CastLedger.Status.UNSUPPORTED);}
        finally {caster.getMagicData().resetCastingState();target=null;caster.setTarget(null);}
        if(host!=null && host.ledger().status()==CastLedger.Status.CASTING)host.status(CastLedger.Status.COOLDOWN);
    }
}
