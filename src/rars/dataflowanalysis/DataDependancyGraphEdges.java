package rars.dataflow;

/**
 * Defines the dependancies between instructions 
 * Example : 
 * add t2, t0, t1
 * what exactly is stored into t0 and t1 when the values are 
 * fused to create the result of the instruction into t2 ?
 */
public class DDGEdge {

    public final DDGNode from;

    public final DDGNode to;

    public final int register;

    public final Sign sign;

    public DDGEdge(DDGNode from, DDGNode to, int register, Sign sign) {
        this.from     = from;
        this.to       = to;
        this.register = register;
        this.sign     = sign;
    }

    public String dotLabel() {
        return "x" + register + " (" + sign + ")";
    }
}