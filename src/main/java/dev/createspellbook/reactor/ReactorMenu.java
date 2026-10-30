package dev.createspellbook.reactor;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;

/** Paged, fixed-identity slots and size-based telemetry. No manual SU override.
 * All settings go through vanilla's authenticated current-menu packets. */
public final class ReactorMenu extends AbstractContainerMenu {
    public static final int DATA=37,PAGE_SIZE=54;

    private final ReactorControllerBlockEntity controller;
    private final net.minecraft.core.BlockPos access;
    private final ContainerData data;
    public ReactorMenu(int id,Inventory player){this(id,player,null,null,new ReactorStabilizers(()->{}),new SimpleContainerData(DATA));}
    public ReactorMenu(int id,Inventory player,ReactorControllerBlockEntity controller){this(id,player,controller,controller.getBlockPos());}
    public ReactorMenu(int id,Inventory player,ReactorControllerBlockEntity controller,net.minecraft.core.BlockPos access){this(id,player,controller,access,controller.operation.stabilizers,new ContainerData(){
        private int selectedPage;
        @Override public int get(int index){var op=controller.operation;int rateM=(int)Math.round(op.manaRate()*1000),rateW=(int)Math.round(op.matterRate()*1000);return switch(index){
            case 0->op.target()&65535;case 1->op.target()>>>16;case 2->op.actual()&65535;case 3->op.actual()>>>16;case 4->op.state().ordinal();case 5->op.requested()?1:0;case 6->op.working();case 7->op.reserves();case 8->op.manaStored()&65535;case 9->op.manaStored()>>>16;case 10->op.storedMatter()&65535;case 11->op.storedMatter()>>>16;case 12->op.alarm().ordinal();
            case 13->controller.isFormed()?1:0;case 14->controller.issue().ordinal();case 15->op.required();case 16->op.rpm();case 17->op.manaCapacity()&65535;case 18->op.manaCapacity()>>>16;case 19->rateM&65535;case 20->rateM>>>16;case 21->rateW&65535;case 22->rateW>>>16;case 23->op.legacyPending()&65535;case 24->op.legacyPending()>>>16;case 25->op.unstable()?1:0;case 26->op.accidentPhase();case 27->op.accidentSeconds();case 28->op.sizeUnits();case 29->controller.lastLayout()==null?0:controller.lastLayout().size();case 30->op.pairs();case 31->op.reactionCount();case 32->op.energyCount();case 33->op.stabilizers.activeSlots();case 34->selectedPage;case 35->op.reserves();case 36->(int)Math.round(op.accidentRadius()*10);default->0;};}
        @Override public void set(int index,int value){if(index==34)selectedPage=value;}
        @Override public int getCount(){return DATA;}
    });}
    private ReactorMenu(int id,Inventory player,ReactorControllerBlockEntity controller,net.minecraft.core.BlockPos access,IItemHandler inventory,ContainerData data){
        super(ReactorContent.CONTROL_MENU.get(),id);this.controller=controller;this.access=access;this.data=data;
        for(int index=0;index<ReactorStabilizers.MAX_SLOTS;index++){
            final int slotIndex=index;int cell=index%PAGE_SIZE;
            addSlot(new SlotItemHandler(inventory,index,10+(cell%9)*18,101+(cell/9)*18){
                @Override public boolean isActive(){return page()==slotIndex/PAGE_SIZE;}
                @Override public boolean mayPlace(ItemStack stack){return isActive() && slotIndex<capacity() && super.mayPlace(stack);}
                @Override public boolean mayPickup(Player player){return isActive() && (controller==null || stillValid(player));}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,9+row*9+col,10+col*18,243+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(player,col,10+col*18,301));addDataSlots(data);
    }
    public double accidentRadius(){return data.get(36)/10d;}
    public int page(){return data.get(34);}
    public int pages(){return Math.max(1,(Math.max(capacity(),lastOccupied())+PAGE_SIZE-1)/PAGE_SIZE);}
    private int lastOccupied(){int last=0;for(int i=0;i<ReactorStabilizers.MAX_SLOTS;i++)if(!slots.get(i).getItem().isEmpty())last=i+1;return last;}
    public int capacity(){return Math.clamp(data.get(33),9,289);}
    public int side(){return data.get(29);}
    public int pairs(){return data.get(30);}
    public int reactions(){return data.get(31);}
    public int energies(){return data.get(32);}
    public double sizePercent(){return data.get(28)/10d;}
    public int number(int low){return (data.get(low)&65535)|((data.get(low+1)&65535)<<16);}
    public int target(){return number(0);}
    public int actual(){return number(2);}
    public int working(){return data.get(6);}
    public int reserves(){return data.get(7);}
    public int mana(){return number(8);}
    public int matter(){return number(10);}
    public boolean formed(){return data.get(13)!=0;}
    public int required(){return data.get(15);}
    public int rpm(){return data.get(16);}
    public int manaCapacity(){return number(17);}
    public double manaRate(){return number(19)/1000d;}
    public double matterRate(){return number(21)/1000d;}
    public int legacyPending(){return number(23);}
    public int accidentPhase(){return Math.clamp(data.get(26),0,3);}
    public int accidentSeconds(){return Math.max(0,data.get(27));}
    public ReactorOperation.Alarm alarm(){return ReactorOperation.Alarm.values()[Math.clamp(data.get(12),0,ReactorOperation.Alarm.values().length-1)];}
    public ReactorScanner.Issue issue(){return ReactorScanner.Issue.values()[Math.clamp(data.get(14),0,ReactorScanner.Issue.values().length-1)];}
    public ReactorOperation.State state(){return ReactorOperation.State.values()[Math.clamp(data.get(4),0,ReactorOperation.State.values().length-1)];}
    @Override public void broadcastChanges(){if(controller!=null && page()>=pages())data.set(34,0);super.broadcastChanges();}
    @Override public boolean stillValid(Player player){
        if(controller==null)return true;var level=controller.getLevel();
        if(controller.isRemoved() || level==null || player.level()!=level || !level.hasChunkAt(controller.getBlockPos()) || !level.hasChunkAt(access) || level.getBlockEntity(controller.getBlockPos())!=controller || player.distanceToSqr(access.getCenter())>64)return false;
        return access.equals(controller.getBlockPos()) || level.getBlockEntity(access) instanceof ReactorPartBlockEntity part && part.role==ReactorRole.STABILIZER && part.reactorBinding().ownedBy(controller) && part.reactorBinding().valid();
    }
    @Override public boolean clickMenuButton(Player player,int action){
        if(controller==null || player.isSpectator() || !stillValid(player))return false;
        if(action==1){if(!controller.isFormed())return false;controller.operation.start();}else if(action==2)controller.operation.stop();else if(action>=100 && action<100+6){int page=action-100;if(page>=pages())return false;data.set(34,page);}else return false;return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(controller!=null && !stillValid(player))return ItemStack.EMPTY;
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<ReactorStabilizers.MAX_SLOTS){if(index/PAGE_SIZE!=page() || !moveItemStackTo(stack,ReactorStabilizers.MAX_SLOTS,slots.size(),true))return ItemStack.EMPTY;}
        else {
            if(!ReactorStabilizers.usable(stack) || controller==null)return ItemStack.EMPTY;
            var rest=ItemHandlerHelper.insertItemStacked(controller.operation.stabilizers,stack.copy(),false);
            if(rest.getCount()==stack.getCount())return ItemStack.EMPTY;stack.setCount(rest.getCount());
        }
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
    @Override public void clicked(int slot,int button,ClickType action,Player player){if(slot>=0 && slot<ReactorStabilizers.MAX_SLOTS && slot/PAGE_SIZE!=page())return;if(controller==null || stillValid(player))super.clicked(slot,button,action,player);}
}
