package rars.dataflowanalysis;

import rars.ProgramStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines what a node looks like inside of the Data Dependancy Graph
 */
public class DDGNode {

    public final int id;

    public final ProgramStatement stmt;

    public final Sign outSign;

    public final boolean isExternalCall;

    public final String callLabel;

    public final List<DDGEdge> outEdges = new ArrayList<>();

    public DDGNode(int id, ProgramStatement stmt, Sign outSign,
                   boolean isExternalCall, String callLabel) {
        this.id             = id;
        this.stmt           = stmt;
        this.outSign        = outSign;
        this.isExternalCall = isExternalCall;
        this.callLabel      = callLabel;
    }

    public String dotLabel() {
        if (isExternalCall && callLabel != null) {
            return "call " + callLabel;
        }
        String asm = stmt.getBasicAssemblyStatement();
        if (asm == null || asm.isEmpty()) asm = stmt.getSource().trim();
        asm = replaceRegisters(asm);
        asm = asm.replace("\"", "\\\"");
        String signStr = (outSign != Sign.BOTTOM) ? " | " + outSign : "";
        return asm + signStr;
    }

    private static String replaceRegisters(String asm) {
        for (int i = 31; i >= 0; i--) {
            asm = asm.replaceAll("\\bx" + i + "\\b", abiName(i));
        }
        return asm;
    }

    static String abiName(int reg) {
        String[] names = {
            "zero", "ra", "sp", "gp", "tp",
            "t0", "t1", "t2",
            "s0", "s1",
            "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7",
            "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11",
            "t3", "t4", "t5", "t6"
        };
        return (reg >= 0 && reg < 32) ? names[reg] : "x" + reg;
    }
}