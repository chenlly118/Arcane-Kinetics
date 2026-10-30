package dev.createspellbook.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/** Persisted addresses are display hints, never authority to resume a reactor. */
public final class ReactorBinding {
    private ReactorControllerBlockEntity owner;
    private BlockPos address;
    private UUID identity;
    private final Runnable changed;
    public ReactorBinding(Runnable changed){this.changed=changed;}
    public ReactorControllerBlockEntity owner(){return owner;}
    public BlockPos address(){return address;}
    public boolean valid(){return owner!=null && !owner.isRemoved() && owner.isFormed() && owner.identity().equals(identity);}
    public boolean ownedBy(ReactorControllerBlockEntity controller){return owner==controller;}
    public void bind(ReactorControllerBlockEntity controller){owner=controller;address=controller.getBlockPos();identity=controller.identity();changed.run();}
    public void clear(){owner=null;address=null;identity=null;changed.run();}
    public void suspended(){var prior=owner;clear();if(prior!=null)prior.requestValidation();}
    public void write(CompoundTag tag){if(address!=null)tag.putLong("ReactorController",address.asLong());if(identity!=null)tag.putUUID("ReactorIdentity",identity);}
    public void read(CompoundTag tag){owner=null;address=tag.contains("ReactorController")?BlockPos.of(tag.getLong("ReactorController")):null;identity=tag.hasUUID("ReactorIdentity")?tag.getUUID("ReactorIdentity"):null;}
}
