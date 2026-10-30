package dev.createspellbook.compat;

import dev.createspellbook.CreateSpellbook;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

@EventBusSubscriber(modid=CreateSpellbook.ID,value=Dist.CLIENT)
public final class SpellRecipeReload {
    @SubscribeEvent public static void reload(RecipesUpdatedEvent event){
        if(net.neoforged.fml.ModList.get().isLoaded("jei"))net.minecraft.client.Minecraft.getInstance().execute(ArcaneProcessingJei::refreshRecipes);
    }
}
