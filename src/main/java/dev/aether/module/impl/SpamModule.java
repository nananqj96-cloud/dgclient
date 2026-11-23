package dev.aether.module.impl;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import org.apache.commons.lang3.RandomStringUtils;

public class SpamModule extends Module {
    public static volatile SpamModule active;
    private static final int MAX_MESSAGE_LENGTH = 256;
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", 20.0, 0.0, 1200.0, 1.0F));
    private final BooleanSetting disableOnLeave = this.addSetting(new BooleanSetting("Disable On Leave", true));
    private final BooleanSetting disableOnDisconnect = this.addSetting(new BooleanSetting("Disable On Disconnect", true));
    private final BooleanSetting random = this.addSetting(new BooleanSetting("Randomise", false));
    private final BooleanSetting autoSplitMessages = this.addSetting(new BooleanSetting("Auto Split Messages", false));
    private final SliderSetting splitLength = this.addSetting(new SliderSetting("Split Length", 256.0, 1.0, 256.0, 1.0F));
    private final SliderSetting autoSplitDelay = this.addSetting(new SliderSetting("Split Delay", 20.0, 0.0, 200.0, 1.0F));
    private final BooleanSetting bypass = this.addSetting(new BooleanSetting("Bypass", false));
    private final BooleanSetting uppercase = this.addSetting(new BooleanSetting("Include Uppercase Characters", true));
    private final SliderSetting length = this.addSetting(new SliderSetting("Length", 16.0, 1.0, 256.0, 1.0F));
    private final Random rng = new Random();
    private int messageI;
    private int timer;
    private int splitNum;
    private String text;
    private boolean resetPending;

    public SpamModule() {
        super("spam", "Spams specified messages in chat.", Category.MISC);
        this.splitLength.visibleWhen(this.autoSplitMessages::get);
        this.autoSplitDelay.visibleWhen(this.autoSplitMessages::get);
        this.uppercase.visibleWhen(this.bypass::get);
        this.length.visibleWhen(this.bypass::get);
    }

    @Override
    public boolean inConfigs() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.resetPending = true;
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
    }

    public static void onGameLeft() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) {
            mc.execute(SpamModule::onGameLeft);
        } else {
            SpamModule spam = active;
            if (spam != null) {
                spam.handleGameLeft();
            }
        }
    }

    public static void onDisconnectedScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) {
            mc.execute(SpamModule::onDisconnectedScreen);
        } else {
            SpamModule spam = active;
            if (spam != null && spam.isEnabled() && spam.disableOnDisconnect.get()) {
                spam.requestDisable();
            }
        }
    }

    private void handleGameLeft() {
        if (this.isEnabled()) {
            if (this.disableOnLeave.get()) {
                this.requestDisable();
            } else {
                this.resetPending = true;
            }
        }
    }

    private void onPostTick(TickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && mc.level != null) {
            if (this.resetPending) {
                this.resetPending = false;
                this.timer = this.delay.getInt();
                this.messageI = 0;
                this.splitNum = 0;
            }

            List<String> list = this.messages.get();
            if (!list.isEmpty()) {
                if (this.timer > 0) {
                    this.timer--;
                } else {
                    if (this.text == null) {
                        int i;
                        if (this.random.get()) {
                            i = this.rng.nextInt(list.size());
                        } else {
                            if (this.messageI >= list.size()) {
                                this.messageI = 0;
                            }

                            i = this.messageI++;
                        }

                        this.text = list.get(i);
                        if (this.bypass.get()) {
                            String suffix = RandomStringUtils.insecure().nextAlphabetic(this.length.getInt());
                            if (!this.uppercase.get()) {
                                suffix = suffix.toLowerCase(Locale.ROOT);
                            }

                            this.text = this.text + " " + suffix;
                        }
                    }

                    int chunk = this.splitLength.getInt();
                    if (this.autoSplitMessages.get() && this.text.length() > chunk) {
                        int splits = (this.text.length() + chunk - 1) / chunk;
                        if (this.splitNum >= splits) {
                            this.splitNum = 0;
                        }

                        int start = this.splitNum * chunk;
                        int end = Math.min(start + chunk, this.text.length());
                        sendPlayerMsg(mc, player.connection, this.text.substring(start, end));
                        this.splitNum = (this.splitNum + 1) % splits;
                        this.timer = this.autoSplitDelay.getInt();
                        if (this.splitNum == 0) {
                            this.timer = this.delay.getInt();
                            this.text = null;
                        }
                    } else {
                        if (this.text.length() > 256) {
                            this.text = this.text.substring(0, 256);
                        }

                        sendPlayerMsg(mc, player.connection, this.text);
                        this.timer = this.delay.getInt();
                        this.text = null;
                    }
                }
            }
        }
    }

    private static void sendPlayerMsg(Minecraft mc, ClientPacketListener connection, String message) {
        mc.gui.getChat().addRecentChat(message);
        if (message.startsWith("/")) {
            connection.sendCommand(message.substring(1));
        } else {
            connection.sendChat(message);
        }
    }
}
