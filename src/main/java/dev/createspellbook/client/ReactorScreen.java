package dev.createspellbook.client;

import dev.createspellbook.reactor.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.Locale;

/** Fixed packet slot identities, server-validated pages; capacity follows the base. */
public final class ReactorScreen extends AbstractContainerScreen<ReactorMenu> {
    private Button start,previous,next;
    public ReactorScreen(ReactorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=390;imageHeight=327;inventoryLabelX=10;inventoryLabelY=231;}
    @Override protected void init(){
        super.init();
        start=addRenderableWidget(Button.builder(Component.translatable("gui.create_spellbook.reactor.start"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)).bounds(leftPos+10,topPos+52,178,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.create_spellbook.reactor.stop"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,2)).bounds(leftPos+202,topPos+52,178,20).build());
        previous=addRenderableWidget(Button.builder(Component.literal("<"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100+menu.page()-1)).bounds(leftPos+10,topPos+211,24,16).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100+menu.page()+1)).bounds(leftPos+148,topPos+211,24,16).build());
    }
    @Override protected void containerTick(){super.containerTick();if(start!=null)start.active=menu.formed() && menu.pairs()>0 && menu.accidentPhase()<2;if(previous!=null)previous.active=menu.page()>0;if(next!=null)next.active=menu.page()+1<menu.pages();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos-1,topPos-1,leftPos+imageWidth+1,topPos+imageHeight+1,0xff9f8a64);g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff242b3d);
        g.fill(leftPos+180,topPos+82,leftPos+382,topPos+318,0xff192131);
        for(int i=0;i<menu.slots.size();i++){var slot=menu.slots.get(i);if(!slot.isActive())continue;int x=leftPos+slot.x,y=topPos+slot.y;
            g.fill(x-1,y-1,x+17,y+17,0xff101622);g.fill(x,y,x+16,y+16,i<289 && i>=menu.capacity()?0xff292c34:0xff50596d);
        }
    }
    private void line(GuiGraphics g,Component text,int y,int color){g.drawString(font,font.substrByWidth(text,194).getString(),184,y,color,false);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,10,8,0xffeee5d4,false);
        var status=Component.translatable("gui.create_spellbook.reactor.structure",Component.translatable("reactor.create_spellbook.issue."+menu.issue().name().toLowerCase(Locale.ROOT)));
        g.drawString(font,font.substrByWidth(status,370).getString(),10,23,menu.formed()?0xff9cd6bc:0xffffb5a5,false);
        g.drawString(font,menu.accidentPhase()>=2?Component.translatable("gui.create_spellbook.reactor.physical_radius",String.format(Locale.ROOT,"%.1f",menu.accidentRadius())):Component.translatable("gui.create_spellbook.reactor.field_size",String.format(Locale.ROOT,"%.1f",menu.sizePercent()),menu.side()),10,38,menu.sizePercent()>50?0xffffb5a5:0xffbddeff,false);
        g.drawString(font,Component.translatable("gui.create_spellbook.reactor.slot_capacity",menu.capacity()),10,84,0xffdbd2ff,false);
        g.drawString(font,Component.literal((menu.page()+1)+" / "+menu.pages()),74,215,0xffdbd2ff,false);
        line(g,Component.translatable("reactor.create_spellbook.run."+menu.state().name().toLowerCase(Locale.ROOT)),89,0xffe5ddff);
        line(g,Component.translatable("gui.create_spellbook.reactor.core_pairs",menu.reactions(),menu.energies(),menu.pairs()),105,0xffbddeff);
        line(g,Component.translatable("gui.create_spellbook.reactor.capacity",String.format(Locale.ROOT,"%,d",menu.actual())),121,0xffbddeff);
        line(g,Component.translatable("gui.create_spellbook.reactor.rpm",menu.rpm()),137,0xffbddeff);
        line(g,Component.translatable("gui.create_spellbook.reactor.mana_stored",menu.mana(),menu.manaCapacity()),153,0xffbddeff);
        line(g,Component.translatable("gui.create_spellbook.reactor.mana_rate",String.format(Locale.ROOT,"%.0f",menu.manaRate())),169,0xffbddeff);
        line(g,Component.translatable("gui.create_spellbook.reactor.active",menu.working(),menu.required()),185,0xffe5ddff);
        line(g,Component.translatable("gui.create_spellbook.reactor.waste_stored",menu.matter(),ReactorResources.MATTER_CAPACITY),201,0xffd5b8f3);
        line(g,Component.translatable("gui.create_spellbook.reactor.matter_rate",String.format(Locale.ROOT,"%.2f",menu.matterRate())),217,0xffd5b8f3);
        var alarm=Component.translatable("reactor.create_spellbook.alarm."+menu.alarm().name().toLowerCase(Locale.ROOT));
        if(menu.formed() && menu.pairs()==0)alarm=Component.translatable("reactor.create_spellbook.alarm.no_cores");
        if(menu.accidentPhase()==1)alarm=Component.translatable("gui.create_spellbook.reactor.growing");
        if(menu.accidentPhase()>=2)alarm=Component.translatable("gui.create_spellbook.reactor.accident",Component.translatable("reactor.create_spellbook.accident.phase."+menu.accidentPhase()),menu.accidentSeconds());
        var lines=font.split(alarm,190);for(int i=0;i<Math.min(4,lines.size());i++)g.drawString(font,lines.get(i),184,238+i*11,0xffffc0a3,false);
        if(menu.reserves()>0)line(g,Component.translatable("gui.create_spellbook.reactor.overflow",menu.reserves()),294,0xffffb5a5);
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xffe5ddff,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
