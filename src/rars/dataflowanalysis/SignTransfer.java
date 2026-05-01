package rars.dataflowanalysis;

import rars.ProgramStatement;
import rars.riscv.Instruction;

/**
 * Defines the transfer function for the sign analysis.
 * Based on the instruction type, it modifies the sign state 
 * to the expected sign.
 * ie, an "ADD" instruction is encountered with
 * t0 = t1 + t2
 * t1 = POS
 * t2 = NEG
 * t0 = TOP
 */

public class SignTransfer {

    public SignState transfer(ProgramStatement stmt, SignState before) {
        SignState after = before.copy();

        Instruction instr = stmt.getInstruction();
        if (instr == null) return after;

        String mnemonic = instr.getName().toLowerCase();
        int[] ops = stmt.getOperands();
        int nops = ops == null ? 0 : ops.length;

        switch (mnemonic) {

            case "add": case "addw":
                if (nops >= 3) after.set(ops[0], Sign.add(before.get(ops[1]), before.get(ops[2])));
                break;
            case "sub": case "subw":
                if (nops >= 3) after.set(ops[0], Sign.sub(before.get(ops[1]), before.get(ops[2])));
                break;
            case "mul": case "mulw":
                if (nops >= 3) after.set(ops[0], Sign.mul(before.get(ops[1]), before.get(ops[2])));
                break;
            case "div": case "divu": case "divw": case "divuw":
                if (nops >= 3) after.set(ops[0], Sign.div(before.get(ops[1]), before.get(ops[2])));
                break;
            case "rem": case "remu": case "remw": case "remuw":
                if (nops >= 3) after.set(ops[0], Sign.TOP);
                break;

            case "addi": case "addiw":
                if (nops >= 3) after.set(ops[0], Sign.add(before.get(ops[1]), Sign.inject(ops[2])));
                break;
            case "subi":  // pseudo: subi rd,rs,imm  (same as addi with -imm)
                if (nops >= 3) after.set(ops[0], Sign.sub(before.get(ops[1]), Sign.inject(ops[2])));
                break;
            case "muli":
                if (nops >= 3) after.set(ops[0], Sign.mul(before.get(ops[1]), Sign.inject(ops[2])));
                break;

            case "and": case "andi":
            case "or":  case "ori":
            case "xor": case "xori":
            case "sll": case "slli": case "sllw": case "slliw":
            case "srl": case "srli": case "srlw": case "srliw":
            case "sra": case "srai": case "sraw": case "sraiw":
                if (nops >= 1) after.set(ops[0], Sign.TOP);
                break;

            case "slt": case "sltu": case "slti": case "sltiu":
                if (nops >= 1) after.set(ops[0], Sign.ZERO_POS);
                break;

            case "lui":
                if (nops >= 2) after.set(ops[0], Sign.inject(ops[1]));
                break;
            case "auipc":
                if (nops >= 1) after.set(ops[0], Sign.POS);
                break;

            case "lb": case "lbu": case "lh": case "lhu":
            case "lw": case "lwu": case "ld":
                if (nops >= 1) after.set(ops[0], Sign.TOP);
                break;

            case "sb": case "sh": case "sw": case "sd":
                break;

            case "mv":
                if (nops >= 2) after.set(ops[0], before.get(ops[1]));
                break;
            case "neg": case "negw":
                if (nops >= 2) after.set(ops[0], Sign.neg(before.get(ops[1])));
                break;
            case "not":
                // bitwise not → sign unknown
                if (nops >= 1) after.set(ops[0], Sign.TOP);
                break;

            case "li":
                if (nops >= 2) after.set(ops[0], Sign.inject(ops[1]));
                break;

            case "beq": case "bne":
            case "blt": case "bltu":
            case "bge": case "bgeu":
            case "beqz": case "bnez":
            case "bltz": case "bgez":
            case "bgtz": case "blez":
                break;

            case "jal":
                if (nops >= 1 && ops[0] != 0) {
                    after.set(ops[0], Sign.POS);
                }
                break;

            case "jalr":
                if (nops >= 1 && ops[0] != 0) {
                    after.set(ops[0], Sign.POS); 
                }
                clobberCallerSaved(after);
                break;

            case "ecall": case "ebreak":
                break;
            case "csrrs": case "csrrw": case "csrrc":
            case "csrrsi": case "csrrwi": case "csrrci":
                if (nops >= 1) after.set(ops[0], Sign.TOP);
                break;

            default:
                if (nops >= 1) after.set(ops[0], Sign.TOP);
                break;
        }

        return after;
    }

    public static void clobberCallerSaved(SignState s) {
        int[] callerSaved = {1, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16, 17, 28, 29, 30, 31};
        for (int r : callerSaved) {
            s.set(r, Sign.TOP);
        }
    }
}