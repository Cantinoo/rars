package rars.dataflowanalysis;

import rars.AssemblyException;
import rars.Globals;
import rars.ProgramStatement;
import rars.api.Options;
import rars.api.Program;
import rars.Settings;
import rars.riscv.hardware.Memory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


/** 
 * Generates the instruction set for a given program using the RARS compiler directly
 * This is much simpler than adding the handling of macros and .eqv to my linter so
 * I preferred this approach (even though some bugs defined in the run function were encountered)
 */ 

public class DataFlowRunner {

    public static boolean run(String sourceFile, String outputDot, java.io.PrintStream out) {

        Memory.getInstance().clear();

        // I havent really tried tweaking with the start at main and other settings, but I often had
        // errors related to "double text segments located at adress X" bugs, especially with START_AT_MAIN
        // enable, would be nice to be able to fix properly later on
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.EXCEPTION_HANDLER_ENABLED, false);
        Globals.getSettings().setExceptionHandler("");
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.ASSEMBLE_OPEN, false);
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.RV64_ENABLED, false);
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.START_AT_MAIN, false);
        rars.riscv.InstructionSet.rv64 = false;
        Globals.instructionSet.populate();

        Program program = new Program(new Options());
        try {
                program.assemble(sourceFile);
        } catch (AssemblyException e) {
                out.println("[df] Assembly failed:");
                out.println(e.errors().generateErrorAndWarningReport());
                return false;
        }

        List<ProgramStatement> machineList = new ArrayList<>(program.getCode().getMachineList());
        if (machineList.isEmpty()) {
                out.println("[df] No instructions found after assembly.");
                return false;
        }

        out.println("[df] Assembled " + machineList.size() + " instruction(s).");

        SignAnalysis analysis = new SignAnalysis(machineList);
        analysis.analyze();
        out.println("[df] Sign analysis complete.");

        DDGBuilder builder = new DDGBuilder(analysis);
        List<DDGNode> nodes = builder.build();
        out.println("[df] DDG built: " + nodes.size() + " node(s).");

        try (PrintWriter pw = new PrintWriter(new File(outputDot))) {
                new DotEmitter().emit(nodes, pw);
        } catch (FileNotFoundException e) {
                out.println("[df] Could not write output file: " + outputDot);
                return false;
        }

        out.println("[df] Dataflow graph written to: " + outputDot);
        return true;
    }

    public static String defaultDotPath(String sourceFile) {
        File f = new File(sourceFile);
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        String base = (dot >= 0) ? name.substring(0, dot) : name;
        return base + ".dot";
    }
}