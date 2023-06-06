package dev.agnor.functionalui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class GrindstoneChanger {
    private static int progress = 0;
    private static int rotationProgress = 0;
    private static List<ParticleData> particles = new ArrayList<>();

    private static final ResourceLocation additionalAssets = new ResourceLocation(FunctionalUI.MODID, "textures/gui/additionalassets.png");


    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing closing) {
        if (closing.getScreen() instanceof GrindstoneScreen) {
            progress = 0;
            rotationProgress = 0;
            particles.clear();
            hasPulledOut = false;
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post init) {
        if (init.getScreen() instanceof GrindstoneScreen screen) {
            removeSlots(screen);
            particles.clear();
            hasPulledOut = false;
        }
    }

    private static int mouseX = 0;
    private static int mouseY = 0;
    private static boolean hasPulledOut = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START  && Minecraft.getInstance().screen instanceof GrindstoneScreen screen) {
            rotationProgress = (rotationProgress + 10)%360;
            if (screen.getGuiLeft() + 42 - 6 < mouseX && screen.getGuiLeft() + 6 + 42 + 24> mouseX
                    && screen.getGuiTop() + 23 - 6 < mouseY && screen.getGuiTop() + 6 + 23 + 24 > mouseY) {
                ItemStack carried = screen.getMenu().getCarried();
                findFirstInput(screen).ifPresent(input -> {
                    if (input.getItem().isEmpty() && !hasPulledOut)
                        clickInput(screen);
                });
                if (!findOutput(screen).map(Slot::getItem).orElse(ItemStack.EMPTY).isEmpty()) {
                    progress++;
                    Minecraft.getInstance().level.playSound(Minecraft.getInstance().player, Minecraft.getInstance().player, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1, 1);
                    if (random.nextFloat() < 0.7) {
                        particles.add(new ParticleData(new Vector2f(mouseX, mouseY)));
                        if (random.nextFloat() < 0.3)
                            particles.add(new ParticleData(new Vector2f(mouseX, mouseY)));
                    }
                    if (progress > 100 && carried.isEmpty()) {
                        clickOutput(screen);
                        if (findOutput(screen).map(Slot::getItem).orElse(ItemStack.EMPTY).isEmpty()) {
                            progress = 0;
                            hasPulledOut = true;
                        }
                    }
                }
            } else {
                if (!findFirstInput(screen).map(Slot::getItem).orElse(ItemStack.EMPTY).isEmpty())
                    clickInput(screen);
                hasPulledOut = false;
            }
            particles.removeIf(ParticleData::tick);
        }

    }

    private static void clickInput(GrindstoneScreen screen) {
        ItemStack carried = screen.getMenu().getCarried();
        findFirstInput(screen).ifPresent(input -> {
            if (input.getItem().isEmpty() ^ carried.isEmpty()) {
                ClickType clicktype = ClickType.PICKUP;
                screen.slotClicked(input, 0, 0, clicktype);
            }
        });
    }
    private static void clickOutput(GrindstoneScreen screen) {
        ItemStack carried = screen.getMenu().getCarried();
        findOutput(screen).ifPresent(output -> {
            if (output.getItem().isEmpty() ^ carried.isEmpty()) {
                ClickType clicktype = ClickType.PICKUP;
                screen.slotClicked(output, 0, 0, clicktype);
            }
        });
    }

    private static void removeSlots(GrindstoneScreen screen) {
        findOutput(screen).ifPresent(slot -> {slot.x = -2000; slot.y = 0;});
        findFirstInput(screen).ifPresent(slot -> {slot.x = -2000; slot.y = 500;});
        findSecondInput(screen).ifPresent(slot -> slot.x = Integer.MIN_VALUE);
    }

    private static Optional<Slot> findOutput(GrindstoneScreen screen) {
        return screen.getMenu().slots.stream().filter(slot -> slot.container == screen.getMenu().resultSlots).findFirst();
    }
    private static Optional<Slot> findFirstInput(GrindstoneScreen screen) {
        return screen.getMenu().slots.stream().filter(slot -> slot.container == screen.getMenu().repairSlots).findFirst();
    }
    private static Optional<Slot> findSecondInput(GrindstoneScreen screen) {
        return screen.getMenu().slots.stream().filter(slot -> slot.container == screen.getMenu().repairSlots).filter(slot -> slot.getSlotIndex() == 1).findFirst();
    }

    private static int offset = 6;
    @SubscribeEvent
    public static void onScreenRendered(ContainerScreenEvent.Render.Background backgroundRendered) {
        if (backgroundRendered.getContainerScreen() instanceof GrindstoneScreen screen) {
            mouseX = backgroundRendered.getMouseX();
            mouseY = backgroundRendered.getMouseY();
            PoseStack poseStack = backgroundRendered.getPoseStack();
            RenderSystem.setShaderTexture(0, additionalAssets);
            for (int x = 30; x < 84; x+=30) {
                for (int y = 15; y < 71; y+=30) {
                    GuiComponent.blit(poseStack, screen.getGuiLeft()+x, screen.getGuiTop()+y, 82, 0, 30, 30);
                }
            }
            GuiComponent.blit(poseStack, screen.getGuiLeft()+128, screen.getGuiTop()+33, 82, 0, 18, 18);
            poseStack.pushPose();
            poseStack.translate(screen.getGuiLeft() + 48 + offset, screen.getGuiTop() + 29 + offset, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.lerp(Minecraft.getInstance().getPartialTick(), rotationProgress + 350, rotationProgress + 360)));
            poseStack.translate(-(screen.getGuiLeft() + 48 + offset), -(screen.getGuiTop() + 29 + offset), 0);
            GuiComponent.blit(poseStack, screen.getGuiLeft()+42, screen.getGuiTop()+23, 173, 0, 24, 24);
            poseStack.popPose();
            GuiComponent.blit(poseStack, screen.getGuiLeft()+48, screen.getGuiTop()+29, 161, 0, 12, 26);
            GuiComponent.blit(poseStack, screen.getGuiLeft() + 95, screen.getGuiTop() + 34, 0, 0, (int)(Math.min(100, progress) / 100f * 22), 15);
        }
    }

    @SubscribeEvent
    public static void postScreenRendered(ScreenEvent.Render.Post postRender) {
        if (postRender.getScreen() instanceof GrindstoneScreen screen) {
            PoseStack poseStack = postRender.getPoseStack();
            findFirstInput(screen).map(Slot::getItem).ifPresent(stack -> {
                if (!stack.isEmpty()) {
                    screen.renderFloatingItem(poseStack, stack, postRender.getMouseX() - 8, postRender.getMouseY() - 8, null);

                    ClientLevel level = Minecraft.getInstance().level;
                    BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, 0);
                    TextureAtlasSprite particleIcon = model.getOverrides().resolve(model, stack, level, null, 0).getParticleIcon(ModelData.EMPTY);
                    ResourceLocation name = particleIcon.contents().name();
                    if (!name.getPath().equals("missingno") || !name.getNamespace().equals("minecraft")) {
                        name = new ResourceLocation(name.getNamespace(), "textures/" + name.getPath() + ".png");
                        RenderSystem.setShaderTexture(0, name);
                        for (ParticleData data : particles) {
                            GuiComponent.blit(poseStack, (int) data.pos.x, (int) data.pos.y, particleIcon.contents().width() / 4f * data.uvpart.x, particleIcon.contents().height() / 4f * data.uvpart.y, particleIcon.contents().width() / 4, particleIcon.contents().height() / 4, particleIcon.contents().width(), particleIcon.contents().height());
                        }
                    }
                }
            });
        }
    }

    private static Random random = new Random();

    public static class ParticleData {
        private final Vector2f pos;
        private final Vector2f vel;
        private final Vector2i uvpart;
        private int lifeTime;

        public ParticleData(Vector2f pos) {
            this.pos = pos.add(random.nextInt(15) - 7,random.nextInt(15) - 7);
            this.lifeTime = random.nextInt(15)+10;
            vel = new Vector2f(random.nextInt(5) - 2, random.nextInt(4));
            uvpart = new Vector2i(random.nextInt(4), random.nextInt(4));
        }

        public boolean tick() {
            vel.add(0, 0.4f);
            lifeTime--;
            pos.add(vel);
            return lifeTime < 0;
        }
    }

}
