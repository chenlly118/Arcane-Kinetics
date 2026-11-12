package dev.createspellbook.client.ponder;

import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;

/** Generated concise tutorial captions. */
public final class BriefPonderTexts {
    private BriefPonderTexts(){}
    public static void register(SharedTextRegistrationHelper helper){
        helper.registerSharedText("brief_reactor_frame","外壳为 5～19 格边长的立方体；棱边使用奥术机壳，内部留空。");
        helper.registerSharedText("brief_reactor_ports","侧面放一个控制器；法力、稳定质与废料接口各至少一个，应力从侧面引出。");
        helper.registerSharedText("brief_reactor_cores","反应核心装在顶面，能量核心装在底面；相同数量组成有效核心对。");
        helper.registerSharedText("brief_reactor_run","接入液态法力并启动。中央黑洞默认稳定在 80%，侧面传动轴输出动力。");
        helper.registerSharedText("brief_reactor_supply","稳定质放入端口，坍缩物质及时排出；每对核心至少一个有效稳定质。");
        helper.registerSharedText("brief_reactor_safety","缺稳定质、废料堵塞或结构破坏会使黑洞膨胀；超过 100% 失控，及时修复或停机。");
        helper.registerSharedText("brief_arm_setup","传动轴接动力，侧面接口或紫环接法力。手持卷轴右键配置，空手取出。");
        helper.registerSharedText("brief_arm_modes","点击置物台或传送带顶部可朝下安装，自动进入加工模式。扳手可切换模式。");
        helper.registerSharedText("brief_arm_cooldown","施法同时受法力、频率和原法术冷却限制；空手右键取出卷轴。");
        helper.registerSharedText("brief_basin_spell","攻击法术命中聚魔盆后按实际伤害产法力，默认每点伤害 10 mB。");
        helper.registerSharedText("brief_basin_pipe","使用 Create 管道和动力泵抽取；盆满时暂停产液。");
        helper.registerSharedText("brief_basin_stack","将奥术反应装置或烟花反应装置放在盆上方，可持续吸收其中的法力。");
        helper.registerSharedText("brief_depot_input","把待加工物品放在置物台上，使用符合配方条件的法术命中它。");
        helper.registerSharedText("brief_depot_process","例如：骨头受到冰学派法术加工，得到冰封之骨；可由奥术施法装置自动完成。");
        helper.registerSharedText("brief_depot_parity","普通置物台、魔化置物台和传送带共享配方；机械臂、漏斗与常规加工照常使用。");
        helper.registerSharedText("brief_printer_config","侧面装卷轴或列表过滤器；印刷头装对应学派材料，上方输入合适品质的墨水。");
        helper.registerSharedText("brief_printer_work","在下方两格的置物台或传送带放纸，印刷头下压后产出卷轴。");
        helper.registerSharedText("brief_printer_automation","模板和学派材料可重复使用。用扳手切换升级模式，供入下一级所需品质的墨水。");
        helper.registerSharedText("brief_caster_regen","奥术反应装置通过收集大气中的法力来回复法力");
        helper.registerSharedText("brief_caster_work","手持卷轴右键配置，空手取出；法力和冷却允许时自动攻击敌对生物。");
        helper.registerSharedText("brief_caster_basin","下方紧贴聚魔盆可吸收法力，默认每 Tick 1 mB；烟花反应装置同样适用。");
        helper.registerSharedText("brief_portal_pair","使用同一组末影珍珠依次点击两端配对；向内箭头收取，向外箭头输出，可跨维度。");
        helper.registerSharedText("brief_portal_flow","物品进入输入漏斗后，从绑定的输出端返回传送带；堵塞时保留在缓存中。");
        helper.registerSharedText("brief_portal_filter","扳手反转收发方向；过滤槽接受物品或过滤器，潜行空手右键解绑。");
        helper.registerSharedText("brief_firework_config","侧面右键复制烟花配置；装置收集大气法力，也支持管道补充。");
        helper.registerSharedText("brief_firework_work","每次消耗 50 mB 法力，默认每 20 Tick 发射一枚真实烟花。");
        helper.registerSharedText("brief_firework_stop","红石暂停发射；空手取出模板。下方聚魔盆也能吸取储存法力。");
    }
}
