package dev.createspellbook.constructs;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import java.util.UUID;

public interface ConstructHost {
    ScrollTemplate scroll();
    CastLedger ledger();
    ConstructCaster caster();
    Level constructLevel();
    Vec3 constructPosition();
    UUID ownerId();
    boolean operating();
    boolean canPay(int mana,boolean execute);
    int cooldownMultiplier();
    void constructChanged();
    default int storedMana() {return 0;}
    default int manaCapacity() {return 0;}
    default int liquidCost(int mana) {
        double amount=Math.ceil(Math.max(0,mana)*dev.createspellbook.Balance.ARM_MB_PER_MANA.get());return amount>=Integer.MAX_VALUE?Integer.MAX_VALUE:(int)amount;
    }
    default boolean validCastTarget(net.minecraft.world.entity.LivingEntity target,io.redspace.ironsspellbooks.api.spells.AbstractSpell spell,int level) {return true;}
    default boolean canConfigure(Player player) {return player.isAlive() && player.level()==constructLevel() && player.distanceToSqr(constructPosition())<=64
            && (ownerId()==null || ownerId().equals(player.getUUID()) || player.isCreative());}
    default void status(CastLedger.Status value) {if(ledger().status(value))constructChanged();}
}
