package rars.dataflow;

import rars.ProgramStatement;
import rars.riscv.Instruction;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the transfer function defined in SignTransfer.java
 * Iterates over the instructions produced by RARS after the
 * assembly of a program.
 */
public class SignAnalysis {

    private final List<ProgramStatement> instructions;
    private final SignState[] stateAfter;

    public SignAnalysis(List<ProgramStatement> instructions) {
        this.instructions = new ArrayList<>(instructions);
        this.stateAfter   = new SignState[instructions.size()];
        for (int i = 0; i < stateAfter.length; i++) {
            stateAfter[i] = new SignState();
        }
    }

    public SignState[] analyze() {
        SignTransfer transfer = new SignTransfer();
        boolean changed = true;

        while (changed) {
            changed = false;

            for (int i = 0; i < instructions.size(); i++) {
                ProgramStatement stmt = instructions.get(i);

                SignState before;
                if (i == 0) {
                    before = new SignState(); 
                } else {
                    before = stateAfter[i - 1].copy();
                    int targetAddr = stmt.getAddress();
                    for (int j = 0; j < i; j++) {
                        if (isBranchTo(instructions.get(j), targetAddr)) {
                            before.joinWith(stateAfter[j]);
                        }
                    }

                    for (int j = i + 1; j < instructions.size(); j++) {
                        if (isBranchTo(instructions.get(j), targetAddr)) {
                            before.joinWith(stateAfter[j]);
                        }
                    }
                }

                SignState after;
                if (isExternalCall(stmt, i)) {
                    after = before.copy();
                    SignTransfer.clobberCallerSaved(after);
                    after.set(1, Sign.POS);
                } else {
                    after = transfer.transfer(stmt, before);
                }

                if (!after.equals(stateAfter[i])) {
                    stateAfter[i] = after;
                    changed = true;
                }
            }
        }

        return stateAfter;
    }

    private boolean isBranchTo(ProgramStatement stmt, int targetAddr) {
        Instruction instr = stmt.getInstruction();
        if (instr == null) return false;
        String m = instr.getName().toLowerCase();
        int[] ops = stmt.getOperands();
        if (ops == null || ops.length == 0) return false;

        if (isBranchMnemonic(m) && ops.length >= 3) {
            int abs = stmt.getAddress() + ops[2];
            return abs == targetAddr;
        }
        if (m.equals("jal") && ops.length >= 2) {
            int abs = stmt.getAddress() + ops[1];
            return abs == targetAddr;
        }
        return false;
    }

    private boolean isBranchMnemonic(String m) {
        switch (m) {
            case "beq": case "bne": case "blt": case "bltu":
            case "bge": case "bgeu": case "beqz": case "bnez":
            case "bltz": case "bgez": case "bgtz": case "blez":
                return true;
            default:
                return false;
        }
    }

    private boolean isExternalCall(ProgramStatement stmt, int idx) {
        Instruction instr = stmt.getInstruction();
        if (instr == null) return false;
        String m = instr.getName().toLowerCase();
        int[] ops = stmt.getOperands();
        if (ops == null) return false;

        if (m.equals("jalr")) return true; 

        if (m.equals("jal") && ops.length >= 2) {
            int targetAbs = stmt.getAddress() + ops[1];
            int minAddr = instructions.get(0).getAddress();
            int maxAddr = instructions.get(instructions.size() - 1).getAddress();
            return targetAbs < minAddr || targetAbs > maxAddr;
        }
        return false;
    }

    public SignState stateAfter(int index) {
        return stateAfter[index];
    }

    public int size() {
        return instructions.size();
    }

    public ProgramStatement instruction(int i) {
        return instructions.get(i);
    }
}