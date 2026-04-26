package rars.dataflow;

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
        asm = asm.replace("\"", "\\\"");
        String signStr = (outSign != Sign.BOTTOM) ? " | " + outSign : "";
        return asm + signStr;
    }
}