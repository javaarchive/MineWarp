package io.javaarchive.minewarp.mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Mouse.class, priority = 5)
public interface MouseAccessor {
    @Accessor("cursorLocked")
    void setCursorLocked(boolean cursorLocked);
}