package dev.createspellbook.client.ponder;

import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;

/** Generated authored captions; see scripts/generate_utility_ponder.py. */
public final class UtilityPonderTexts {
    private UtilityPonderTexts(){}
    public static void register(SharedTextRegistrationHelper helper){
        helper.registerSharedText("utility_portal_pair","手持同一组末影珍珠，依次右键两个漏斗的外框。");
        helper.registerSharedText("utility_portal_crossdim","配对可以跨维度，珍珠不消耗。");
        helper.registerSharedText("utility_portal_flow","物品从收取端进入，只传送到绑定的另一端。");
        helper.registerSharedText("utility_portal_wait","对端堵塞时保留物品；需要时临时加载目标区块。");
        helper.registerSharedText("utility_portal_unlink","潜行空手右键解绑；缓存里的物品会保留。");
        helper.registerSharedText("utility_portal_belt","像黄铜漏斗一样安装在传送带或置物台上。");
        helper.registerSharedText("utility_portal_input","向内的箭头表示收取：从带上拿取物品并送入传送门。");
        helper.registerSharedText("utility_portal_wrench","用 Create 扳手切换收取与输出，箭头会同时反向。");
        helper.registerSharedText("utility_portal_output","输出端将收到的物品放回下方传送带。");
        helper.registerSharedText("utility_portal_filter","过滤槽支持物品、列表过滤器以及精确或最多数量。");
        helper.registerSharedText("utility_portal_inline","沿传送带安装时，扳手切换延伸与回缩，作用方向跟随带的运动方向。");
        helper.registerSharedText("utility_firework_template","手持烟花右键侧面，复制完整配置，原烟花保留。");
        helper.registerSharedText("utility_firework_mana","自动恢复法力，也可以从背面管道补充。");
        helper.registerSharedText("utility_firework_launch","每次发射消耗 50 mB 法力，默认间隔 20 Tick。");
        helper.registerSharedText("utility_firework_redstone","红石信号会暂停发射；空手右键清除烟花模板。");
    }
}
