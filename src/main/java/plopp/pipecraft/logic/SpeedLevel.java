package plopp.pipecraft.logic;

import net.minecraft.util.StringRepresentable;

public enum SpeedLevel implements StringRepresentable {
	LEVEL_1(1), LEVEL_2(2), LEVEL_3(3), LEVEL_4(4), LEVEL_5(5), LEVEL_6(6), LEVEL_7(7), LEVEL_8(8),
	LEVEL_9(9), LEVEL_10(10), LEVEL_11(11), LEVEL_12(12), LEVEL_13(13), LEVEL_14(14), LEVEL_15(15), LEVEL_16(16),
	LEVEL_17(17), LEVEL_18(18), LEVEL_19(19), LEVEL_20(20), LEVEL_21(21), LEVEL_22(22), LEVEL_23(23), LEVEL_24(24),
	LEVEL_25(25), LEVEL_26(26), LEVEL_27(27), LEVEL_28(28), LEVEL_29(29), LEVEL_30(30), LEVEL_31(31), LEVEL_32(32),
	LEVEL_33(33), LEVEL_34(34), LEVEL_35(35), LEVEL_36(36), LEVEL_37(37), LEVEL_38(38), LEVEL_39(39), LEVEL_40(40),
	LEVEL_41(41), LEVEL_42(42), LEVEL_43(43), LEVEL_44(44), LEVEL_45(45), LEVEL_46(46), LEVEL_47(47), LEVEL_48(48),
	LEVEL_49(49), LEVEL_50(50), LEVEL_51(51), LEVEL_52(52), LEVEL_53(53), LEVEL_54(54), LEVEL_55(55), LEVEL_56(56),
	LEVEL_57(57), LEVEL_58(58), LEVEL_59(59), LEVEL_60(60), LEVEL_61(61), LEVEL_62(62), LEVEL_63(63), LEVEL_64(64),
	LEVEL_65(65), LEVEL_66(66), LEVEL_67(67), LEVEL_68(68), LEVEL_69(69), LEVEL_70(70), LEVEL_71(71), LEVEL_72(72),
	LEVEL_73(73), LEVEL_74(74), LEVEL_75(75), LEVEL_76(76), LEVEL_77(77), LEVEL_78(78), LEVEL_79(79), LEVEL_80(80),
	LEVEL_81(81), LEVEL_82(82), LEVEL_83(83), LEVEL_84(84), LEVEL_85(85), LEVEL_86(86), LEVEL_87(87), LEVEL_88(88),
	LEVEL_89(89), LEVEL_90(90), LEVEL_91(91), LEVEL_92(92), LEVEL_93(93), LEVEL_94(94), LEVEL_95(95), LEVEL_96(96),
	LEVEL_97(97), LEVEL_98(98), LEVEL_99(99),  LEVEL_100(100), LEVEL_101(101), LEVEL_102(102), LEVEL_103(103), LEVEL_104(104),
	LEVEL_105(105), LEVEL_106(106), LEVEL_107(107), LEVEL_108(108), LEVEL_109(109), LEVEL_110(110), LEVEL_111(111), LEVEL_112(112),
	LEVEL_113(113), LEVEL_114(114), LEVEL_115(115), LEVEL_116(116), LEVEL_117(117), LEVEL_118(118), LEVEL_119(119), LEVEL_120(120),
	LEVEL_121(121), LEVEL_122(122), LEVEL_123(123), LEVEL_124(124), LEVEL_125(125), LEVEL_126(126), LEVEL_127(127), LEVEL_128(128); 

    private final int value;

    SpeedLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String getSerializedName() {
        int visual = 129 - value; // 1 → 128, 128 → 1
        return String.valueOf(visual);
    }

    public static SpeedLevel fromInt(int i) {
        for (SpeedLevel level : values()) {
            if (level.getValue() == i) return level;
        }
        return LEVEL_32; // default 
    }
}


