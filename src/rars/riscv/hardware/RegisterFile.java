package rars.riscv.hardware;

import rars.Globals;
import rars.Settings;
import rars.assembler.SymbolTable;
import rars.riscv.Instruction;

import java.util.Observer;

/**
 * Represents the collection of RISCV integer registers.
 *
 * @author Jason Bumgarner, Jason Shrewsbury
 * @version June 2003
 **/

public class RegisterFile {

    public static final int GLOBAL_POINTER_REGISTER = 3;
    public static final int STACK_POINTER_REGISTER = 2;
    private static final RegisterBlock instance = new RegisterBlock('x', new Register[]{
            new Register("zero", 0, 0), new Register("ra", 1, 0),
            new Register("sp", STACK_POINTER_REGISTER, Memory.stackPointer),
            new Register("gp", GLOBAL_POINTER_REGISTER, Memory.globalPointer),
            new Register("tp", 4, 0), new Register("t0", 5, 0),
            new Register("t1", 6, 0), new Register("t2", 7, 0),
            new Register("s0", 8, 0), new Register("s1", 9, 0),
            new Register("a0", 10, 0), new Register("a1", 11, 0),
            new Register("a2", 12, 0), new Register("a3", 13, 0),
            new Register("a4", 14, 0), new Register("a5", 15, 0),
            new Register("a6", 16, 0), new Register("a7", 17, 0),
            new Register("s2", 18, 0), new Register("s3", 19, 0),
            new Register("s4", 20, 0), new Register("s5", 21, 0),
            new Register("s6", 22, 0), new Register("s7", 23, 0),
            new Register("s8", 24, 0), new Register("s9", 25, 0),
            new Register("s10", 26, 0), new Register("s11", 27, 0),
            new Register("t3", 28, 0), new Register("t4", 29, 0),
            new Register("t5", 30, 0), new Register("t6", 31, 0)
    });

    private static Register programCounter = new Register("pc", -1, Memory.textBaseAddress);

    /**
     * This method updates the register value who's number is num.  Also handles the lo and hi registers
     *
     * @param num Register to set the value of.
     * @param val The desired value for the register.
     **/

    public static void updateRegister(int num, long val) {
        if (num == 0) {
            ;
        } else {
            if ((Globals.getSettings().getBackSteppingEnabled())) {
                Globals.program.getBackStepper().addRegisterFileRestore(num, instance.updateRegister(num, val));
            } else {
                instance.updateRegister(num, val);
            }
        }
    }

    /**
     * Sets the value of the register given to the value given.
     *
     * @param name Name of register to set the value of.
     * @param val  The desired value for the register.
     **/

    public static void updateRegister(String name, long val) {
        updateRegister(instance.getRegister(name).getNumber(), val);
    }

    /**
     * Returns the value of the register.
     *
     * @param num The register number.
     * @return The value of the given register.
     **/

    public static int getValue(int num) {
        return (int) instance.getValue(num);

    }

    /**
     * Returns the value of the register. Observers are not notified.
     *
     * @param num The register's number.
     * @return value The value of the given register.
     */
    public static int getValueNoNotify(int num) {
        return (int) instance.getValueNoNotify(num);

    }

    /**
     * Returns the value of the register.
     *
     * @param num The register number.
     * @return The value of the given register.
     **/

    public static long getValueLong(int num) {
        return instance.getValue(num);
    }

    public static long getValueLong(String name) {
        return instance.getValue(name);
    }

    /**
     * Returns the value of the register.
     *
     * @param name The register's name.
     * @return The value of the given register.
     **/

    public static int getValue(String name) {
        return (int) instance.getValue(name);
    }

    /**
     * For returning the set of registers.
     *
     * @return The set of registers.
     **/

    public static Register[] getRegisters() {
        return instance.getRegisters();
    }

    /**
     * Get register object corresponding to given name.  If no match, return null.
     *
     * @param name The register name, either in $0 or $zero format.
     * @return The register object,or null if not found.
     **/

    public static Register getRegister(String name) {
        if(name.equals("fp")){
            return instance.getRegister("s0");
        }
        return instance.getRegister(name);
    }

    /**
     * For initializing the Program Counter.  Do not use this to implement jumps and
     * branches, as it will NOT record a backstep entry with the restore value.
     * If you need backstepping capability, use setProgramCounter instead.
     *
     * @param value The value to set the Program Counter to.
     **/

    public static void initializeProgramCounter(int value) {
        programCounter.setValue((long)value);
    }

    /**
     * Will initialize the Program Counter to either the default reset value, or the address
     * associated with source program global label "main", if it exists as a text segment label
     * and the global setting is set.
     *
     * @param startAtMain If true, will set program counter to address of statement labeled
     *                    'main' (or other defined start label) if defined.  If not defined, or if parameter false,
     *                    will set program counter to default reset value.
     **/

    public static void initializeProgramCounter(boolean startAtMain) {
        int mainAddr = Globals.symbolTable.getAddress(SymbolTable.getStartLabel());
        if (startAtMain && mainAddr != SymbolTable.NOT_FOUND && Memory.inTextSegment(mainAddr)) {
            initializeProgramCounter(mainAddr);
            // Make the main function return to a text segment that contains "li a7, 10; ecall"
            updateRegister(1, Memory.addressEndOfTextSegment);
        } else {
            initializeProgramCounter((int)programCounter.getResetValue());
        }
    }

    /**
     * For setting the Program Counter.  Note that ordinary PC update should be done using
     * incrementPC() method. Use this only when processing jumps and branches.
     *
     * @param value The value to set the Program Counter to.
     * @return previous PC value
     **/

    public static int setProgramCounter(int value) {
        int old = (int)programCounter.getValue();
        programCounter.setValue(value);
        if (Globals.getSettings().getBackSteppingEnabled()) {
            Globals.program.getBackStepper().addPCRestore(old);
        }
        return old;
    }

    /**
     * For returning the program counters value.
     *
     * @return The program counters value as an int.
     **/

    public static int getProgramCounter() {
        return (int)programCounter.getValue();
    }

    /**
     * Returns Register object for program counter.  Use with caution.
     *
     * @return program counter's Register object.
     */
    public static Register getProgramCounterRegister() {
        return programCounter;
    }

    /**
     * For returning the program counter's initial (reset) value.
     *
     * @return The program counter's initial value
     **/

    public static int getInitialProgramCounter() {
        return (int)programCounter.getResetValue();
    }

    /**
     * Method to reinitialize the values of the registers.
     * <b>NOTE:</b> Should <i>not</i> be called from command-mode MARS because this
     * this method uses global settings from the registry.  Command-mode must operate
     * using only the command switches, not registry settings.  It can be called
     * from tools running stand-alone, and this is done in
     * <code>AbstractToolAndApplication</code>.
     **/

    public static void resetRegisters() {
        instance.resetRegisters();
        initializeProgramCounter(Globals.getSettings().getBooleanSetting(Settings.Bool.START_AT_MAIN));// replaces "programCounter.resetValue()", DPS 3/3/09
    }

    /**
     * Method to increment the Program counter in the general case (not a jump or branch).
     **/

    public static void incrementPC() {
        programCounter.setValue(programCounter.getValue() + Instruction.INSTRUCTION_LENGTH);
    }

    /**
     * Each individual register is a separate object and Observable.  This handy method
     * will add the given Observer to each one.  Currently does not apply to Program
     * Counter.
     */
    public static void addRegistersObserver(Observer observer) {
        instance.addRegistersObserver(observer);
    }

    /**
     * Each individual register is a separate object and Observable.  This handy method
     * will delete the given Observer from each one.  Currently does not apply to Program
     * Counter.
     */
    public static void deleteRegistersObserver(Observer observer) {
        instance.deleteRegistersObserver(observer);
    }
}
