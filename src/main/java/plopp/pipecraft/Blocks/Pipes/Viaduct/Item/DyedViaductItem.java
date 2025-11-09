package plopp.pipecraft.Blocks.Pipes.Viaduct.Item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaduct;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductDetector;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductLinker;
import plopp.pipecraft.Blocks.Pipes.Viaduct.BlockViaductSpeed;

public class DyedViaductItem extends BlockItem {

    private final String tooltipKeyPrefix;

    public DyedViaductItem(Block block, String tooltipKeyPrefix, Properties props) {
        super(block, props);
        this.tooltipKeyPrefix = tooltipKeyPrefix;
    }

    public static void setColor(ItemStack stack, DyeColor color) {
        stack.set(ModDataComponents.COLOR, color);
    }

    @Nullable
    public static DyeColor getColor(ItemStack stack) {
        return stack.get(ModDataComponents.COLOR);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DyeColor color = getColor(stack);
        if (color != null) {
            tooltip.add(Component.literal("Color: " + color.getName())
                .withStyle(style -> style.withColor(color.getTextColor())));
        }

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip." + tooltipKeyPrefix + ".desc").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip." + tooltipKeyPrefix + ".usage").withStyle(ChatFormatting.WHITE));
        } else {
            tooltip.add(Component.translatable("tooltip.hold_shift").withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        DyeColor color = getColor(stack);
        if (color != null) {
            if (state.hasProperty(BlockViaduct.COLOR)) {
                level.setBlock(pos, state.setValue(BlockViaduct.COLOR, color), 3);
            } else if (state.hasProperty(BlockViaductLinker.COLOR)) {
                level.setBlock(pos, state.setValue(BlockViaductLinker.COLOR, color), 3);
            } else if (state.hasProperty(BlockViaductSpeed.COLOR)) {
                level.setBlock(pos, state.setValue(BlockViaductSpeed.COLOR, color), 3);
            } else if (state.hasProperty(BlockViaductDetector.COLOR)) {
                level.setBlock(pos, state.setValue(BlockViaductDetector.COLOR, color), 3);
            }
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }
}