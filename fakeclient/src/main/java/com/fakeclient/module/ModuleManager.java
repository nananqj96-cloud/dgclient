/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  com.fakeclient.module.Module
 *  com.fakeclient.module.modules.FakeItemCount
 *  com.fakeclient.module.modules.FakeMedia
 *  com.fakeclient.module.modules.FakePay
 *  com.fakeclient.module.modules.FakeScoreboard
 *  com.fakeclient.module.modules.FakeSpawner
 *  com.fakeclient.module.modules.PaySimulator
 *  com.fakeclient.module.modules.TextureChanger
 */
package com.fakeclient.module;

import com.fakeclient.Settings;
import com.fakeclient.module.Module;
import com.fakeclient.module.modules.FakeItemCount;
import com.fakeclient.module.modules.FakeMedia;
import com.fakeclient.module.modules.FakePay;
import com.fakeclient.module.modules.FakeScoreboard;
import com.fakeclient.module.modules.FakeSpawner;
import com.fakeclient.module.modules.PaySimulator;
import com.fakeclient.module.modules.TextureChanger;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<Module>();
    public final FakePay fakePay = this.register(new FakePay(), "Payments");
    public final PaySimulator paySimulator = this.register(new PaySimulator(), "Payments");
    public final FakeScoreboard fakeScoreboard = this.register(new FakeScoreboard(), "Visuals");
    public final FakeItemCount fakeItemCount = this.register(new FakeItemCount(), "Visuals");
    public final FakeSpawner fakeSpawner = this.register(new FakeSpawner(), "Visuals");
    public final TextureChanger textureChanger = this.register(new TextureChanger(), "Visuals");
    public final FakeMedia fakeMedia = this.register(new FakeMedia(), "Media");

    public <T extends Module> T register(T module) {
        this.modules.add(module);
        this.applyState(module);
        return module;
    }

    public <T extends Module> T register(T module, String category) {
        module.setCategory(category);
        return this.register(module);
    }

    public List<Module> getModules() {
        return this.modules;
    }

    public <T extends Module> T get(Class<T> clazz) {
        for (Module module : this.modules) {
            if (!clazz.isInstance(module)) continue;
            return (T)((Module)clazz.cast(module));
        }
        return null;
    }

    public void tickEnabled() {
        for (Module m : this.modules) {
            if (!m.isEnabled()) continue;
            m.tick();
        }
    }

    public void loadState() {
        for (Module m : this.modules) {
            this.applyState(m);
        }
    }

    private void applyState(Module m) {
        Boolean on;
        Settings s = Settings.get();
        Integer k = (Integer)s.binds.get(m.getName());
        if (k != null) {
            m.setKey(k.intValue());
        }
        if ((on = (Boolean)s.moduleStates.get(m.getName())) != null) {
            m.setEnabled(on.booleanValue());
        }
    }

    public void saveState() {
        Settings s = Settings.get();
        for (Module m : this.modules) {
            s.binds.put(m.getName(), m.getKey());
            s.moduleStates.put(m.getName(), m.isEnabled());
        }
        s.save();
    }

    public void disableAll() {
        for (Module m : this.modules) {
            if (!m.isEnabled()) continue;
            m.setEnabled(false);
        }
    }
}
