package rars.dataflowanalysis;

/**
 * Seven element sign lattice for my dataflow implementation
 * Credits given to INF889A - UQAM - a class on program analysis
 * given by Quentin Stiévenart and his implementation of dataflow
 * analysis in the homebrewed miniC compiler
 * 
 * Ordering of the dataflow is defined by : 
 * Bottom <= everything
 * NEG, ZERO and POS <= ZERO_NEG and ZERO_POS
 * ZERO_NEG and ZERO_POS <= TOP
 * everything <= TOP
 */

public enum Sign {
 
    BOTTOM("bottom"),
    NEG("-"),
    ZERO("0"),
    POS("+"),
    ZERO_NEG("0-"),
    ZERO_POS("0+"),
    TOP("top");
 
    private final String symbol;
 
    Sign(String symbol) { this.symbol = symbol; }
 
    @Override public String toString() { return symbol; }
 
 
    public static Sign join(Sign a, Sign b) {
        if (a == b)      return a;
        if (a == BOTTOM) return b;
        if (b == BOTTOM) return a;
        if (a == TOP || b == TOP) return TOP;
        if (unord(a, b, NEG,  ZERO))     return ZERO_NEG;
        if (unord(a, b, POS,  ZERO))     return ZERO_POS;
        if (unord(a, b, NEG,  ZERO_NEG)) return ZERO_NEG;
        if (unord(a, b, ZERO, ZERO_NEG)) return ZERO_NEG;
        if (unord(a, b, POS,  ZERO_POS)) return ZERO_POS;
        if (unord(a, b, ZERO, ZERO_POS)) return ZERO_POS;
        return TOP;
    }
 
    private static boolean unord(Sign a, Sign b, Sign x, Sign y) {
        return (a == x && b == y) || (a == y && b == x);
    }
 
    public static boolean leq(Sign a, Sign b) {
        if (a == b || a == BOTTOM || b == TOP) return true;
        if (b == ZERO_NEG && (a == NEG  || a == ZERO)) return true;
        if (b == ZERO_POS && (a == ZERO || a == POS))  return true;
        return false;
    }
 
    public static Sign add(Sign a, Sign b) { return Tables.ADD[a.ordinal()][b.ordinal()]; }
    public static Sign sub(Sign a, Sign b) { return Tables.SUB[a.ordinal()][b.ordinal()]; }
    public static Sign mul(Sign a, Sign b) { return Tables.MUL[a.ordinal()][b.ordinal()]; }
 
    public static Sign neg(Sign a) {
        switch (a) {
            case NEG:      return POS;
            case POS:      return NEG;
            case ZERO_NEG: return ZERO_POS;
            case ZERO_POS: return ZERO_NEG;
            default:       return a;
        }
    }
 
    public static Sign div(Sign a, Sign b) {
        if (a == BOTTOM || b == BOTTOM) return BOTTOM;
        if (b == ZERO)  return TOP;
        if (a == ZERO)  return ZERO;
        if ((a == POS && b == POS) || (a == NEG && b == NEG)) return ZERO_POS;
        if ((a == POS && b == NEG) || (a == NEG && b == POS)) return ZERO_NEG;
        return TOP;
    }
 
    public static Sign inject(long value) {
        if (value > 0) return POS;
        if (value < 0) return NEG;
        return ZERO;
    }
 
    static final class Tables {
        private static final int B=0,N=1,Z=2,P=3,ZN=4,ZP=5,T=6;
 
        private static Sign s(int i) { return Sign.values()[i]; }

        static final Sign[][] ADD  = new Sign[7][7];
        static final Sign[][] SUB  = new Sign[7][7];
        static final Sign[][] MUL  = new Sign[7][7];
 
        static {
            int[][] add = {
                //   B  N  Z  P ZN ZP  T
                /*B*/{B, B, B, B, B, B, B},
                /*N*/{B, N, N, T, N, T, T},
                /*Z*/{B, N, Z, P,ZN,ZP, T},
                /*P*/{B, T, P, P, T, P, T},
                /*ZN*/{B,N,ZN, T,ZN, T, T},
                /*ZP*/{B, T,ZP, P, T,ZP, T},
                /*T*/{B, T, T, T, T, T, T},
            };
            int[][] sub = {
                //   B  N  Z  P ZN ZP  T
                /*B*/{B, B, B, B, B, B, B},
                /*N*/{B, T, N, N, T, N, T},
                /*Z*/{B, P, Z, N,ZP,ZN, T},
                /*P*/{B, P, P, T, P, T, T},
                /*ZN*/{B,T,ZN,ZN, T,ZN, T},
                /*ZP*/{B,ZP,ZP, T,ZP, T, T},
                /*T*/{B, T, T, T, T, T, T},
            };
            int[][] mul = {
                //   B  N  Z  P ZN ZP  T
                /*B*/{B, B, B, B, B, B, B},
                /*N*/{B, P, Z, N,ZP,ZN, T},
                /*Z*/{B, Z, Z, Z, Z, Z, T},
                /*P*/{B, N, Z, P,ZN,ZP, T},
                /*ZN*/{B,ZP,Z,ZN, T, T, T},
                /*ZP*/{B,ZN,Z,ZP, T, T, T},
                /*T*/{B, T, T, T, T, T, T},
            };

            for (int i = 0; i < 7; i++)
                for (int j = 0; j < 7; j++) {
                    ADD[i][j] = s(add[i][j]);
                    SUB[i][j] = s(sub[i][j]);
                    MUL[i][j] = s(mul[i][j]);
                }
        }
    }
}
