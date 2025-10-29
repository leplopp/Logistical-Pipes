package plopp.pipecraft.model;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ViaductBlockBox {

	public static VoxelShape getShape(BlockState state, BooleanProperty northProp, BooleanProperty southProp,
			BooleanProperty eastProp, BooleanProperty westProp, BooleanProperty upProp, BooleanProperty downProp) {

		boolean north = state.getValue(northProp);
		boolean south = state.getValue(southProp);
		boolean east = state.getValue(eastProp);
		boolean west = state.getValue(westProp);
		boolean up = state.getValue(upProp);
		boolean down = state.getValue(downProp);

		VoxelShape center = box(4, 4, 4, 12, 12, 12);
		VoxelShape northArm = box(4, 4, 0, 12, 12, 4);
		VoxelShape southArm = box(4, 4, 12, 12, 12, 16);
		VoxelShape westArm = box(0, 4, 4, 4, 12, 12);
		VoxelShape eastArm = box(12, 4, 4, 16, 12, 12);
		VoxelShape upArm = box(4, 12, 4, 12, 16, 12);
		VoxelShape downArm = box(4, 0, 4, 12, 4, 12);

		VoxelShape defaultShape = Shapes.or(center, northArm, southArm, westArm, eastArm, upArm, downArm);

		boolean noConnections = !(north || south || east || west || up || down);

		boolean ConnectionCross = (north && south && east && west && up && down);

		boolean ConnectionsUpDown = (up && down) && !(north || south || east || west);
		boolean ConnectionsWestEast = (east && west) && !(north || south || up || down);
		boolean ConnectionsSouthNorth = (north && south) && !(east || west || up || down);

		boolean ConnectionsUpWest = (west && up) && !(east || south || north || down);
		boolean ConnectionsUpEast = (east && up) && !(west || south || north || down);
		boolean ConnectionsUpNorth = (north && up) && !(east || south || west || down);
		boolean ConnectionsUpSouth = (south && up) && !(east || west || north || down);
		boolean ConnectionsDownWest = (west && down) && !(east || south || north || up);
		boolean ConnectionsDownEast = (east && down) && !(west || south || north || up);
		boolean ConnectionsDownNorth = (north && down) && !(east || south || west || up);
		boolean ConnectionsDownSouth = (south && down) && !(east || west || north || up);
		boolean ConnectionsNorthWest = (west && north) && !(east || south || down || up);
		boolean ConnectionsNorthEast = (east && north) && !(west || south || down || up);
		boolean ConnectionsSouthEast = (south && east) && !(down || north || west || up);
		boolean ConnectionsSouthWest = (south && west) && !(east || down || north || up);

		boolean onlyUp = down && !(north || south || east || west || up);
		boolean onlyDown = up && !(north || south || east || west || down);
		boolean onlyNorth = north && !(south || east || west || up || down);
		boolean onlySouth = south && !(north || east || west || up || down);
		boolean onlyEast = east && !(north || south || west || up || down);
		boolean onlyWest = west && !(north || south || east || up || down);

		if (noConnections) {
			return defaultShape;
		}

		if (ConnectionCross) {
			northArm = box(4, 0, 0, 12, 16, 16);
			southArm = box(0, 4, 0, 16, 12, 16);
			westArm = box(0, 0, 4, 16, 16, 12);

			return Shapes.or(northArm, southArm, westArm);
		}

		if (ConnectionsUpDown) {
			northArm = box(4, 0, 0, 12, 16, 16);
			westArm = box(0, 0, 4, 16, 16, 12);

			return Shapes.or(northArm, westArm);
		}

		if (ConnectionsWestEast) {
			northArm = box(0, 4, 0, 16, 12, 16);
			westArm = box(0, 0, 4, 16, 16, 12);

			return Shapes.or(northArm, westArm);
		}

		if (ConnectionsSouthNorth) {
			northArm = box(0, 4, 0, 16, 12, 16);
			westArm = box(4, 0, 0, 12, 16, 16);

			return Shapes.or(northArm, westArm);
		}

		if (onlyUp) {
			upArm = box(4, 12, 4, 12, 16, 12);
			northArm = box(4, 0, 0, 12, 12, 16);
			westArm = box(0, 0, 4, 16, 12, 12);

			return Shapes.or(northArm, westArm, upArm);
		}

		if (onlyDown) {
			downArm = box(4, 0, 4, 12, 4, 12);
			northArm = box(4, 4, 0, 12, 16, 16);
			westArm = box(0, 4, 4, 16, 16, 12);

			return Shapes.or(northArm, westArm, downArm);
		}

		if (onlyNorth) {

			northArm = box(0, 4, 0, 16, 12, 12);
			westArm = box(4, 0, 0, 12, 16, 12);
			southArm = box(4, 4, 12, 12, 12, 16);

			return Shapes.or(northArm, westArm, southArm);
		}

		if (onlySouth) {

			northArm = box(4, 4, 0, 12, 12, 4);
			westArm = box(4, 0, 4, 12, 16, 16);
			southArm = box(0, 4, 4, 16, 12, 16);

			return Shapes.or(northArm, westArm, southArm);
		}

		if (onlyEast) {

			northArm = box(4, 4, 0, 16, 12, 16);
			westArm = box(0, 4, 4, 4, 12, 12);
			southArm = box(4, 0, 4, 16, 16, 12);

			return Shapes.or(northArm, westArm, southArm);
		}

		if (onlyWest) {

			eastArm = box(12, 4, 4, 16, 12, 12);
			westArm = box(0, 4, 0, 12, 12, 16);
			southArm = box(0, 0, 4, 12, 16, 12);

			return Shapes.or(eastArm, westArm, southArm);
		}

		if (ConnectionsUpWest) {

			northArm = box(4, 4, 0, 12, 16, 16);
			eastArm = box(12, 4, 4, 16, 16, 12);
			westArm = box(0, 4, 0, 12, 12, 16);
			southArm = box(0, 0, 4, 12, 16, 12);

			return Shapes.or(eastArm, westArm, southArm, northArm);
		}

		if (ConnectionsUpEast) {

			northArm = box(4, 4, 0, 12, 16, 16);
			eastArm = box(4, 0, 4, 16, 16, 12);
			westArm = box(4, 4, 0, 16, 12, 16);
			southArm = box(0, 4, 4, 16, 16, 12);

			return Shapes.or(eastArm, westArm, southArm, northArm);
		}

		if (ConnectionsUpNorth) {

			northArm = box(4, 4, 0, 12, 16, 16);
			eastArm = box(4, 0, 0, 12, 4, 12);
			westArm = box(0, 4, 4, 16, 16, 12);
			southArm = box(0, 4, 0, 16, 12, 12);

			return Shapes.or(eastArm, westArm, southArm, northArm);
		}

		if (ConnectionsUpSouth) {

			northArm = box(4, 4, 0, 12, 16, 16);
			eastArm = box(4, 0, 4, 12, 4, 16);
			westArm = box(0, 4, 4, 16, 16, 12);
			southArm = box(0, 4, 4, 16, 12, 16);

			return Shapes.or(eastArm, westArm, southArm, northArm);
		}

		if (ConnectionsDownWest) {

			northArm = box(4, 0, 0, 12, 12, 16);
			eastArm = box(12, 0, 4, 16, 12, 12);
			westArm = box(0, 4, 0, 12, 12, 16);
			southArm = box(0, 0, 4, 12, 16, 12);

			return Shapes.or(eastArm, westArm, southArm, northArm);
		}

		if (ConnectionsDownEast) {

			northArm = box(4, 0, 0, 12, 12, 16);
			eastArm = box(4, 0, 4, 16, 16, 12);
			westArm = box(4, 4, 0, 16, 12, 16);
			southArm = box(0, 0, 4, 16, 12, 12);

			return Shapes.or(eastArm, westArm, southArm, northArm);
		}

		if (ConnectionsDownNorth) {

			northArm = box(4, 0, 0, 12, 12, 16);
			eastArm = box(4, 0, 0, 12, 16, 12);
			westArm = box(0, 4, 0, 16, 12, 12);
			southArm = box(0, 0, 4, 16, 12, 12);

			return Shapes.or(northArm, eastArm, westArm, southArm);
		}

		if (ConnectionsDownSouth) {

			northArm = box(4, 0, 0, 12, 12, 16);
			eastArm = box(4, 0, 4, 12, 16, 16);
			westArm = box(0, 4, 4, 16, 12, 16);
			southArm = box(0, 0, 4, 16, 12, 12);

			return Shapes.or(eastArm, westArm, southArm, northArm);
		}

		if (ConnectionsNorthWest) {

			northArm = box(4, 0, 0, 12, 16, 12);
			eastArm = box(0, 4, 0, 16, 12, 12);
			westArm = box(0, 4, 4, 12, 12, 16);
			southArm = box(0, 0, 4, 12, 16, 12);

			return Shapes.or(southArm, westArm, eastArm, northArm);
		}

		if (ConnectionsNorthEast) {

			northArm = box(4, 0, 0, 12, 16, 12);
			eastArm = box(0, 4, 0, 16, 12, 12);
			westArm = box(4, 4, 4, 16, 12, 16);
			southArm = box(4, 0, 4, 16, 16, 12);

			return Shapes.or(northArm, eastArm, westArm, southArm);
		}

		if (ConnectionsSouthEast) {

			northArm = box(4, 0, 4, 12, 16, 16);
			eastArm = box(0, 4, 4, 16, 12, 16);
			westArm = box(4, 4, 0, 16, 12, 16);
			southArm = box(4, 0, 4, 16, 16, 12);

			return Shapes.or(northArm, eastArm, westArm, southArm);
		}

		if (ConnectionsSouthWest) {

			northArm = box(4, 0, 4, 12, 16, 16);
			eastArm = box(0, 4, 4, 16, 12, 16);
			westArm = box(0, 4, 0, 12, 12, 16);
			southArm = box(0, 0, 4, 12, 16, 12);

			return Shapes.or(northArm, eastArm, westArm, southArm);
		}

		return defaultShape;
	}

	private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
		return Shapes.box(x1 / 16.0, y1 / 16.0, z1 / 16.0, x2 / 16.0, y2 / 16.0, z2 / 16.0);
	}
}
