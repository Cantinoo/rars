package rars.venus;

import rars.Globals;
import rars.Settings;
import rars.SimulationException;
import rars.riscv.InstructionSet;
import rars.riscv.dump.DumpFormatLoader;
import rars.simulator.Simulator;
import rars.simulator.SimulatorNotice;
import rars.tools.ConversionTool;
import rars.venus.registers.ControlAndStatusWindow;
import rars.venus.registers.FloatingPointWindow;
import rars.venus.registers.RegistersPane;
import rars.venus.registers.RegistersWindow;
import rars.venus.run.*;
import rars.venus.settings.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.ArrayList;

/**
 * Top level container for Venus GUI.
 *
 * @author Sanderson and Team JSpim
 **/

/* Heavily modified by Pete Sanderson, July 2004, to incorporate JSPIMMenu and JSPIMToolbar
 * not as subclasses of JMenuBar and JToolBar, but as instances of them.  They are both
 * here primarily so both can share the Action objects.
 */

public class VenusUI extends JFrame {
    VenusUI mainUI;
    public JMenuBar menu;
    private JToolBar toolbar;
    private MainPane mainPane;
    private RegistersPane registersPane;
    private RegistersWindow registersTab;
    private FloatingPointWindow fpTab;
    private ControlAndStatusWindow csrTab;
    private MessagesPane messagesPane;
    private JSplitPane splitter, horizonSplitter;
    JPanel north;

    private int frameState; // see windowActivated() and windowDeactivated()
    private static int menuState = FileStatus.NO_FILE;

    // PLEASE PUT THESE TWO (& THEIR METHODS) SOMEWHERE THEY BELONG, NOT HERE
    private boolean reset = true; // registers/memory reset for execution
    private boolean started = false;  // started execution
    Editor editor;

    // components of the menubar
    private JMenu file, run, window, help, edit, settings;
    private JMenuItem fileNew, fileOpen, fileClose, fileCloseAll, fileSave, fileSaveAs, fileSaveAll, fileDumpMemory, fileExit;
    private JMenuItem editUndo, editRedo, editCut, editCopy, editPaste, editFindReplace, editSelectAll;
    private JMenuItem runGo, runStep, runBackstep, runReset, runAssemble, runStop, runPause, runClearBreakpoints, runToggleBreakpoints;
    private JCheckBoxMenuItem settingsLabel, settingsValueDisplayBase, settingsAddressDisplayBase,
            settingsExtended, settingsAssembleOnOpen, settingsAssembleAll, settingsAssembleOpen, settingsWarningsAreErrors,
            settingsStartAtMain, settingsSelfModifyingCode, settingsRV64, settingsDeriveCurrentWorkingDirectory, settingsDarkMode, 
            settingsDisplayRegisterNumbers;
    private JMenuItem settingsExceptionHandler, settingsEditor, settingsHighlighting, settingsMemoryConfiguration;
    private JMenuItem helpHelp, helpAbout;

    // components of the toolbar
    private JButton Undo, Redo, Cut, Copy, Paste, FindReplace, SelectAll;
    private JButton New, Open, Save, SaveAs, SaveAll, DumpMemory;
    private JButton Run, Assemble, Reset, Step, Backstep, Stop, Pause;
    private JButton Help;
    ConversionTool conversionTool = new ConversionTool();

    // The "action" objects, which include action listeners.  One of each will be created then
    // shared between a menu item and its corresponding toolbar button.  This is a very cool
    // technique because it relates the button and menu item so closely

    private Action fileNewAction, fileOpenAction, fileCloseAction, fileCloseAllAction, fileSaveAction;
    private Action fileSaveAsAction, fileSaveAllAction, fileDumpMemoryAction, fileExitAction;
    private Action editUndoAction;
    private Action editRedoAction;
    private Action editCutAction, editCopyAction, editPasteAction, editFindReplaceAction, editSelectAllAction;
    private Action runAssembleAction, runGoAction, runStepAction, runBackstepAction, runResetAction,
            runStopAction, runPauseAction, runClearBreakpointsAction, runToggleBreakpointsAction;
    private Action settingsLabelAction, settingsValueDisplayBaseAction, settingsAddressDisplayBaseAction,
            settingsExtendedAction, settingsAssembleOnOpenAction, settingsAssembleOpenAction, settingsAssembleAllAction,
            settingsWarningsAreErrorsAction, settingsStartAtMainAction,
            settingsExceptionHandlerAction, settingsEditorAction, settingsHighlightingAction, settingsMemoryConfigurationAction,
            settingsSelfModifyingCodeAction, settingsRV64Action, settingsDeriveCurrentWorkingDirectoryAction, settingsDarkModeAction,
            settingsDisplayRegisterNumbersAction;
    private Action helpHelpAction, helpAboutAction;


    /**
     * Constructor for the Class. Sets up a window object for the UI
     *
     * @param name Name of the window to be created.
     * @param paths File paths to open width
     **/

    public VenusUI(String name, ArrayList<String> paths) {
        super(name);
        mainUI = this;
        Globals.setGui(this);
        this.editor = new Editor(this);
        Rectangle maximumWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        double screenWidth = maximumWindowBounds.getWidth();
        double screenHeight = maximumWindowBounds.getHeight();
        // basically give up some screen space if running at 800 x 600
        double messageWidthPct = (screenWidth < 1000.0) ? 0.67 : 0.73;
        double messageHeightPct = (screenWidth < 1000.0) ? 0.12 : 0.15;
        double mainWidthPct = (screenWidth < 1000.0) ? 0.67 : 0.73;
        double mainHeightPct = (screenWidth < 1000.0) ? 0.60 : 0.65;
        double registersWidthPct = (screenWidth < 1000.0) ? 0.18 : 0.22;
        double registersHeightPct = (screenWidth < 1000.0) ? 0.72 : 0.80;

        Dimension messagesPanePreferredSize = new Dimension((int) (screenWidth * messageWidthPct), (int) (screenHeight * messageHeightPct));
        Dimension mainPanePreferredSize = new Dimension((int) (screenWidth * mainWidthPct), (int) (screenHeight * mainHeightPct));
        Dimension registersPanePreferredSize = new Dimension((int) (screenWidth * registersWidthPct), (int) (screenHeight * registersHeightPct));

        // the "restore" size (window control button that toggles with maximize)
        // I want to keep it large, with enough room for user to get handles
        //this.setSize((int)(screenWidth*.8),(int)(screenHeight*.8));

        Globals.initialize();

        //  image courtesy of NASA/JPL.
        URL im = this.getClass().getResource(Globals.imagesPath + "RISC-V.png");
        if (im == null) {
            System.out.println("Internal Error: images folder or file not found");
            System.exit(0);
        }
        Image mars = Toolkit.getDefaultToolkit().getImage(im);
        this.setIconImage(mars);
        // Everything in frame will be arranged on JPanel "center", which is only frame component.
        // "center" has BorderLayout and 2 major components:
        //   -- panel (jp) on North with 2 components
        //      1. toolbar
        //      2. run speed slider.
        //   -- split pane (horizonSplitter) in center with 2 components side-by-side
        //      1. split pane (splitter) with 2 components stacked
        //         a. main pane, with 2 tabs (edit, execute)
        //         b. messages pane with 2 tabs (rars, run I/O)
        //      2. registers pane with 3 tabs (register file, coproc 0, coproc 1)
        // I should probably run this breakdown out to full detail.  The components are created
        // roughly in bottom-up order; some are created in component constructors and thus are
        // not visible here.

        registersTab = new RegistersWindow();
        fpTab = new FloatingPointWindow();
        csrTab = new ControlAndStatusWindow();
        registersPane = new RegistersPane(mainUI, registersTab, fpTab, csrTab);
        registersPane.setPreferredSize(registersPanePreferredSize);

        //Insets defaultTabInsets = (Insets)UIManager.get("TabbedPane.tabInsets");
        //UIManager.put("TabbedPane.tabInsets", new Insets(1, 1, 1, 1));
        mainPane = new MainPane(mainUI, editor, registersTab, fpTab, csrTab);
        //UIManager.put("TabbedPane.tabInsets", defaultTabInsets);

  

        mainPane.setPreferredSize(mainPanePreferredSize);
        messagesPane = new MessagesPane();
        messagesPane.setPreferredSize(messagesPanePreferredSize);
        splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPane, messagesPane);
        splitter.setOneTouchExpandable(true);
        splitter.resetToPreferredSizes();
        horizonSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitter, registersPane);
        horizonSplitter.setOneTouchExpandable(true);
        horizonSplitter.resetToPreferredSizes();

        // due to dependencies, do not set up menu/toolbar until now.
        this.createActionObjects();
        menu = this.setUpMenuBar();
        this.setJMenuBar(menu);

        toolbar = this.setUpToolBar();

        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(toolbar);
        jp.add(RunSpeedPanel.getInstance());
        jp.add(conversionTool.createConversionToolPanel());
        JPanel center = new JPanel(new BorderLayout());
        center.add(jp, BorderLayout.NORTH);
        center.add(horizonSplitter);


        this.getContentPane().add(center);

        FileStatus.reset();
        // The following has side effect of establishing menu state
        FileStatus.set(FileStatus.NO_FILE);

        // This is invoked when opening the app.  It will set the app to
        // appear at full screen size.
        this.addWindowListener(
                new WindowAdapter() {
                    public void windowOpened(WindowEvent e) {
                        mainUI.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                });

        // This is invoked when exiting the app through the X icon.  It will in turn
        // check for unsaved edits before exiting.
        this.addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        if (mainUI.editor.closeAll()) {
                            System.exit(0);
                        }
                    }
                });

        // The following will handle the windowClosing event properly in the
        // situation where user Cancels out of "save edits?" dialog.  By default,
        // the GUI frame will be hidden but I want it to do nothing.
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        this.pack();
        this.setVisible(true);

        // Open files
        if (!this.editor.open(paths)) {
            System.out.println("Internal Error: could not open files" + String.join(", ", paths));
            System.exit(1);
        }
    }


    /*
     * Action objects are used instead of action listeners because one can be easily shared between
     * a menu item and a toolbar button.  Does nice things like disable both if the action is
     * disabled, etc.
     */
    private void createActionObjects() {
        try {
            fileNewAction = new GuiAction("New", loadIcon("New22.png"),
                    "Create a new file for editing", KeyEvent.VK_N, makeShortcut(KeyEvent.VK_N)) {
                public void actionPerformed(ActionEvent e) {
                    editor.newFile();
                }
            };
            fileOpenAction = new GuiAction("Open ...", loadIcon("Open22.png"),
                    "Open a file for editing", KeyEvent.VK_O, makeShortcut(KeyEvent.VK_O)) {
                public void actionPerformed(ActionEvent e) {
                    editor.open();
                }
            };
            fileCloseAction = new GuiAction("Close", null, "Close the current file", KeyEvent.VK_C,
                    makeShortcut(KeyEvent.VK_W)) {
                public void actionPerformed(ActionEvent e) {
                    editor.close();
                }
            };
            fileCloseAllAction = new GuiAction("Close All", null, "Close all open files",
                    KeyEvent.VK_L, null) {
                public void actionPerformed(ActionEvent e) {
                    editor.closeAll();
                }
            };
            fileSaveAction = new GuiAction("Save", loadIcon("Save22.png"), "Save the current file",
                    KeyEvent.VK_S, makeShortcut(KeyEvent.VK_S)) {
                public void actionPerformed(ActionEvent e) {
                    editor.save();
                }
            };
            fileSaveAsAction = new GuiAction("Save as ...", loadIcon("SaveAs22.png"),
                    "Save current file with different name", KeyEvent.VK_A, null) {
                public void actionPerformed(ActionEvent e) {
                    editor.saveAs();
                }
            };
            fileSaveAllAction = new GuiAction("Save All", null, "Save all open files",
                    KeyEvent.VK_V, null) {
                public void actionPerformed(ActionEvent e) {
                    editor.saveAll();
                }
            };
            fileDumpMemoryAction = new FileDumpMemoryAction("Dump Memory ...", loadIcon("Dump22.png"),
                    "Dump machine code or data in an available format", KeyEvent.VK_D, makeShortcut(KeyEvent.VK_D),
                    mainUI);
            fileExitAction = new GuiAction("Exit", null, "Exit Rars", KeyEvent.VK_X, null) {
                public void actionPerformed(ActionEvent e) {
                    if (editor.closeAll()) {
                        System.exit(0);
                    }
                }
            };

            editUndoAction = new GuiAction("Undo", loadIcon("Undo22.png"), "Undo last edit",
                    KeyEvent.VK_U, makeShortcut(KeyEvent.VK_Z)) {
                {
                    setEnabled(false);
                }

                public void actionPerformed(ActionEvent e) {
                    EditPane editPane = mainPane.getEditPane();
                    if (editPane != null) {
                        editPane.undo();
                        updateUndoAndRedoState();
                    }
                }
            };
            editRedoAction = new GuiAction("Redo", loadIcon("Redo22.png"), "Redo last edit",
                    KeyEvent.VK_R, makeShortcut(KeyEvent.VK_Y)) {
                {
                    setEnabled(false);
                }

                public void actionPerformed(ActionEvent e) {
                    EditPane editPane = mainPane.getEditPane();
                    if (editPane != null) {
                        editPane.redo();
                        updateUndoAndRedoState();
                    }
                }
            };
            editCutAction = new GuiAction("Cut", loadIcon("Cut22.gif"), "Cut", KeyEvent.VK_C,
                    makeShortcut(KeyEvent.VK_X)) {
                public void actionPerformed(ActionEvent e) {
                    mainPane.getEditPane().cutText();
                }
            };
            editCopyAction = new GuiAction("Copy", loadIcon("Copy22.png"), "Copy", KeyEvent.VK_O,
                    makeShortcut(KeyEvent.VK_C)) {
                public void actionPerformed(ActionEvent e) {
                    mainPane.getEditPane().copyText();
                }
            };
            editPasteAction = new GuiAction("Paste", loadIcon("Paste22.png"), "Paste", KeyEvent.VK_P,
                    makeShortcut(KeyEvent.VK_V)) {
                public void actionPerformed(ActionEvent e) {
                    mainPane.getEditPane().pasteText();
                }
            };
            editFindReplaceAction = new EditFindReplaceAction("Find/Replace", loadIcon("Find22.png"),
                    "Find/Replace", KeyEvent.VK_F, makeShortcut(KeyEvent.VK_F), mainUI);
            editSelectAllAction = new GuiAction("Select All",
                    null, //new ImageIcon(tk.getImage(cs.getResource(Globals.imagesPath+"Find22.png"),
                    "Select All", KeyEvent.VK_A,
                    makeShortcut(KeyEvent.VK_A)) {
                public void actionPerformed(ActionEvent e) {
                    mainPane.getEditPane().selectAllText();
                }
            };

            runAssembleAction = new RunAssembleAction("Assemble", loadIcon("Assemble22.png"),
                    "Assemble the current file and clear breakpoints", KeyEvent.VK_A,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), mainUI);
            runGoAction = new RunGoAction("Go", loadIcon("Play22.png"), "Run the current program",
                    KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), mainUI);
            runStepAction = new RunStepAction("Step", loadIcon("StepForward22.png"),
                    "Run one step at a time", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), mainUI);
            runBackstepAction = new RunBackstepAction("Backstep", loadIcon("StepBack22.png"),
                    "Undo the last step", KeyEvent.VK_B, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), mainUI);
            runPauseAction = new GuiAction("Pause", loadIcon("Pause22.png"),
                    "Pause the currently running program", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)) {
                public void actionPerformed(ActionEvent e) {
                    Simulator.getInstance().pauseExecution();
                    // RunGoAction's "paused" method will do the cleanup.
                }
            };
            runStopAction = new GuiAction("Stop", loadIcon("Stop22.png"),
                    "Stop the currently running program", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)) {
                public void actionPerformed(ActionEvent e) {
                    Simulator.getInstance().stopExecution();
                    // RunGoAction's "stopped" method will take care of the cleanup.
                }
            };
            runResetAction = new RunResetAction("Reset", loadIcon("Reset22.png"), "Reset memory and registers",
                    KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), mainUI);
            runClearBreakpointsAction = new RunClearBreakpointsAction("Clear all breakpoints", null,
                    "Clears all execution breakpoints set since the last assemble.", KeyEvent.VK_K,
                    makeShortcut(KeyEvent.VK_K)
            );
            runToggleBreakpointsAction = new GuiAction("Toggle all breakpoints", null,
                    "Disable/enable all breakpoints without clearing (can also click Bkpt column header)",
                    KeyEvent.VK_T, makeShortcut(KeyEvent.VK_T)) {
                public void actionPerformed(ActionEvent e) {
                    //settingsLabelAction.actionPerformed(e); 
                    mainPane.getExecutePane().getTextSegmentWindow().toggleBreakpoints();
                }
            };
            settingsLabelAction = new SettingsAction("Show Labels Window (symbol table)",
                    "Toggle visibility of Labels window (symbol table) in the Execute tab",
                    Settings.Bool.LABEL_WINDOW_VISIBILITY) {
                public void handler(boolean visibility) {
                    mainPane.getExecutePane().setLabelWindowVisibility(visibility);
                    System.out.println("ExecutePane reference 2");
                }
            };

            settingsValueDisplayBaseAction = new SettingsAction("Values displayed in hexadecimal",
                    "Toggle between hexadecimal and decimal display of memory/register values",
                    Settings.Bool.DISPLAY_VALUES_IN_HEX) {
                public void handler(boolean isHex) {
                    mainPane.getExecutePane().getValueDisplayBaseChooser().setSelected(isHex);
                }
            };
            settingsAddressDisplayBaseAction = new SettingsAction("Addresses displayed in hexadecimal",
                    "Toggle between hexadecimal and decimal display of memory addresses",
                    Settings.Bool.DISPLAY_ADDRESSES_IN_HEX) {
                public void handler(boolean isHex) {
                    mainPane.getExecutePane().getAddressDisplayBaseChooser().setSelected(isHex);
                }
            };
            settingsExtendedAction = new SettingsAction("Permit extended (pseudo) instructions and formats",
                    "If set, extended (pseudo) instructions are formats are permitted.",
                    Settings.Bool.EXTENDED_ASSEMBLER_ENABLED);
            settingsAssembleOnOpenAction = new SettingsAction("Assemble file upon opening",
                    "If set, a file will be automatically assembled as soon as it is opened.  File Open dialog will show most recently opened file.",
                    Settings.Bool.ASSEMBLE_ON_OPEN);
            settingsAssembleAllAction = new SettingsAction("Assemble all files in directory",
                    "If set, all files in current directory will be assembled when Assemble operation is selected.",
                    Settings.Bool.ASSEMBLE_ALL);
            settingsAssembleOpenAction = new SettingsAction("Assemble all files currently open",
                    "If set, all files currently open for editing will be assembled when Assemble operation is selected.",
                    Settings.Bool.ASSEMBLE_OPEN);
            settingsWarningsAreErrorsAction = new SettingsAction("Assembler warnings are considered errors",
                    "If set, assembler warnings will be interpreted as errors and prevent successful assembly.",
                    Settings.Bool.WARNINGS_ARE_ERRORS);
            settingsStartAtMainAction = new SettingsAction("Initialize Program Counter to global 'main' if defined",
                    "If set, assembler will initialize Program Counter to text address globally labeled 'main', if defined.",
                    Settings.Bool.START_AT_MAIN);
            settingsSelfModifyingCodeAction = new SettingsAction("Self-modifying code",
                    "If set, the program can write and branch to both text and data segments.",
                    Settings.Bool.SELF_MODIFYING_CODE_ENABLED);

            // TODO: review this
            settingsRV64Action = new SettingsAction("64 bit",
                    "If set, registers are 64 bits wide and new instructions are available",
                    Settings.Bool.RV64_ENABLED) {
                public void handler(boolean value) {
                    InstructionSet.rv64 = value;
                    Globals.instructionSet.populate();
                    registersTab.updateRegisters();
                    csrTab.updateRegisters();
                }
            };
            settingsDeriveCurrentWorkingDirectoryAction = new SettingsAction("Derive current working directory",
                    "If set, the working directory is derived from the main file instead of the RARS executable directory.",
                    Settings.Bool.DERIVE_CURRENT_WORKING_DIRECTORY);
            settingsDarkModeAction = new SettingsAction("Dark mode", "If set, RARS will be in dark mode at the next opening. Uncheck for light mode",
                    Settings.Bool.DARK_MODE_ENABLED);
            settingsDisplayRegisterNumbersAction = new SettingsAction("Display Register Numbers", "Toggle display of register numbers in the Registers Tab",
                    Settings.Bool.DISPLAY_REGISTER_NUMBERS);

            settingsEditorAction = new SettingsEditorAction("Editor...", null,
                    "View and modify text editor settings.", null, null
            );
            settingsHighlightingAction = new SettingsHighlightingAction("Highlighting...", null,
                    "View and modify Execute Tab highlighting colors", null, null
            );
            settingsExceptionHandlerAction = new SettingsExceptionHandlerAction("Exception Handler...", null,
                    "If set, the specified exception handler file will be included in all Assemble operations.",
                    null, null
            );
            settingsMemoryConfigurationAction = new SettingsMemoryConfigurationAction("Memory Configuration...",
                    null, "View and modify memory segment base addresses for the simulated processor",
                    null, null
            );
            

            helpHelpAction = new HelpHelpAction("Help", loadIcon("Help22.png"),
                    "Help", KeyEvent.VK_H, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), mainUI);
            helpAboutAction = new HelpAboutAction("About ...", null,
                    "Information about Rars", null, null, mainUI);
        } catch (NullPointerException e) {
            System.out.println("Internal Error: images folder not found, or other null pointer exception while creating Action objects");
            e.printStackTrace();
            System.exit(0);
        }
    }
   
    /*
     * build the menus and connect them to action objects (which serve as action listeners
     * shared between menu item and corresponding toolbar icon).
     */

    private JMenuBar setUpMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        run = new JMenu("Run");
        run.setMnemonic(KeyEvent.VK_R);
        //window = new JMenu("Window");
        //window.setMnemonic(KeyEvent.VK_W);
        settings = new JMenu("Settings");
        settings.setMnemonic(KeyEvent.VK_S);
        help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        // slight bug: user typing alt-H activates help menu item directly, not help menu

        fileNew = new JMenuItem(fileNewAction);
        fileNew.setIcon(loadIcon("New16.png"));
        fileOpen = new JMenuItem(fileOpenAction);
        fileOpen.setIcon(loadIcon("Open16.png"));
        fileClose = new JMenuItem(fileCloseAction);
        fileClose.setIcon(loadIcon("MyBlank16.gif"));
        fileCloseAll = new JMenuItem(fileCloseAllAction);
        fileCloseAll.setIcon(loadIcon("MyBlank16.gif"));
        fileSave = new JMenuItem(fileSaveAction);
        fileSave.setIcon(loadIcon("Save16.png"));
        fileSaveAs = new JMenuItem(fileSaveAsAction);
        fileSaveAs.setIcon(loadIcon("SaveAs16.png"));
        fileSaveAll = new JMenuItem(fileSaveAllAction);
        fileSaveAll.setIcon(loadIcon("MyBlank16.gif"));
        fileDumpMemory = new JMenuItem(fileDumpMemoryAction);
        fileDumpMemory.setIcon(loadIcon("Dump16.png"));
        fileExit = new JMenuItem(fileExitAction);
        fileExit.setIcon(loadIcon("MyBlank16.gif"));
        file.add(fileNew);
        file.add(fileOpen);
        file.add(fileClose);
        file.add(fileCloseAll);
        file.addSeparator();
        file.add(fileSave);
        file.add(fileSaveAs);
        file.add(fileSaveAll);
        if (DumpFormatLoader.getDumpFormats().size() > 0) {
            file.add(fileDumpMemory);
        }
        file.addSeparator();
        file.add(fileExit);

        editUndo = new JMenuItem(editUndoAction);
        editUndo.setIcon(loadIcon("Undo16.png"));//"Undo16.gif"));
        editRedo = new JMenuItem(editRedoAction);
        editRedo.setIcon(loadIcon("Redo16.png"));//"Redo16.gif"));
        editCut = new JMenuItem(editCutAction);
        editCut.setIcon(loadIcon("Cut16.gif"));
        editCopy = new JMenuItem(editCopyAction);
        editCopy.setIcon(loadIcon("Copy16.png"));//"Copy16.gif"));
        editPaste = new JMenuItem(editPasteAction);
        editPaste.setIcon(loadIcon("Paste16.png"));//"Paste16.gif"));
        editFindReplace = new JMenuItem(editFindReplaceAction);
        editFindReplace.setIcon(loadIcon("Find16.png"));//"Paste16.gif"));
        editSelectAll = new JMenuItem(editSelectAllAction);
        editSelectAll.setIcon(loadIcon("MyBlank16.gif"));
        edit.add(editUndo);
        edit.add(editRedo);
        edit.addSeparator();
        edit.add(editCut);
        edit.add(editCopy);
        edit.add(editPaste);
        edit.addSeparator();
        edit.add(editFindReplace);
        edit.add(editSelectAll);

        runAssemble = new JMenuItem(runAssembleAction);
        runAssemble.setIcon(loadIcon("Assemble16.png"));//"MyAssemble16.gif"));
        runGo = new JMenuItem(runGoAction);
        runGo.setIcon(loadIcon("Play16.png"));//"Play16.gif"));
        runStep = new JMenuItem(runStepAction);
        runStep.setIcon(loadIcon("StepForward16.png"));//"MyStepForward16.gif"));
        runBackstep = new JMenuItem(runBackstepAction);
        runBackstep.setIcon(loadIcon("StepBack16.png"));//"MyStepBack16.gif"));
        runReset = new JMenuItem(runResetAction);
        runReset.setIcon(loadIcon("Reset16.png"));//"MyReset16.gif"));
        runStop = new JMenuItem(runStopAction);
        runStop.setIcon(loadIcon("Stop16.png"));//"Stop16.gif"));
        runPause = new JMenuItem(runPauseAction);
        runPause.setIcon(loadIcon("Pause16.png"));//"Pause16.gif"));
        runClearBreakpoints = new JMenuItem(runClearBreakpointsAction);
        runClearBreakpoints.setIcon(loadIcon("MyBlank16.gif"));
        runToggleBreakpoints = new JMenuItem(runToggleBreakpointsAction);
        runToggleBreakpoints.setIcon(loadIcon("MyBlank16.gif"));

        run.add(runAssemble);
        run.add(runGo);
        run.add(runStep);
        run.add(runBackstep);
        run.add(runPause);
        run.add(runStop);
        run.add(runReset);
        run.addSeparator();
        run.add(runClearBreakpoints);
        run.add(runToggleBreakpoints);

        settingsLabel = new JCheckBoxMenuItem(settingsLabelAction);
        settingsLabel.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.LABEL_WINDOW_VISIBILITY));
        settingsValueDisplayBase = new JCheckBoxMenuItem(settingsValueDisplayBaseAction);
        settingsValueDisplayBase.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.DISPLAY_VALUES_IN_HEX));//mainPane.getExecutePane().getValueDisplayBaseChooser().isSelected());
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has already been created.
        mainPane.getExecutePane().getValueDisplayBaseChooser().setSettingsMenuItem(settingsValueDisplayBase);
        settingsAddressDisplayBase = new JCheckBoxMenuItem(settingsAddressDisplayBaseAction);
        settingsAddressDisplayBase.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.DISPLAY_ADDRESSES_IN_HEX));//mainPane.getExecutePane().getValueDisplayBaseChooser().isSelected());
        // Tell the corresponding JCheckBox in the Execute Pane about me -- it has already been created.
        mainPane.getExecutePane().getAddressDisplayBaseChooser().setSettingsMenuItem(settingsAddressDisplayBase);
        settingsExtended = new JCheckBoxMenuItem(settingsExtendedAction);
        settingsExtended.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.EXTENDED_ASSEMBLER_ENABLED));
        settingsSelfModifyingCode = new JCheckBoxMenuItem(settingsSelfModifyingCodeAction);
        settingsSelfModifyingCode.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.SELF_MODIFYING_CODE_ENABLED));
        settingsRV64 = new JCheckBoxMenuItem(settingsRV64Action);
        settingsRV64.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.RV64_ENABLED));
        settingsDeriveCurrentWorkingDirectory = new JCheckBoxMenuItem(settingsDeriveCurrentWorkingDirectoryAction);
        settingsDeriveCurrentWorkingDirectory.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.DERIVE_CURRENT_WORKING_DIRECTORY));
        settingsDarkMode = new JCheckBoxMenuItem(settingsDarkModeAction);
        settingsDarkMode.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.DARK_MODE_ENABLED));
        settingsDarkMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (settingsDarkMode.isSelected()) {
                        UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
                        SwingUtilities.updateComponentTreeUI(mainUI);
                        Globals.getSettings().setDarkMode();
                    } else {
                        UIManager.setLookAndFeel("com.formdev.flatlaf.FlatIntelliJLaf");
                        SwingUtilities.updateComponentTreeUI(mainUI);
                        Globals.getSettings().setLightMode();
                    }
                } catch (Exception ex) {
                    System.err.println( "Failed to initialize LaF. Continue with default LaF." );
                }
            }
        });
        settingsDisplayRegisterNumbers = new JCheckBoxMenuItem(settingsDisplayRegisterNumbersAction);
        settingsDisplayRegisterNumbers.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.DISPLAY_REGISTER_NUMBERS));
        settingsAssembleOnOpen = new JCheckBoxMenuItem(settingsAssembleOnOpenAction);
        settingsAssembleOnOpen.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.ASSEMBLE_ON_OPEN));
        settingsAssembleAll = new JCheckBoxMenuItem(settingsAssembleAllAction);
        settingsAssembleAll.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.ASSEMBLE_ALL));
        settingsAssembleOpen = new JCheckBoxMenuItem(settingsAssembleOpenAction);
        settingsAssembleOpen.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.ASSEMBLE_OPEN));
        settingsWarningsAreErrors = new JCheckBoxMenuItem(settingsWarningsAreErrorsAction);
        settingsWarningsAreErrors.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.WARNINGS_ARE_ERRORS));
        settingsStartAtMain = new JCheckBoxMenuItem(settingsStartAtMainAction);
        settingsStartAtMain.setSelected(Globals.getSettings().getBooleanSetting(Settings.Bool.START_AT_MAIN));
        settingsEditor = new JMenuItem(settingsEditorAction);
        settingsHighlighting = new JMenuItem(settingsHighlightingAction);
        settingsExceptionHandler = new JMenuItem(settingsExceptionHandlerAction);
        settingsMemoryConfiguration = new JMenuItem(settingsMemoryConfigurationAction);

        settings.add(settingsLabel);
        settings.add(settingsAddressDisplayBase);
        settings.add(settingsValueDisplayBase);
        settings.addSeparator();
        settings.add(settingsAssembleOnOpen);
        settings.add(settingsAssembleAll);
        settings.add(settingsAssembleOpen);
        settings.add(settingsWarningsAreErrors);
        settings.add(settingsStartAtMain);
        settings.add(settingsDeriveCurrentWorkingDirectory);
        settings.addSeparator();
        settings.add(settingsExtended);
        settings.add(settingsSelfModifyingCode);
        settings.add(settingsRV64);
        settings.addSeparator();
        settings.add(settingsDarkMode);
        settings.add(settingsDisplayRegisterNumbers);
        settings.add(settingsEditor);
        settings.add(settingsHighlighting);
        settings.add(settingsExceptionHandler);
        settings.add(settingsMemoryConfiguration);

        helpHelp = new JMenuItem(helpHelpAction);
        helpHelp.setIcon(loadIcon("Help16.png"));//"Help16.gif"));
        helpAbout = new JMenuItem(helpAboutAction);
        helpAbout.setIcon(loadIcon("MyBlank16.gif"));
        help.add(helpHelp);
        help.addSeparator();
        help.add(helpAbout);

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(run);
        menuBar.add(settings);
        JMenu toolMenu = ToolLoader.buildToolsMenu();
        if (toolMenu != null) menuBar.add(toolMenu);
        menuBar.add(help);

        // experiment with popup menu for settings. 3 Aug 2006 PS
        //setupPopupMenu();

        return menuBar;
    }
   
    /*
     * build the toolbar and connect items to action objects (which serve as action listeners
     * shared between toolbar icon and corresponding menu item).
     */

    JToolBar setUpToolBar() {
        JToolBar toolBar = new JToolBar();

        New = new JButton(fileNewAction);
        New.setText("");
        Open = new JButton(fileOpenAction);
        Open.setText("");
        Save = new JButton(fileSaveAction);
        Save.setText("");
        SaveAs = new JButton(fileSaveAsAction);
        SaveAs.setText("");
        DumpMemory = new JButton(fileDumpMemoryAction);
        DumpMemory.setText("");

        Undo = new JButton(editUndoAction);
        Undo.setText("");
        Redo = new JButton(editRedoAction);
        Redo.setText("");
        Cut = new JButton(editCutAction);
        Cut.setText("");
        Copy = new JButton(editCopyAction);
        Copy.setText("");
        Paste = new JButton(editPasteAction);
        Paste.setText("");
        FindReplace = new JButton(editFindReplaceAction);
        FindReplace.setText("");
        SelectAll = new JButton(editSelectAllAction);
        SelectAll.setText("");

        Run = new JButton(runGoAction);
        Run.setText("");
        Assemble = new JButton(runAssembleAction);
        Assemble.setText("");
        Step = new JButton(runStepAction);
        Step.setText("");
        Backstep = new JButton(runBackstepAction);
        Backstep.setText("");
        Reset = new JButton(runResetAction);
        Reset.setText("");
        Stop = new JButton(runStopAction);
        Stop.setText("");
        Pause = new JButton(runPauseAction);
        Pause.setText("");
        Help = new JButton(helpHelpAction);
        Help.setText("");

        toolBar.add(New);
        toolBar.add(Open);
        toolBar.add(Save);
        toolBar.add(SaveAs);
        if (DumpFormatLoader.getDumpFormats().size() > 0) {
            toolBar.add(DumpMemory);
        }
        toolBar.add(new JToolBar.Separator());
        toolBar.add(Undo);
        toolBar.add(Redo);
        toolBar.add(Cut);
        toolBar.add(Copy);
        toolBar.add(Paste);
        toolBar.add(FindReplace);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(Assemble);
        toolBar.add(Run);
        toolBar.add(Step);
        toolBar.add(Backstep);
        toolBar.add(Pause);
        toolBar.add(Stop);
        toolBar.add(Reset);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(Help);
        toolBar.add(new JToolBar.Separator());

        return toolBar;
    }


    /* Determine from FileStatus what the menu state (enabled/disabled)should 
     * be then call the appropriate method to set it.  Current states are:
     *
     * setMenuStateInitial: set upon startup and after File->Close
     * setMenuStateEditingNew: set upon File->New
     * setMenuStateEditing: set upon File->Open or File->Save or erroneous Run->Assemble
     * setMenuStateRunnable: set upon successful Run->Assemble
     * setMenuStateRunning: set upon Run->Go
     * setMenuStateTerminated: set upon completion of simulated execution
     */
    public void setMenuState(int status) {
        menuState = status;
        switch (status) {
            case FileStatus.NO_FILE:
                setMenuStateInitial();
                break;
            case FileStatus.NEW_NOT_EDITED:
                setMenuStateEditingNew();
                break;
            case FileStatus.NEW_EDITED:
                setMenuStateEditingNew();
                break;
            case FileStatus.NOT_EDITED:
                setMenuStateNotEdited(); // was MenuStateEditing. DPS 9-Aug-2011
                break;
            case FileStatus.EDITED:
                setMenuStateEditing();
                break;
            case FileStatus.RUNNABLE:
                setMenuStateRunnable();
                break;
            case FileStatus.RUNNING:
                setMenuStateRunning();
                break;
            case FileStatus.TERMINATED:
                setMenuStateTerminated();
                break;
            case FileStatus.OPENING:// This is a temporary state. DPS 9-Aug-2011
                break;
            default:
                System.out.println("Invalid File Status: " + status);
                break;
        }
    }


    private void setMenuStateInitial() {
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(false);
        fileCloseAllAction.setEnabled(false);
        fileSaveAction.setEnabled(false);
        fileSaveAsAction.setEnabled(false);
        fileSaveAllAction.setEnabled(false);
        fileDumpMemoryAction.setEnabled(false);
        fileExitAction.setEnabled(true);
        editUndoAction.setEnabled(false);
        editRedoAction.setEnabled(false);
        editCutAction.setEnabled(false);
        editCopyAction.setEnabled(false);
        editPasteAction.setEnabled(false);
        editFindReplaceAction.setEnabled(false);
        editSelectAllAction.setEnabled(false);
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(false);
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runClearBreakpointsAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        updateUndoAndRedoState();
    }

    /* Added DPS 9-Aug-2011, for newly-opened files.  Retain
        existing Run menu state (except Assemble, which is always true).
         Thus if there was a valid assembly it is retained. */
    void setMenuStateNotEdited() {
      /* Note: undo and redo are handled separately by the undo manager*/
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(false);
        fileExitAction.setEnabled(true);
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsMemoryConfigurationAction.setEnabled(true);
        runAssembleAction.setEnabled(true);
        // If assemble-all, allow previous Run menu settings to remain.
        // Otherwise, clear them out.  DPS 9-Aug-2011
        if (!Globals.getSettings().getBooleanSetting(Settings.Bool.ASSEMBLE_ALL)) {
            runGoAction.setEnabled(false);
            runStepAction.setEnabled(false);
            runBackstepAction.setEnabled(false);
            runResetAction.setEnabled(false);
            runStopAction.setEnabled(false);
            runPauseAction.setEnabled(false);
            runClearBreakpointsAction.setEnabled(false);
            runToggleBreakpointsAction.setEnabled(false);
        }
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        updateUndoAndRedoState();
    }


    void setMenuStateEditing() {
      /* Note: undo and redo are handled separately by the undo manager*/
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(false);
        fileExitAction.setEnabled(true);
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(true);
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runClearBreakpointsAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        updateUndoAndRedoState();
    }

    /* Use this when "File -> New" is used
     */
    void setMenuStateEditingNew() {
      /* Note: undo and redo are handled separately by the undo manager*/
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(false);
        fileExitAction.setEnabled(true);
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(false);
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runClearBreakpointsAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        updateUndoAndRedoState();
    }

    /* Use this upon successful assemble or reset
     */
    void setMenuStateRunnable() {
      /* Note: undo and redo are handled separately by the undo manager */
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(true);
        runGoAction.setEnabled(true);
        runStepAction.setEnabled(true);
        runBackstepAction.setEnabled(
                Globals.getSettings().getBackSteppingEnabled() && !Globals.program.getBackStepper().empty());
        runResetAction.setEnabled(true);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(true);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        updateUndoAndRedoState();
    }

    /* Use this while program is running
     */
    void setMenuStateRunning() {
      /* Note: undo and redo are handled separately by the undo manager */
        fileNewAction.setEnabled(false);
        fileOpenAction.setEnabled(false);
        fileCloseAction.setEnabled(false);
        fileCloseAllAction.setEnabled(false);
        fileSaveAction.setEnabled(false);
        fileSaveAsAction.setEnabled(false);
        fileSaveAllAction.setEnabled(false);
        fileDumpMemoryAction.setEnabled(false);
        fileExitAction.setEnabled(false);
        editCutAction.setEnabled(false);
        editCopyAction.setEnabled(false);
        editPasteAction.setEnabled(false);
        editFindReplaceAction.setEnabled(false);
        editSelectAllAction.setEnabled(false);
        settingsMemoryConfigurationAction.setEnabled(false); // added 21 July 2009
        runAssembleAction.setEnabled(false);
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(false);
        runResetAction.setEnabled(false);
        runStopAction.setEnabled(true);
        runPauseAction.setEnabled(true);
        runToggleBreakpointsAction.setEnabled(false);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        editUndoAction.setEnabled(false);//updateUndoState(); // DPS 10 Jan 2008
        editRedoAction.setEnabled(false);//updateRedoState(); // DPS 10 Jan 2008
    }

    /* Use this upon completion of execution
     */
    void setMenuStateTerminated() {
      /* Note: undo and redo are handled separately by the undo manager */
        fileNewAction.setEnabled(true);
        fileOpenAction.setEnabled(true);
        fileCloseAction.setEnabled(true);
        fileCloseAllAction.setEnabled(true);
        fileSaveAction.setEnabled(true);
        fileSaveAsAction.setEnabled(true);
        fileSaveAllAction.setEnabled(true);
        fileDumpMemoryAction.setEnabled(true);
        fileExitAction.setEnabled(true);
        editCutAction.setEnabled(true);
        editCopyAction.setEnabled(true);
        editPasteAction.setEnabled(true);
        editFindReplaceAction.setEnabled(true);
        editSelectAllAction.setEnabled(true);
        settingsMemoryConfigurationAction.setEnabled(true); // added 21 July 2009
        runAssembleAction.setEnabled(true);
        runGoAction.setEnabled(false);
        runStepAction.setEnabled(false);
        runBackstepAction.setEnabled(
                Globals.getSettings().getBackSteppingEnabled() && !Globals.program.getBackStepper().empty());
        runResetAction.setEnabled(true);
        runStopAction.setEnabled(false);
        runPauseAction.setEnabled(false);
        runToggleBreakpointsAction.setEnabled(true);
        helpHelpAction.setEnabled(true);
        helpAboutAction.setEnabled(true);
        updateUndoAndRedoState();
    }


    /**
     * Get current menu state.  State values are constants in FileStatus class.  DPS 23 July 2008
     *
     * @return current menu state.
     **/

    public static int getMenuState() {
        return menuState;
    }

    /**
     * Method called when a simulation is started to update the UI.
     *
     * @return true if the ui was updated to start the simulation.
     */
    public boolean onStartedSimulation(String name) {
        ExecutePane executePane = getMainPane().getExecutePane();

        if (!FileStatus.isAssembled()) {
            // note: this should never occur since "Go" and "Step" are only enabled after successful assembly.
            JOptionPane.showMessageDialog(this, "The program must be assembled before it can be run.");
            return false;
        }

        if (!getStarted()) {
            getMessagesPane().processProgramArgumentsIfAny();
        }

        if (!getReset() && !getStarted()) {
            // This should never occur because at termination the Go and Step buttons are disabled.
            JOptionPane.showMessageDialog(this, "reset " + mainUI.getReset() + " started " + mainUI.getStarted());//"You must reset before you can execute the program again.");
            return false;
        }

        setStarted(true);  // added 8/27/05

        getMessagesPane().postMessage(name + ": running " + FileStatus.getFile().getName() + "\n\n");
        getMessagesPane().selectRunMessageTab();
        executePane.getTextSegmentWindow().setCodeHighlighting(false);
        executePane.getTextSegmentWindow().unhighlightAllSteps();
        //clears highlight of registers and data segment if the run step was used before
        executePane.getRegistersWindow().clearHighlighting();
        executePane.getFloatingPointWindow().clearHighlighting();
        executePane.getControlAndStatusWindow().clearHighlighting();
        executePane.getDataSegmentWindow().clearHighlighting();

        return true;
    }

    /**
     * Method called when a simulation is stopped to update the UI.
     */
    public void onStoppedSimulation(String name, SimulatorNotice notice) {
        ExecutePane executePane = getMainPane().getExecutePane();
        executePane.getRegistersWindow().updateRegisters();
        executePane.getFloatingPointWindow().updateRegisters();
        executePane.getControlAndStatusWindow().updateRegisters();
        executePane.getDataSegmentWindow().updateValues();

        if (notice.getDone()) {
            RunGoAction.resetMaxSteps();
            executePane.getTextSegmentWindow().unhighlightAllSteps();
            executePane.getTextSegmentWindow().setCodeHighlighting(false);
            FileStatus.set(FileStatus.TERMINATED);
        } else {
            executePane.getTextSegmentWindow().setCodeHighlighting(true);
            executePane.getTextSegmentWindow().highlightStepAtPC();
            FileStatus.set(FileStatus.RUNNABLE);
        }

        Simulator.Reason reason = notice.getReason();
        switch (reason) {
            case NORMAL_TERMINATION:
                mainUI.getMessagesPane().postMessage(
                        "\n" + name + ": execution completed successfully.\n\n");
                mainUI.getMessagesPane().postRunMessage(
                        "\n-- program is finished running (" + Globals.exitCode + ") --\n\n");
                mainUI.getMessagesPane().selectRunMessageTab();
                break;
            case CLIFF_TERMINATION:
                mainUI.getMessagesPane().postMessage(
                        "\n" + name + ": execution terminated by null instruction.\n\n");
                mainUI.getMessagesPane().postRunMessage(
                        "\n-- program is finished running (dropped off bottom) --\n\n");
                mainUI.getMessagesPane().selectRunMessageTab();
                break;
            case EXCEPTION:
                SimulationException pe = notice.getException();
                mainUI.getMessagesPane().postMessage(
                        pe.error().generateReport());
                mainUI.getMessagesPane().postMessage(
                        "\n" + name + ": execution terminated with errors.\n\n");
                mainUI.getMessagesPane().postRunMessage("\n"+pe.error().getMessage());
                break;
            case STOP:
                mainUI.getMessagesPane().postMessage(
                        "\n" + name + ": execution terminated by user.\n\n");
                mainUI.getMessagesPane().selectMessageTab();
                break;
            case MAX_STEPS:
                int maxSteps = notice.getMaxSteps();
                if (maxSteps != 1) {
                    // do not display something on Step Action
                    mainUI.getMessagesPane().postMessage(
                            "\n" + name + ": execution step limit of " + maxSteps + " exceeded.\n\n");
                    mainUI.getMessagesPane().selectMessageTab();
                }
                break;
            case BREAKPOINT:
                mainUI.getMessagesPane().postMessage(
                        "\n" + name + ": execution paused at breakpoint: " + FileStatus.getFile().getName() + "\n\n");
                break;
            case PAUSE:
                mainUI.getMessagesPane().postMessage(
                        "\n" + name + ": execution paused by user: " + FileStatus.getFile().getName() + "\n\n");
                break;
            default:
                // Ne devrait pas arriver
                throw new IllegalStateException("Unexpected value: " + reason);
        }

        mainUI.setReset(false);
    }

    /**
     * To set whether the register values are reset.
     *
     * @param b Boolean true if the register values have been reset.
     **/

    public void setReset(boolean b) {
        reset = b;
    }

    /**
     * To set whether MIPS program execution has started.
     *
     * @param b true if the MIPS program execution has started.
     **/

    public void setStarted(boolean b) {
        started = b;
    }

    /**
     * To find out whether the register values are reset.
     *
     * @return Boolean true if the register values have been reset.
     **/

    public boolean getReset() {
        return reset;
    }

    /**
     * To find out whether MIPS program is currently executing.
     *
     * @return true if MIPS program is currently executing.
     **/
    public boolean getStarted() {
        return started;
    }

    /**
     * Get reference to Editor object associated with this GUI.
     *
     * @return Editor for the GUI.
     **/

    public Editor getEditor() {
        return editor;
    }

    /**
     * Get reference to messages pane associated with this GUI.
     *
     * @return MessagesPane object associated with the GUI.
     **/

    public MainPane getMainPane() {
        return mainPane;
    }

    /**
     * Get reference to messages pane associated with this GUI.
     *
     * @return MessagesPane object associated with the GUI.
     **/

    public MessagesPane getMessagesPane() {
        return messagesPane;
    }

    /**
     * Get reference to registers pane associated with this GUI.
     *
     * @return RegistersPane object associated with the GUI.
     **/

    public RegistersPane getRegistersPane() {
        return registersPane;
    }

    /**
     * Get reference to settings menu item for display base of memory/register values.
     *
     * @return the menu item
     **/

    public JCheckBoxMenuItem getValueDisplayBaseMenuItem() {
        return settingsValueDisplayBase;
    }

    /**
     * Get reference to settings menu item for display base of memory/register values.
     *
     * @return the menu item
     **/

    public JCheckBoxMenuItem getAddressDisplayBaseMenuItem() {
        return settingsAddressDisplayBase;
    }

    /**
     * Return reference tothe Run->Assemble item's action.  Needed by File->Open in case
     * assemble-upon-open flag is set.
     *
     * @return the Action object for the Run->Assemble operation.
     */
    public Action getRunAssembleAction() {
        return runAssembleAction;
    }

    /**
     * Have the menu request keyboard focus.  DPS 5-4-10
     */
    public void haveMenuRequestFocus() {
        this.menu.requestFocus();
    }

    /**
     * Send keyboard event to menu for possible processing.  DPS 5-4-10
     *
     * @param evt KeyEvent for menu component to consider for processing.
     */
    public void dispatchEventToMenu(KeyEvent evt) {
        this.menu.dispatchEvent(evt);
    }

    public void updateUndoAndRedoState() {
        EditPane editPane = getMainPane().getEditPane();
        editUndoAction.setEnabled(editPane != null && editPane.getUndoManager().canUndo());
        editRedoAction.setEnabled(editPane != null && editPane.getUndoManager().canRedo());
    }

    private ImageIcon loadIcon(String name) {
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Globals.imagesPath + name)));
    }

    private KeyStroke makeShortcut(int key) {
        return KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }
}
