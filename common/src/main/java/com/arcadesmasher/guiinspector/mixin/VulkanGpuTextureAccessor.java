package com.arcadesmasher.guiinspector.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
//#if _26_2
//$$	import com.mojang.blaze3d.vulkan.*;
//#endif

// can't use AW
@Mixin(targets = "com.mojang.blaze3d.vulkan.VulkanGpuTexture")
public interface VulkanGpuTextureAccessor {

	//#if _26_2
//$$	@Accessor("device")
//$$	VulkanDevice getDevice();
	//#endif
}
