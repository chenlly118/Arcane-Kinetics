package dev.createspellbook.devices;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

/** Actual configured Iron rarity/maximum level, shared by recipes and assembly steps. */
public record SpellRequirement(Optional<ResourceLocation> spell,Optional<ResourceLocation> school,int minLevel,
        Optional<SpellRarity> minRarity,boolean requireMaxLevel) {
    public static final MapCodec<SpellRequirement> CODEC=RecordCodecBuilder.<SpellRequirement>mapCodec(i->i.group(
            ResourceLocation.CODEC.optionalFieldOf("spell").forGetter(SpellRequirement::spell),
            ResourceLocation.CODEC.optionalFieldOf("school").forGetter(SpellRequirement::school),
            Codec.intRange(1,Integer.MAX_VALUE).optionalFieldOf("min_level",1).forGetter(SpellRequirement::minLevel),
            SpellRarity.CODEC.optionalFieldOf("min_rarity").forGetter(SpellRequirement::minRarity),
            Codec.BOOL.optionalFieldOf("require_max_level",false).forGetter(SpellRequirement::requireMaxLevel)
    ).apply(i,SpellRequirement::new)).validate(r->r.spell.isEmpty() && r.school.isEmpty()?DataResult.error(()->"Specify spell or school"):DataResult.success(r));
    public static final StreamCodec<RegistryFriendlyByteBuf,SpellRequirement> STREAM_CODEC=new StreamCodec<>(){
        @Override public SpellRequirement decode(RegistryFriendlyByteBuf b){
            var spell=b.readBoolean()?Optional.of(b.readResourceLocation()):Optional.<ResourceLocation>empty();
            var school=b.readBoolean()?Optional.of(b.readResourceLocation()):Optional.<ResourceLocation>empty();
            int min=b.readVarInt();var rarity=b.readBoolean()?Optional.of(b.readEnum(SpellRarity.class)):Optional.<SpellRarity>empty();
            return new SpellRequirement(spell,school,min,rarity,b.readBoolean());
        }
        @Override public void encode(RegistryFriendlyByteBuf b,SpellRequirement r){
            b.writeBoolean(r.spell.isPresent());r.spell.ifPresent(b::writeResourceLocation);
            b.writeBoolean(r.school.isPresent());r.school.ifPresent(b::writeResourceLocation);b.writeVarInt(r.minLevel);
            b.writeBoolean(r.minRarity.isPresent());r.minRarity.ifPresent(b::writeEnum);b.writeBoolean(r.requireMaxLevel);
        }
    };
    public boolean accepts(ResourceLocation id,ResourceLocation schoolId,int level){
        if(level<minLevel || spell.filter(s->!s.equals(id)).isPresent() || school.filter(s->!s.equals(schoolId)).isPresent())return false;
        var actual=SpellRegistry.getSpell(id.toString());
        if(actual==SpellRegistry.none() || !actual.getSpellResource().equals(id) || !actual.getSchoolType().getId().equals(schoolId))return false;
        return (!requireMaxLevel || level>=actual.getMaxLevel())
                && minRarity.map(r->actual.getRarity(Math.min(level,actual.getMaxLevel())).getValue()>=r.getValue()).orElse(true);
    }
    public boolean accepts(SpellInteraction hit){return accepts(hit.spellId(),hit.schoolId(),hit.level());}
}
