package rars.dataflow;

import java.util.Arrays;

/**
 * I defined a map with the 32 registers in mind, where
 * each register is linked directly to a sign based
 * on the lattice defined in Sign.java
 * 
 * Note that the register x0 (register number 0) is defined
 * to ALWAYS be equal to zero, so exceptions were made in the code
 * to handle this.
 */

public class SignState {

    public static final int NUM_REGS = 32;

    private final Sign[] signs;

    public SignState() {
        signs = new Sign[NUM_REGS];
        Arrays.fill(signs, Sign.BOTTOM);
        signs[0] = Sign.ZERO; 
    }

    private SignState(Sign[] src) {
        signs = Arrays.copyOf(src, NUM_REGS);
    }

    public Sign get(int reg) {
        return signs[reg];
    }

    public void set(int reg, Sign s) {
        if (reg == 0) return; 
        signs[reg] = s;
    }

    public boolean joinWith(SignState other) {
        boolean changed = false;
        for (int i = 1; i < NUM_REGS; i++) {
            Sign joined = Sign.join(signs[i], other.signs[i]);
            if (joined != signs[i]) {
                signs[i] = joined;
                changed = true;
            }
        }
        return changed;
    }

    public SignState copy() {
        return new SignState(signs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SignState)) return false;
        return Arrays.equals(signs, ((SignState) o).signs);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(signs);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < NUM_REGS; i++) {
            if (signs[i] != Sign.BOTTOM) {
                sb.append("x").append(i).append("=").append(signs[i]).append(" ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}