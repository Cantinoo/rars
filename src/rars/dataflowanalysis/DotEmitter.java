package rars.dataflowanalysis;

import java.io.PrintWriter;
import java.util.List;

public class DotEmitter {

    // I preferred keeping ecall and anything related to a7 outside of my dot file
    // as they are related to syscalls and shouldnt be related to data
    private static boolean isSyscallNode(DDGNode node) {
        if (node.stmt == null || node.stmt.getInstruction() == null) return false;
        String m = node.stmt.getInstruction().getName().toLowerCase();
        if (m.equals("ecall")) return true;
        int[] ops = node.stmt.getOperands();
        if (ops == null || ops.length == 0) return false;
        return ops[0] == 17 && !node.isExternalCall;
    }

    public void emit(List<DDGNode> nodes, PrintWriter out) {
        out.println("digraph DDG {");
        out.println("  rankdir=TB;");
        out.println("  node [fontname=\"Courier\", fontsize=10];");
        out.println("  edge [fontname=\"Courier\", fontsize=9];");
        out.println();

        for (DDGNode node : nodes) {
            if (isSyscallNode(node)) continue;
            String label = escape(node.dotLabel());
            String shape = node.isExternalCall ? "box" : "ellipse";
            String style = node.isExternalCall ? "dashed" : "solid";
            out.printf("  n%d [label=\"%s\", shape=%s, style=\"%s\"];%n",
                    node.id, label, shape, style);
        }

        out.println();

        for (DDGNode node : nodes) {
            if (isSyscallNode(node)) continue;
            for (DDGEdge edge : node.outEdges) {
                if (isSyscallNode(edge.to)) continue;
                out.printf("  n%d -> n%d [label=\"%s\"];%n",
                        edge.from.id, edge.to.id, escape(edge.dotLabel()));
            }
        }

        out.println("}");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}