package dev.createspellbook.constructs;

import io.redspace.ironsspellbooks.api.spells.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Registry-wide non-player adapter, with narrow semantic and ABI exclusions. */
public final class MachineSpellCompatibility {
    // Stationary machines cannot use player inventories, movement, weapon enchantments
    // or self-only utility buffs. These are exclusions, never a ten-spell allow-list.
    private static final Set<String> PERSONAL=Set.of(
        "none","echoing_strikes","flaming_strike","shadow_slash","volt_strike","divine_smite",
        "touch_dig","heartstop","teleport","recall","blood_step","frost_step","burning_dash",
        "thunder_step","evasion","charge","ascension","angel_wing","angel_wings","portal",
        "pocket_dimension","summon_ender_chest","summon_horse","summon_polar_bear","sacrifice",
        "invisibility","haste","spider_aspect","heal","greater_heal","healing_circle","fortify",
        "blessing_of_life","cleanse","planar_sight","abyssal_shroud","oakskin","raise_hell",
        "piston_launch","brass_exoskeleton","mechanic","mechanical_master","stress_drive");
    private static final Set<String> FAILED=new HashSet<>();
    private static final Map<Class<?>,Boolean> ABI=new HashMap<>();
    private static final Set<String> HOOKS=Set.of("onCast","checkPreCastConditions","onServerPreCast","onServerCastTick","onServerCastComplete");
    public static boolean supports(AbstractSpell spell,int level){
        if(level<1 || level>dev.createspellbook.Balance.CONSTRUCT_MAX_LEVEL.get() || FAILED.contains(spell.getSpellId())
                || PERSONAL.contains(spell.getSpellResource().getPath()))return false;
        var type=spell.getCastType();
        return (type==CastType.INSTANT || type==CastType.LONG || type==CastType.CONTINUOUS)
                && ABI.computeIfAbsent(spell.getClass(),MachineSpellCompatibility::nonPlayerHooks);
    }
    private static boolean playerType(String name){return name.equals("net/minecraft/server/level/ServerPlayer") || name.equals("net/minecraft/world/entity/player/Player");}
    private static boolean nonPlayerHooks(Class<?> type){
        // Catch direct player casts before invoking foreign code. Guarded instanceof
        // branches remain usable, as in Iron's native non-player implementations.
        try{
            for(var current=type;current!=null && current!=AbstractSpell.class;current=current.getSuperclass()){
                try(var stream=current.getResourceAsStream("/"+current.getName().replace('.','/')+".class")){
                    if(stream==null)return false;
                    var node=new ClassNode();new ClassReader(stream).accept(node,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
                    for(var method:node.methods)if(HOOKS.contains(method.name)){
                        for(var insn:method.instructions)if(insn instanceof TypeInsnNode cast && cast.getOpcode()==Opcodes.CHECKCAST && playerType(cast.desc)){
                            boolean guarded=false;int distance=0;
                            for(var previous=insn.getPrevious();previous!=null && distance<16;previous=previous.getPrevious()){
                                if(previous.getOpcode()>=0)distance++;
                                if(previous instanceof TypeInsnNode check && check.getOpcode()==Opcodes.INSTANCEOF && playerType(check.desc)){guarded=true;break;}
                            }
                            if(!guarded)return false;
                        }
                    }
                }
            }
            return true;
        }catch(java.io.IOException | RuntimeException error){return false;}
    }
    public static void failed(AbstractSpell spell,Throwable error){
        if(FAILED.add(spell.getSpellId()))org.apache.logging.log4j.LogManager.getLogger("CreateSpellbookMachines")
                .error("Disabled incompatible machine spell {} ({}) until restart",spell.getSpellId(),spell.getClass().getName(),error);
    }
    private MachineSpellCompatibility(){}
}
