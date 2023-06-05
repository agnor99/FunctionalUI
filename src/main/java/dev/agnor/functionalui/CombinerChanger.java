package dev.agnor.functionalui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector2i;

import java.util.Optional;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class CombinerChanger {
    private static int progress = 0;

    private static final ResourceLocation additionalAssets = new ResourceLocation(FunctionalUI.MODID, "textures/gui/additionalassets.png");


    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post postInit) {
        if (postInit.getScreen() instanceof LegacySmithingScreen screen) {
            screen.addRenderableWidget(new CombinerWidget(screen.getGuiLeft() + 17,screen.getGuiTop() + 7, false));
            removeResultSlot(screen);
        }
        if (postInit.getScreen() instanceof AnvilScreen screen) {
            screen.addRenderableWidget(new CombinerWidget(screen.getGuiLeft() + 17,screen.getGuiTop() + 7, true));
            removeResultSlot(screen);
        }
        if (postInit.getScreen() instanceof SmithingScreen screen) {
            screen.addRenderableWidget(new CombinerWidget(screen.getGuiLeft() + 7,screen.getGuiTop() + 7, false));
            removeResultSlot(screen);
        }
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing closing) {
        if (closing.getScreen() instanceof ItemCombinerScreen<?>)
            progress = 0;
    }

    private static void removeResultSlot(ItemCombinerScreen<?> screen) {
        findOutput(screen).ifPresent(slot -> slot.x = Integer.MIN_VALUE);
    }

    private static boolean isOutputPresent(ItemCombinerScreen<?> screen) {
        return findOutput(screen).map(slot -> !slot.getItem().isEmpty()).orElse(false);
    }

    private static Optional<Slot> findOutput(ItemCombinerScreen<?> screen) {
        return screen.getMenu().slots.stream().filter(slot -> slot.container == screen.getMenu().resultSlots).findFirst();
    }

    @SubscribeEvent
    public static void onScreenDragged(ScreenEvent.MouseDragged.Pre dragged) {
        if (dragged.getScreen() instanceof AbstractContainerScreen<?> screen && (screen.getFocused() != null && screen.isDragging() && dragged.getMouseButton() == 0 && screen.getFocused() instanceof DraggableMarker)) {
            screen.getFocused().mouseDragged(dragged.getMouseX(), dragged.getMouseY(), dragged.getMouseButton(), dragged.getDragX(), dragged.getDragY());
        }
    }

    @SubscribeEvent
    public static void onScreenRendered(ContainerScreenEvent.Render.Background backgroundRendered) {
        if (backgroundRendered.getContainerScreen() instanceof ItemCombinerScreen<?> screen) {
            PoseStack poseStack = backgroundRendered.getPoseStack();
            RenderSystem.setShaderTexture(0, additionalAssets);
            if (screen instanceof LegacySmithingScreen legacySmithingScreen) {
                screen.blit(poseStack, legacySmithingScreen.getGuiLeft()+17, legacySmithingScreen.getGuiTop()+7, 82, 0, 30, 30);
            }
            if (screen instanceof AnvilScreen anvilScreen) {
                    screen.blit(poseStack, anvilScreen.getGuiLeft()+17, anvilScreen.getGuiTop()+7, 82, 0, 30, 30);
            }
            if (screen instanceof SmithingScreen smithingScreen) {
                screen.blit(poseStack, smithingScreen.getGuiLeft()+7, smithingScreen.getGuiTop()+7, 82, 0, 30, 31);
            }
            if (progress > 0) {
                if (screen instanceof LegacySmithingScreen legacySmithingScreen) {
                    screen.blit(poseStack, legacySmithingScreen.getGuiLeft() + 102, legacySmithingScreen.getGuiTop() + 48, 0, 0, (int)(22/5d*progress), 15);
                }
                if (screen instanceof AnvilScreen anvilScreen) {
                    screen.blit(poseStack, anvilScreen.getGuiLeft() + 102, anvilScreen.getGuiTop() + 48, 0, 0, (int)(22/5d*progress), 15);
                }
                if (screen instanceof SmithingScreen smithingScreen) {
                    screen.blit(poseStack, smithingScreen.getGuiLeft() + 68, smithingScreen.getGuiTop() + 49, 0, 0, (int)(22/5d*progress), 15);
                }
            }
        }
    }

    private static class CombinerWidget extends AbstractWidget implements DraggableMarker{

        @Nullable
        private Vector2d grabbedPosition;
        private final Vector2i defaultPosition;

        private boolean hasEnoughHeight = false;
        private final boolean isAnvil;
        public CombinerWidget(int x, int y, boolean isAnvil) {
            super(x, y, 30, 30, Component.empty());
            defaultPosition = new Vector2i(x,y);
            this.isAnvil = isAnvil;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            super.onClick(mouseX, mouseY);
            if (!(Minecraft.getInstance().screen instanceof ItemCombinerScreen<?> screen))
                return;
            if (isOutputPresent(screen))
                grabbedPosition = new Vector2d(mouseX, mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            super.onDrag(mouseX, mouseY, dragX, dragY);
            if (!(Minecraft.getInstance().screen instanceof ItemCombinerScreen<?> screen))
                return;
            double y = Optional.ofNullable(grabbedPosition).map(vec -> new Vector2d(vec).sub(mouseX, mouseY).mul(-1).y).orElse(10d);
            if (y < 0) {
                hasEnoughHeight = true;
            }
            if (y > 48 && hasEnoughHeight && progress < 5) {
                hasEnoughHeight = false;
                Minecraft.getInstance().level.playSound(Minecraft.getInstance().player, Minecraft.getInstance().player, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1, 1);
                progress++;
                if (progress >= 5) {
                    findOutput(screen).ifPresent(slot -> {
                        ItemCombinerMenuSlotDefinition.SlotDefinition resultSlot = screen.getMenu().createInputSlotDefinitions().getResultSlot();
                        slot.x = resultSlot.x();
                        slot.y = resultSlot.y();
                    });
                }
            }
        }

        @Override
        public void onRelease(double pMouseX, double pMouseY) {
            super.onRelease(pMouseX, pMouseY);
            setX(defaultPosition.x);
            setY(defaultPosition.y);
            grabbedPosition = null;
        }

        @Override
        public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
            Vector2i pos = Optional.ofNullable(grabbedPosition).map(vec -> new Vector2i(mouseX, mouseY).sub((int) vec.x, (int) vec.y).add(defaultPosition)).orElse(defaultPosition);
            RenderSystem.setShaderTexture(0, additionalAssets);
            Screen.blit(poseStack, pos.x, pos.y, isAnvil ? 52 : 22, 0, 30, 30);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {

        }

        @Override
        public void playDownSound(SoundManager pHandler) {
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            Vector2d pos = Optional.ofNullable(grabbedPosition).map(vec -> new Vector2d(mouseX, mouseY).sub(vec.x,vec.y).add(defaultPosition.x, defaultPosition.y)).orElse(new Vector2d(defaultPosition.x, defaultPosition.y));
            return this.active && this.visible && mouseX >= pos.x && mouseY >= pos.y && mouseX < (pos.x + this.width) && mouseY < (pos.y + this.height);
        }
    }
}
