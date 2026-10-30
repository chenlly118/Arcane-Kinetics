# 法术加工与魔化置物台（0.2.2）

正式名称为“魔化置物台”；原方块、物品和方块实体 ID `create_spellbook:arcane_depot` 保留。普通 Create 置物台 `create:depot`、魔化置物台及 Create 传送带使用同一套服务端法术加工规则。自动施法机器正式称为“奥术施法装置”，保留 ID `create_spellbook:arcane_casting_arm`。

魔化置物台继承 Create 6.0.9 的 `DepotBlock` / `DepotBlockEntity`，使用原生持物槽、八格加工输出缓存、交互、掉落、比较器、水浸、扳手、传送和加工行为。所有面暴露与原生置物台相同的物品能力，不再保留旧版顶部输入／底部仅输出的双槽机制。模型和贴图不变；无序合成为安山机壳 ×1 + 铁魔法奥术锭 ×1。

旧存档的 `Inventory` 两槽分别迁移为原生 `HeldItem` 和 `OutputBuffer`。转换后仅保存原生格式，重复读档不会再次导入旧库存。旧方块状态中的 `facing` / `powered` 属性保留，新增原生 `waterlogged` 属性；不删除注册内容。

## 默认配方

| 产物 | 输入与条件 | 配方 ID（命名空间均为 `create_spellbook`） |
|---|---|---|
| 灰烬源质 `irons_spellbooks:cinder_essence` | 灵魂沙 1；安装烈焰粉 → 史诗及以上火焰法术 → 压制；循环 3 轮 | `sequenced_assembly/cinder_essence` |
| 寻魂烬铃 `irons_spellbooks:cinderous_soulcaller` | 钟 1；满级或超限火焰吐息 `irons_spellbooks:fire_breath` | `arcane_processing/cinderous_soulcaller` |
| 雷电瓶 `irons_spellbooks:lightning_bottle` | 玻璃瓶 1；卓越及以上雷电学派 | `arcane_processing/lightning_bottle` |
| 冰封骨头 `irons_spellbooks:frozen_bone` | 骨头 1；冰学派 | `arcane_processing/frozen_bone` |
| 回响碎片 `minecraft:echo_shard` | 紫水晶碎片 1；邪术冲击波 `irons_spellbooks:eldritch_blast` | `arcane_processing/echo_shard` |

“巫术射线”经用户确认指当前版本的“邪术冲击波”。实际学派为 `irons_spellbooks:fire`、`irons_spellbooks:lightning`、`irons_spellbooks:ice`。本体将 `uncommon` 翻译为“卓越”，`epic` 翻译为“史诗”。稀有度调用铁魔法实际的 `getRarity(level)`；满级调用当前配置的 `getMaxLevel()`，不硬编码为 10。超限等级满足满级要求，稀有度按本体最高等级的品质判断。

已有 `arcane_processing/counterspell_stabilizer` 继续保留：法术反制命中奥术构件获得黑洞稳定质，也能在普通置物台上执行。旧铁锭拆解、黏土烧砖示例已移到 `src/gametest/resources`，正式 JAR 不包含它们。

## 独立加工 JSON

1.21.1 使用单数目录 `data/<namespace>/recipe/<path>.json`。

```json
{
  "type": "create_spellbook:arcane_processing",
  "ingredient": {"item": "minecraft:glass_bottle"},
  "input_count": 1,
  "school": "irons_spellbooks:lightning",
  "min_rarity": "uncommon",
  "min_level": 1,
  "require_max_level": false,
  "priority": 0,
  "result": {"id": "irons_spellbooks:lightning_bottle", "count": 1}
}
```

| 字段 | 规则 |
|---|---|
| `ingredient` | 原生非空 Ingredient；支持物品、物品标签、原生可选列表及 NeoForge 扩展 |
| `input_count` | 默认 1，范围 1～64 |
| `spell` / `school` | 至少指定一个；同时指定则同时满足，使用完整注册 ID |
| `min_level` | 默认 1；接受超限等级，未知等级不加工 |
| `min_rarity` | 可选：`common`、`uncommon`、`rare`、`epic`、`legendary` |
| `require_max_level` | 默认 false；true 时要求当前配置中的满级或更高等级 |
| `priority` | 默认 0；数值越大优先级越高，同优先级按完整配方 ID 排序 |
| `result` | 原生 ItemStack，支持组件和数量；必须容纳于一个合法堆叠 |

输出空间不足时不消耗输入。每次有效施法在同一接收面最多完成一条配方；持续伤害不会对同一批物品连续重复加工，新的施法可以继续加工下一批。去重同时跟随 Create 的 `TransportedItemStack`，在其复制、移动和 NBT 存档中保留最近 16 个施法标记，不向普通物品添加影响堆叠的组件。弹丸和命中事件仍有独立的有界缓存。

## Create 原生序列装配步骤

法术步骤类型为 `create_spellbook:spell_application`，实现真正的 `ProcessingRecipe` 和 `IAssemblyRecipe`。序列进度、原料消耗、循环次数及最终输出由 Create 的原生配方机制管理，不自行模拟装配进度。

```json
{
  "type": "create_spellbook:spell_application",
  "ingredients": [{"item": "create_spellbook:incomplete_cinder_essence"}],
  "school": "irons_spellbooks:fire",
  "min_rarity": "epic",
  "results": [{"id": "create_spellbook:incomplete_cinder_essence"}]
}
```

法术条件字段与独立加工共用 `SpellRequirement`。原生序列步骤每次推进一个输入；此处的 `ingredients` / `results` 使用 Create 格式，产物字段为 `id`。该类型用于序列中的步骤，独立配方应使用 `arcane_processing`。

灰烬源质只新增一个必要的未完成品 `create_spellbook:incomplete_cinder_essence`，使用原生 `SequencedAssemblyItem`。默认序列为：

```json
{
  "type": "create:sequenced_assembly",
  "ingredient": {"item": "minecraft:soul_sand"},
  "loops": 3,
  "transitional_item": {"id": "create_spellbook:incomplete_cinder_essence"},
  "results": [{"id": "irons_spellbooks:cinder_essence", "count": 1}],
  "sequence": [
    {
      "type": "create:deploying",
      "ingredients": [
        {"item": "create_spellbook:incomplete_cinder_essence"},
        {"item": "minecraft:blaze_powder"}
      ],
      "results": [{"id": "create_spellbook:incomplete_cinder_essence"}]
    },
    {
      "type": "create_spellbook:spell_application",
      "ingredients": [{"item": "create_spellbook:incomplete_cinder_essence"}],
      "school": "irons_spellbooks:fire",
      "min_rarity": "epic",
      "results": [{"id": "create_spellbook:incomplete_cinder_essence"}]
    },
    {
      "type": "create:pressing",
      "ingredients": [{"item": "create_spellbook:incomplete_cinder_essence"}],
      "results": [{"id": "create_spellbook:incomplete_cinder_essence"}]
    }
  ]
}
```

有装配进度的半成品只执行该序列当前步骤，不会落入无关的普通加工配方。移除父序列后，原有半成品保留在库存中并等待有效配方，不删除物品或伪造完成结果。

## 自动施法、JEI 与重载

奥术施法装置使用加工模式、足够的动力和液态法力，沿正面寻找有匹配配方的普通置物台、魔化置物台或传送带。保留铁魔法真实的前置条件、施法时间、魔耗和冷却。传送带速度必须给吟唱和弹丸飞行留出时间，过快时物品会离开瞄准位置，机器会取消不再有效的施法。

JEI 独立加工分类显示输入、输出、满足条件的真实卷轴以及奥术施法装置。学派条件循环显示符合条件的法术示例，卷轴为不消耗的催化展示；法术仍须实际命中才能触发。原生 Create 序列组装页面的法术步骤显示奥术施法装置及真实卷轴，不把卷轴列为消耗材料。

使用同 ID 的高优先级数据包覆盖默认配方；新增文件增加配方。若需通过纯数据包禁用某条默认配方，可同 ID 覆盖为：

```json
{"neoforge:conditions": [{"type": "neoforge:false"}], "type": "create_spellbook:arcane_processing"}
```

随后执行 `/reload`。服务端不缓存跨重载的配方对象，客户端会重新同步条件并刷新 JEI 可见配方。JEI 19 的公开 API 提供隐藏／恢复配方而不提供物理删除，因此旧展示对象会隐藏到本次运行结束，不再参与可见查询。

KubeJS 可使用原生 JSON：

```js
ServerEvents.recipes(event => {
  event.remove({ id: 'create_spellbook:arcane_processing/frozen_bone' })
  event.custom({
    type: 'create_spellbook:arcane_processing',
    ingredient: { tag: 'minecraft:logs' },
    input_count: 2,
    school: 'irons_spellbooks:ice',
    min_rarity: 'rare',
    result: { id: 'minecraft:packed_ice', count: 1 }
  }).id('example:chilled_logs')
})
```

已测试真实数据包的添加、覆盖、条件禁用、撤除、标签和数量，以及原生 JEI 重载；本轮没有额外加载 KubeJS 验证其专用脚本构建器。

兼容边界：经过现有桥接器的真实弹丸、射线、范围和持续伤害可触发。无命中行为的自身增益、完全自定义的第三方法术伤害源、无法获取等级的延迟效果仍需专门适配；不会推测未知等级。封闭自充能的奥术反应装置继续禁止把免费反击转为加工产物。
