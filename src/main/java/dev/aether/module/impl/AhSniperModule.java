package dev.aether.module.impl;

import dev.aether.AetherClient;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AhSniperModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
    private static final String EXACT = "Exactly these";
    private static final String AT_LEAST = "At least these";
    private final ModeSetting enchantMatch = this.addSetting(
        "enchant-match", new ModeSetting("Enchant Match", "Exactly these", "Exactly these", "At least these")
    );
    private final SliderSetting minHours = this.addSetting(new SliderSetting("Min Time Left (h)", 0.0, 0.0, 48.0, 0.5F));
    private final SliderSetting refresh = this.addSetting(new SliderSetting("Refresh Every (ms)", 300.0, 100.0, 2000.0, 50.0F));
    private final SliderSetting settle = this.addSetting(new SliderSetting("Wait After Page Change (ms)", 200.0, 50.0, 1000.0, 25.0F));
    private final BooleanSetting requireConfirm = this.addSetting(new BooleanSetting("Only Buy With Confirm", false));
    private final SliderSetting maxBuys = this.addSetting(new SliderSetting("Stop After (buys, 0 = never)", 0.0, 0.0, 100.0, 1.0F));
    private final BooleanSetting autoSell = this.addSetting(new BooleanSetting("Auto Sell", false));
    private final MacroWorker worker = new MacroWorker("Anubis AH Sniper");
    private volatile SniperBot bot;

    public AhSniperModule() {
        super("ah-sniper", "Refreshes the AH and instantly buys your item below your price, with optional auto selling.", Category.DONUT);
        this.usesNotifications();
        this.enchantMatch.visibleWhen(() -> !this.enchants.get().isBlank());
        this.sellPrice.visibleWhen(this.autoSell::get);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null && mc.getUser() != null) {
            long max = ServerText.parseMoney(this.maxPrice.get());
            if (max <= 0L) {
//                 this.say(ModuleChat.body().warn("Set Buy Below to a price like 50m."));
                this.requestDisable();
            } else {
                SnipeTarget target = SnipeTarget.parse(
                    this.item.get(), this.enchants.get(), this.enchantMatch.is("Exactly these"), max, Math.round(this.minHours.get() * 60.0F)
                );
                if (target == null) {
//                     this.say(ModuleChat.body().warn("Pick an item to snipe first."));
                    this.requestDisable();
                } else {
                    long spendLimit = 0L;
                    if (!this.budget.get().isBlank()) {
                        spendLimit = ServerText.parseMoney(this.budget.get());
                        if (spendLimit <= 0L) {
//                             this.say(ModuleChat.body().warn("Set Max Total Spend to a price like 500m, or leave it empty."));
                            this.requestDisable();
                            return;
                        }
                    }

                    long sell = 0L;
                    if (this.autoSell.get()) {
                        sell = ServerText.parseMoney(this.sellPrice.get());
                        if (sell <= 0L) {
//                             this.say(ModuleChat.body().warn("Set Sell Price to a price like 60m, or turn Auto Sell off."));
                            this.requestDisable();
                            return;
                        }

                        if (sell <= max) {
                            this.say(
//                                 ModuleChat.body().warn("Sell Price must be higher than Buy Below").text(", or the sniper would buy your own listing back.")
                            );
                            this.requestDisable();
                            return;
                        }
                    }

                    SniperBot created = new SniperBot(
                        this,
                        this::stopFromBot,
                        this.worker,
                        target,
                        this.refresh.getInt(),
                        this.settle.getInt(),
                        spendLimit,
                        this.requireConfirm.get(),
                        this.maxBuys.getInt(),
                        sell,
                        mc.getUser().getName()
                    );
                    this.bot = created;
                    this.worker.start(created::run, failure -> {
                        LOGGER.error("[Anubis] AH Sniper stopped", failure);
//                         this.say(ModuleChat.body().danger("Stopped").text(": " + failure));
                        this.stopFromBot(created);
                    });
                }
            }
        } else {
//             this.say(ModuleChat.body().warn("Join a server first."));
            this.requestDisable();
        }
    }

    void stopFromBot(SniperBot from) {
        Minecraft.getInstance().execute(() -> {
            if (this.bot == from && this.isEnabled()) {
                this.setEnabled(false);
            }
        });
    }

    @Override
    protected void onDisable() {
        SniperBot stopped = this.bot;
        this.bot = null;
        this.worker.stop();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (stopped != null && player != null) {
            if (player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            } else {
                Dialogs.close(mc);
            }

//             this.say(ModuleChat.body().text(stopped.summary()));
        }
    }

//     @EventHandler
//     public void onPacket(PacketReceiveNettyEvent event) {
        SniperBot current = this.bot;
        if (current != null && event.packet instanceof ClientboundSystemChatPacket chat && chat.content() != null) {
            current.onChat(chat.content().getString());
        }
    }

//     private void say(ModuleChat.Body body) {
        AetherClient.chat(this, body);
    }
}
