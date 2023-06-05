package dev.agnor.functionalui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2i;

import java.util.*;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class    ShulkerChanger {

    private static final Map<Slot, SlotFlyData> flyData = new HashMap<>();
    private static final Map<Slot, SlotTargetData> targetData = new HashMap<>();
    private static final ResourceLocation additionalAssets = new ResourceLocation(FunctionalUI.MODID, "textures/gui/additionalassets.png");

    @SubscribeEvent
    public static void onScreenRendered(ContainerScreenEvent.Render.Background backgroundRendered) {
        if (backgroundRendered.getContainerScreen() instanceof ShulkerBoxScreen screen) {
            PoseStack poseStack = backgroundRendered.getPoseStack();
            RenderSystem.setShaderTexture(0, additionalAssets);
            float partialTicks = Minecraft.getInstance().getPartialTick();
            Optional.ofNullable(Minecraft.getInstance().level).map(Level::getGameTime).ifPresent(gameTime -> {
                for (var entry : flyData.entrySet()) {
                    SlotFlyData slotFlyData = entry.getValue();
                    Vector2i vector2i = slotFlyData.originalPos();
                    Screen.blit(poseStack, screen.getGuiLeft() + vector2i.x() - 1, screen.getGuiTop() + vector2i.y() - 1, 82, 0, 18, 18);
                }
                for (var entry : flyData.entrySet()) {
                    SlotFlyData slotFlyData = entry.getValue();
                    Slot target = entry.getKey();
                    Vector2i vector2i = slotFlyData.originalPos();
                    int timeDiff = (int)(gameTime - slotFlyData.gametime());
                    int heightDiff = -timeDiff;
                    if (timeDiff > 100) {
                        timeDiff = timeDiff - 100;
                        heightDiff = (int) (0.5f*0.125f * timeDiff * timeDiff) - 100;
                    }
                    target.y = vector2i.y + heightDiff;
                    Screen.blit(poseStack, screen.getGuiLeft() + vector2i.x() - 1, screen.getGuiTop() + vector2i.y() + heightDiff - 1, 112, 0, 18, 18);
                }
            });
            for (boolean isLeft: new Boolean[]{true, false}) {
                int x = isLeft ? 0 : screen.width - 16;
                int y = (screen.height - 24) / 2;
                Screen.blit(poseStack, x , y, 130, 0, 16, 24);
            }
            for (SlotTargetData value : targetData.values()) {
                int x = (int)Mth.lerp(partialTicks, value.lastPos.x, value.currentPos.x);
                int y = (int)Mth.lerp(partialTicks, value.lastPos.y, value.currentPos.y);
                Screen.blit(poseStack, x , y, 146, 0, 8, 8);
                for (int i = 0; i < value.health; i++) {
                    Screen.blit(poseStack, x - 8 + i*8, y - 10, 154, 0, 7, 7);
                }
            }
        }
    }

    private static final Random random = new Random();
    private static boolean lastSpawnSideLeft = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && Minecraft.getInstance().screen instanceof ShulkerBoxScreen screen) {
            Optional.ofNullable(Minecraft.getInstance().level).map(Level::getGameTime).ifPresent(gameTime -> {
                List<Slot> toRemove = new ArrayList<>();
                for (var slotFlyData : flyData.entrySet()) {
                    if (gameTime - slotFlyData.getValue().gametime() >= 140) {
                        slotFlyData.getKey().y = slotFlyData.getValue().originalPos.y;
                        toRemove.add(slotFlyData.getKey());
                    }
                }
                toRemove.forEach(flyData::remove);
                if (gameTime % 20 == 0) {
                    List<Slot> availableSlots = screen.getMenu().slots.stream().filter(slot -> slot instanceof ShulkerBoxSlot).filter(slot -> !flyData.containsKey(slot)).filter(slot -> !targetData.containsKey(slot)).toList();
                    if (availableSlots.isEmpty())
                        return;
                    List<Slot> availableSlotsWithItems = availableSlots.stream().filter(slot -> !slot.getItem().isEmpty()).toList();
                    Slot slot;
                    if (random.nextBoolean() && !availableSlotsWithItems.isEmpty()) {
                        slot = availableSlotsWithItems.get(random.nextInt(availableSlotsWithItems.size()));
                    } else {
                        slot = availableSlots.get(random.nextInt(availableSlots.size()));
                    }
                    Minecraft.getInstance().level.playSound(Minecraft.getInstance().player, Minecraft.getInstance().player, SoundEvents.SHULKER_SHOOT, SoundSource.BLOCKS, 1, 1);
                    boolean startsLeft = (random.nextFloat() > 0.3f) != lastSpawnSideLeft;
                    lastSpawnSideLeft = startsLeft;
                    int x = startsLeft ? 8 : screen.width - 8;
                    int y = (screen.height - 12) / 2;
                    targetData.put(slot, new SlotTargetData(new Vector2i(x, y), SlotTargetData.FlyingDirection.getStartDirection(startsLeft)));
                }
                List<Slot> startFlying = new ArrayList<>();
                for (var entry : targetData.entrySet()) {
                    Slot slot = entry.getKey();
                    SlotTargetData target = entry.getValue();
                    for (int i = 0; i < 4; i++) {
                        target.lastPos = new Vector2i(target.currentPos);
                        if ((target.flyingsince > 40 && random.nextFloat() > 0.995 && !target.hasReached(slot, !target.flyingDirection.isUpDownAxis())) || target.hasReached(slot, target.flyingDirection.isUpDownAxis())) {
                            target.changeDirection(slot);
                        }
                        target.move();
                        if (target.hasReached(slot)) {
                            startFlying.add(slot);
                            break;
                        }
                    }
                }
                for (Slot slot : startFlying) {
                    targetData.remove(slot);
                    flyData.put(slot, new SlotFlyData(new Vector2i(slot.x, slot.y), gameTime));
                }
            });
        }
    }


    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing closing) {
        if (closing.getScreen() instanceof ShulkerBoxScreen) {
            flyData.clear();
            targetData.clear();
        }
    }
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Pre init) {
        if (init.getScreen() instanceof ShulkerBoxScreen) {
            flyData.forEach((key, value) -> {
                key.x = value.originalPos.x;
                key.y = value.originalPos.y;
            });
            flyData.clear();
            targetData.clear();
        }
    }

    @SubscribeEvent
    public static void onScreenClick(ScreenEvent.MouseButtonPressed.Pre mousePress) {
        if (mousePress.getScreen() instanceof ShulkerBoxScreen) {
            List<Slot> toRemove = new ArrayList<>();
            for (var entry : targetData.entrySet()) {
                SlotTargetData target = entry.getValue();
                if (target.currentPos.x -6 < mousePress.getMouseX() && target.currentPos.x +6 +8 > mousePress.getMouseX()
                        && target.currentPos.y -6 < mousePress.getMouseY() && target.currentPos.y +6 +8 > mousePress.getMouseY()) {
                    target.health--;
                    if (target.health <= 0) {
                        toRemove.add(entry.getKey());
                    }
                }
            }
            toRemove.forEach(targetData::remove);
        }
    }

    private record SlotFlyData(Vector2i originalPos, long gametime) {
    }
    private static final class SlotTargetData {
        private final Vector2i currentPos;
        private Vector2i lastPos;
        private FlyingDirection flyingDirection;
        private int flyingsince = 0;

        private int health = 3;

        private SlotTargetData(Vector2i pos, FlyingDirection flyingDirection) {
            currentPos = pos;
            lastPos = pos;
            this.flyingDirection = flyingDirection;
        }

        private boolean hasReached(Slot target) {
            return hasReached(target, true) && hasReached(target, false);
        }

        private boolean hasReached(Slot target, boolean upDownAxis) {
            return distance(target, upDownAxis) == 0;
        }
        private int distance(Slot target, boolean upDownAxis) {
            if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) {
                if (upDownAxis) {
                    return screen.getGuiTop() + target.y - currentPos.y + 8;
                } else {
                    return screen.getGuiLeft() + target.x - currentPos.x + 8;
                }
            }
            return 0;
        }


        private void move() {
            switch (flyingDirection) {
                case LEFT -> currentPos.add(-1,0);
                case RIGHT -> currentPos.add(1,0);
                case UP -> currentPos.add(0, -1);
                case DOWN -> currentPos.add(0, 1);
            }
            flyingsince++;
        }

        private void changeDirection(Slot target) {
            boolean targetAxisUpDown = !flyingDirection.isUpDownAxis();
            int distance = distance(target, targetAxisUpDown);
            flyingsince = 0;
            if (distance == 0) {
                flyingDirection = FlyingDirection.values()[random.nextInt(FlyingDirection.values().length)];
                return;
            }
            if (targetAxisUpDown) {
                flyingDirection = distance > 0 ? FlyingDirection.DOWN : FlyingDirection.UP;
            } else {
                flyingDirection = distance > 0 ? FlyingDirection.RIGHT : FlyingDirection.LEFT;
            }
        }


        private enum FlyingDirection {
            LEFT,
            RIGHT,
            UP,
            DOWN;

            private static FlyingDirection getStartDirection(boolean isLeft) {
                return isLeft ? RIGHT : LEFT;
            }

            boolean isUpDownAxis() {
                return this == UP || this == DOWN;
            }
        }
    }
}
