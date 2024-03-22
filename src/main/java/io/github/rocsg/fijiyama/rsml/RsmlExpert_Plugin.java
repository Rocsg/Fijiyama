/*
 *
 */
package io.github.rocsg.fijiyama.rsml;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.Memory;
import ij.plugin.RGBStackMerge;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import io.github.rocsg.fijiyama.common.Timer;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import org.apache.commons.io.FileUtils;
import org.scijava.vecmath.Point3d;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * The Class RsmlExpert_Plugin.
 */
public class RsmlExpert_Plugin extends PlugInFrame implements KeyListener, ActionListener {


    /**
     * The Constant serialVersionUID.
     */
    /* Internal variables ******************************************************************************/
    private static final long serialVersionUID = 1L;
    /**
     * The Constant OK.
     */
    private static final int OK = 1;
    /**
     * The Constant UNDO.
     */
    private static final int UNDO = 2;
    /**
     * The Constant MOVE.
     */
    private static final int MOVE = 3;
    /**
     * The Constant REMOVE.
     */
    private static final int REMOVE = 4;
    /**
     * The Constant ADD.
     */
    private static final int ADD = 5;
    /**
     * The Constant SWITCH.
     */
    private static final int SWITCH = 6;
    /**
     * The Constant CREATE.
     */
    private static final int CREATE = 7;
    /**
     * The Constant EXTEND.
     */
    private static final int EXTEND = 8;
    /**
     * The Constant INFO.
     */
    private static final int INFO = 9;
    /**
     * The Constant CHANGE.
     */
    private static final int CHANGE = 10;
    /**
     * The Constant INFO.
     */
    private static final int SAVE = 11;
    /**
     * The Constant CHANGE.
     */
    private static final int RESAMPLE = 12;
    /**
     * The Constant BACKEXTEND.
     */
    private static final int BACKEXTEND = 13;
    /**
     * The Constant all.
     */
    private static final int[] all = new int[]{OK, UNDO, MOVE, REMOVE, ADD, SWITCH, CREATE, EXTEND, INFO, CHANGE,
            SAVE, RESAMPLE, BACKEXTEND};
    /**
     * The button ok.
     */
    private final JButton buttonOk = new JButton("OK");

    /**
     * The button undo.
     */
    private final JButton buttonUndo = new JButton("Undo last action");

    /**
     * The button move.
     */
    private final JButton buttonMove = new JButton("Move a point");

    /**
     * The button remove.
     */
    private final JButton buttonRemove = new JButton("Remove a point");

    /**
     * The button add.
     */
    private final JButton buttonAdd = new JButton("Add a middle point");

    /**
     * The button switch.
     */
    private final JButton buttonSwitch = new JButton("Switch a false cross");

    /**
     * The button create.
     */
    private final JButton buttonCreate = new JButton("Create a new branch");

    /**
     * The button extend.
     */
    private final JButton buttonExtend = new JButton("Extend a branch");

    /**
     * The button extend.
     */
    private final JButton buttonBackExtend = new JButton("Backwards extension");

    /**
     * The button info.
     */
    private final JButton buttonInfo = new JButton("Information about a node and a branch");

    /**
     * The button change.
     */
    private final JButton buttonChange = new JButton("Change time");

    /**
     * The button info.
     */
    private final JButton buttonSave = new JButton("Save RSML");

    /**
     * The button change.
     */
    private final JButton buttonResample = new JButton("Time-resampling");
    /**
     * The log area.
     */
    private final JTextArea logArea = new JTextArea("", 11, 10);
    /**
     * The zoom factor.
     */
    private final int zoomFactor = 2;

    private final int nMaxModifs = 500;
    /**
     * The user precision on click.
     */
    private final double USER_PRECISION_ON_CLICK = 20;
    /**
     * The data dir.
     */
    private String dataDir;
    /**
     * The registered stack.
     */
    private ImagePlus registeredStack = null;
    /**
     * The current image.
     */
    private ImagePlus currentImage = null;
    /**
     * The current model.
     */
    private RootModel currentModel = null;
    /**
     * The Nt.
     */
    private int Nt;
    /**
     * The number of modifications that have been made
     */
    private int nModifs = 0;
    /**
     * The tab of modifications did
     */
    private String[][] tabModifs = null;
    /**
     * The img init size.
     */
    private ImagePlus imgInitSize;
    /**
     * The sr.
     */
    private FSR sr;
    /**
     * The tab reg.
     */
    private ImagePlus[] tabReg;
    /**
     * The tab res.
     */
    private ImagePlus[] tabRes;
    /**
     * The t.
     */
    private Timer t;
    /**
     * The frame.
     */
    private JFrame frame;
    /**
     * The screen width.
     */
    private int screenWidth;
    /**
     * The buttons panel.
     */
    private JPanel buttonsPanel;
    /**
     * The panel global.
     */
    private JPanel panelGlobal;
    /**
     * The ok clicked.
     */
    private boolean okClicked;
    /**
     * The count.
     */
    private int count = 0;


    private String stackPath;

    private String rsmlPath;

    private String version = "v1.6.0 patch Cici";


    /**
     * Instantiates a new rsml expert plugin.
     */
    public RsmlExpert_Plugin() {
        super("");
    }

    /**
     * Instantiates a new ${e.g(1).rsfl()}.
     *
     * @param arg the arg
     */
    public RsmlExpert_Plugin(String arg) {
        super(arg);
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    /* Plugin entry points for test/debug or run in production ******************************************************************/
    public static void main(String[] args) {
        final ImageJ ij = new ImageJ();

        String testDir = "/home/rfernandez/Bureau/A_Test/RootSystemTracker/TestSplit/Processing_of_TEST1230403-SR-split/230403SR056";
        RsmlExpert_Plugin plugin = new RsmlExpert_Plugin();
        plugin.run(testDir);//testDir);
    }

    public String getBoxName() {
        String[] tab;
        if (!this.stackPath.contains("\\")) {
            tab = this.stackPath.split("/");
        } else {
            tab = this.stackPath.split("\\\\");
        }
        return tab[tab.length - 2];
    }

    /**
     * Run.
     *
     * @param arg the arg
     */
    public void run(String arg) {
        this.startPlugin(arg);
    }


    /**
     * Start plugin.
     *
     * @param arg the directory containing the processing files of a box
     */
    /* Setup of plugin and GUI ************************************************************************************/
    public void startPlugin(String arg) {
        t = new Timer();

        //Choose an existing expertize, or initiate a new one
        if (arg != null && arg.length() > 0 && new File(arg).exists()) dataDir = arg;
        else dataDir = VitiDialogs.chooseDirectoryUI("Choose a boite directory", "Ok");
        if (!new File(dataDir, "InfoRSMLExpert.csv").exists()) startNewExpertize();
        readInfoFile();
        t.mark();

        //Choose an existing expertize, or initiate a new one
        setupImageAndRsml();
        addLog(t.gather("Setup image and rsml took : "), 0);
        t.mark();

        startGui();
        welcomeAndInformAboutComputerCapabilities();
    }

    /**
     * Function to read the InfoRSMLExpert.csv file and get the stack and rsml paths
     */
    public void readInfoFile() {
        String[][] tab = VitimageUtils.readStringTabFromCsv(new File(dataDir, "InfoRSMLExpert.csv").getAbsolutePath());
        this.tabModifs = new String[500][nMaxModifs];
        for (int i = 0; i < tabModifs.length; i++) for (int j = 0; j < tabModifs[i].length; j++) tabModifs[i][j] = "";
        for (int i = 0; i < tab.length; i++) System.arraycopy(tab[i], 0, tabModifs[i], 0, tab[i].length);
        this.stackPath = tabModifs[0][0];
        this.rsmlPath = tabModifs[0][1];
        this.version = tabModifs[0][2];
    }

    public void writeInfoFile() {
        VitimageUtils.writeStringTabInCsv2(tabModifs, new File(dataDir, "InfoRSMLExpert.csv").getAbsolutePath());
    }

    public void startNewExpertize() {
        this.stackPath = new File(dataDir, "22_registered_stack.tif").getAbsolutePath().replace("\\", "/");
        this.rsmlPath = new File(dataDir, "61_graph.rsml").getAbsolutePath().replace("\\", "/");
        try {
            FileUtils.copyFile(new File(dataDir, "61_graph.rsml"), new File(dataDir, "61_graph_copy_before_expertize.rsml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        IJ.showMessage("Starting a new expertize of the box " + dataDir + "\n. Using 22_registered_stack.tif as image and 61_graph.rsml as arch. model to edit");
        IJ.showMessage("The rsml will be modified by expertize. But a copy of this have been made in case of, in 61_graph_copy_before_expertize.rsml");

        nModifs = 0;
        tabModifs = new String[500][nMaxModifs];
        for (int i = 0; i < tabModifs.length; i++) for (int j = 0; j < tabModifs[i].length; j++) tabModifs[i][j] = "";
        tabModifs[0][0] = this.stackPath;
        tabModifs[0][1] = this.rsmlPath;
        tabModifs[0][2] = this.version;
        writeInfoFile();
    }

    /**
     * This method initializes the Graphical User Interface (GUI) for the plugin.
     * It sets up the buttons and the frame for the log area.
     * It also logs the start of the Rsml Expert interface and sets the initial state of the buttons.
     * Additionally, it sets the tool in ImageJ to the hand tool and adds a key event dispatcher to handle key presses.
     */
    public void startGui() {
        // Set up the buttons and the button panel
        setupButtonsAndButtonPanel();

        // Set up the frame and the log area
        setupFrameAndLogArea();

        // Log the start of the Rsml Expert interface
        this.addLog("Starting Rsml Expert interface", 0);

        // Enable all buttons
        enable(all);

        // Disable the OK button
        disable(OK);

        // If there are no modifications, disable the UNDO button
        if (nModifs < 1) disable(UNDO);

        // Set the tool in ImageJ to the hand tool
        IJ.setTool("hand");

        // Add a key event dispatcher to handle key presses
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
                new KeyEventDispatcher() {
                    public boolean dispatchKeyEvent(KeyEvent e) {
                        // If the key event is a key press, handle the key press
                        if (e.getID() == KeyEvent.KEY_PRESSED) {
                            handleKeyPress(e);
                        }
                        return false;
                    }
                });
    }


    public void setupFrameAndLogArea() {
        this.screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        if (this.screenWidth > 1920) this.screenWidth /= 2;
        frame = new JFrame();
        JPanel consolePanel = new JPanel();
        consolePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        consolePanel.setLayout(new GridLayout(1, 1, 0, 0));
        logArea.setSize(300, 80);
        logArea.setBackground(new Color(10, 10, 10));
        logArea.setForeground(new Color(245, 255, 245));
        logArea.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        JScrollPane jscroll = new JScrollPane(logArea);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setEditable(false);


        frame = new JFrame();
        panelGlobal = new JPanel();
        frame.setSize(600, 680);
        panelGlobal.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelGlobal.setLayout(new BoxLayout(panelGlobal, BoxLayout.Y_AXIS));
        panelGlobal.add(new JSeparator());
        panelGlobal.add(jscroll);
        panelGlobal.add(new JSeparator());
        panelGlobal.add(buttonsPanel);
        panelGlobal.add(new JSeparator());
        frame.add(panelGlobal);
        frame.setTitle("RSML Expert");
        frame.pack();
        frame.setSize(600, 680);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                IJ.showMessage("See you next time !");
                frame.setVisible(false);
                closeAllViews();
            }
        });
        frame.setVisible(true);
        frame.repaint();
        VitimageUtils.adjustFrameOnScreen(frame, 2, 0);
        logArea.setVisible(true);
        logArea.repaint();

    }

    /**
     * Setup buttons.
     */
    public void setupButtonsAndButtonPanel() {
        buttonsPanel = new JPanel();
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        buttonsPanel.setLayout(new GridLayout(5, 3, 30, 30));
        buttonUndo.addActionListener(this);
        buttonUndo.setToolTipText("<html><p width=\"500\">" + "Undo last action" + "</p></html>");
        buttonOk.addActionListener(this);
        buttonOk.setToolTipText("<html><p width=\"500\">" + "Click here to validate current points" + "</p></html>");
        buttonMove.addActionListener(this);
        buttonMove.setToolTipText("<html><p width=\"500\">" + "Change the position of a point" + "</p></html>");
        buttonRemove.addActionListener(this);
        buttonRemove.setToolTipText("<html><p width=\"500\">" + "Remove a point" + "</p></html>");
        buttonAdd.addActionListener(this);
        buttonAdd.setToolTipText("<html><p width=\"500\">" + "Add a new point" + "</p></html>");
        buttonSwitch.addActionListener(this);
        buttonSwitch.setToolTipText("<html><p width=\"500\">" + "Switch two crossing branches" + "</p></html>");
        buttonExtend.addActionListener(this);
        buttonExtend.setToolTipText("<html><p width=\"500\">" + "Extend an existing branch" + "</p></html>");
        buttonBackExtend.addActionListener(this);
        buttonExtend.setToolTipText("<html><p width=\"500\">" + "Backwards extension of an existing branch" + "</p></html>");
        buttonCreate.addActionListener(this);
        buttonCreate.setToolTipText("<html><p width=\"500\">" + "Create a new branch" + "</p></html>");
        buttonInfo.addActionListener(this);
        buttonInfo.setToolTipText("<html><p width=\"500\">" + "Inform about a node and its root" + "</p></html>");
        buttonChange.addActionListener(this);
        buttonChange.setToolTipText("<html><p width=\"500\">" + "Change time of a node" + "</p></html>");
        buttonResample.addActionListener(this);
        buttonResample.setToolTipText("<html><p width=\"500\">" + "Resample with target timestep" + "</p></html>");
        buttonSave.addActionListener(this);
        buttonSave.setToolTipText("<html><p width=\"500\">" + "Save the current model" + "</p></html>");
        //L1
        buttonsPanel.add(buttonOk);
        buttonsPanel.add(new JLabel(""));
        buttonsPanel.add(buttonUndo);

        //L2
        buttonsPanel.add(buttonMove);
        buttonsPanel.add(buttonRemove);
        buttonsPanel.add(buttonAdd);

        //L3
        buttonsPanel.add(buttonCreate);
        buttonsPanel.add(buttonExtend);
        buttonsPanel.add(buttonBackExtend);

        //L4
        buttonsPanel.add(buttonChange);
        buttonsPanel.add(buttonResample);
        buttonsPanel.add(buttonSwitch);

        buttonsPanel.add(buttonInfo);
        buttonsPanel.add(new JLabel(""));
        buttonsPanel.add(buttonSave);

    }


    /**
     * Setup image and rsml.
     * <p>
     * This method sets up the image and RSML (Root System Markup Language) model for the plugin.
     * It loads the registered stack image and the RSML model from the specified paths.
     * It then applies any modifications that have been made to the model.
     * The method also resizes the stack and projects the RSML model onto the current image.
     * Finally, it logs the steps/hours and mean timestep information.
     */
    public void setupImageAndRsml() {
        // Load the registered stack image from the specified path
        registeredStack = IJ.openImage(new File(stackPath).getAbsolutePath());

        // Initialize the FSR (Flying Software Renderer)
        sr = new FSR();
        sr.initialize();

        // Load the RSML model from the specified path
        currentModel = RootModel.RootModelWildReadFromRsml(rsmlPath);

        // Clean the RSML model and resample the flying roots
        System.out.println(currentModel.cleanWildRsml());
        System.out.println(currentModel.resampleFlyingRoots());

        int j = 1;

        // Apply any modifications that have been made to the model
        while (!tabModifs[j][0].isEmpty()) {
            readLineAndExecuteActionOnModel(tabModifs[j], currentModel);
            j++;
            nModifs++;
        }

        // Resize the stack
        tabReg = VitimageUtils.stackToSlices(registeredStack);
        imgInitSize = tabReg[0].duplicate();
        Nt = tabReg.length;
        tabRes = new ImagePlus[Nt];
        for (int i = 0; i < tabReg.length; i++)
            tabReg[i] = VitimageUtils.resize(tabReg[i], tabReg[i].getWidth() * zoomFactor,
                    tabReg[i].getHeight() * zoomFactor, 1);

        // Project the RSML model onto the current image
        currentImage = projectRsmlOnImage(currentModel);
        currentImage.show();

        // Log the steps/hours and mean timestep information
        double[] tabHours = currentModel.hoursCorrespondingToTimePoints;
        String s = "Steps/hours : ";
        for (int i = 0; i < tabHours.length; i++) s += " | " + i + " -> " + VitimageUtils.dou(tabHours[i]);
        String s2 = "Mean timestep = " + VitimageUtils.dou(tabHours[tabHours.length - 1] / (tabHours.length - 1));
        IJ.log(s + "\n" + s2);
        addLog(s, -1);
        addLog(s2, -1);
    }











    /* Helpers of the Gui ************************************************************************************/


    /**
     * Handle key press.
     *
     * @param e the e
     */
    /* Callbacks  ********************************************************************************************/
    public void handleKeyPress(KeyEvent e) {
        final ExecutorService exec = Executors.newFixedThreadPool(1);
        exec.submit(new Runnable() {
            public void run() {
                if (e.getKeyChar() == 'q' && buttonOk.isEnabled()) {
                    disable(OK);
                    pointStart();
                    actionOkClicked();
                }
            }
        });
    }

    /**
     * Action performed.
     * This method is an event handler for various buttons in the application.
     * It checks which button was clicked and calls the corresponding method to handle the action.
     * Each action is run in a separate thread using an ExecutorService.
     *
     * @param e the ActionEvent object which contains information about the event
     */
    public void actionPerformed(ActionEvent e) {
        System.out.println("Got an event : " + e);

        final ExecutorService exec = Executors.newFixedThreadPool(1);
        exec.submit(new Runnable() {
            public void run() {
                if (e.getSource() == buttonOk && buttonOk.isEnabled()) {
                    disable(OK);
                    pointStart();
                    actionOkClicked();
                    return;
                }
                if (e.getSource() == buttonUndo && buttonUndo.isEnabled()) {
                    disable(UNDO);
                    pointStart();
                    actionUndo();
                    return;
                }
                if (e.getSource() == buttonMove && buttonMove.isEnabled()) {
                    disable(MOVE);
                    pointStart();
                    actionMovePoint();
                    return;
                }
                if (e.getSource() == buttonRemove && buttonRemove.isEnabled()) {
                    disable(REMOVE);
                    pointStart();
                    actionRemovePoint();
                    return;
                }
                if (e.getSource() == buttonAdd && buttonAdd.isEnabled()) {
                    disable(ADD);
                    pointStart();
                    actionAddMiddlePoints();
                    return;
                }
                if (e.getSource() == buttonSwitch && buttonSwitch.isEnabled()) {
                    disable(SWITCH);
                    pointStart();
                    actionSwitchPoint();
                    return;
                }
                if (e.getSource() == buttonCreate && buttonCreate.isEnabled()) {
                    disable(CREATE);
                    pointStart();
                    actionCreateBranch();
                    return;
                }
                if (e.getSource() == buttonExtend && buttonExtend.isEnabled()) {
                    disable(EXTEND);
                    pointStart();
                    actionExtendBranch();
                    return;
                }
                if (e.getSource() == buttonBackExtend && buttonBackExtend.isEnabled()) {
                    disable(BACKEXTEND);
                    pointStart();
                    actionBackExtendBranch();
                    return;
                }
                if (e.getSource() == buttonInfo && buttonInfo.isEnabled()) {
                    disable(INFO);
                    pointStart();
                    actionInfo();
                    return;
                }
                if (e.getSource() == buttonChange && buttonChange.isEnabled()) {
                    disable(CHANGE);
                    pointStart();
                    actionChangeTime();
                    return;
                }
                if (e.getSource() == buttonSave && buttonSave.isEnabled()) {
                    disable(SAVE);
                    actionSave();
                    return;
                }
                if (e.getSource() == buttonResample && buttonResample.isEnabled()) {
                    disable(RESAMPLE);
                    actionResample();
                }
            }
        });
    }

    /**
     * Action undo.
     * This method is responsible for undoing the last action performed by the user.
     * It resets the current image and the ROI manager, and then reverts the changes made to the current model.
     * The method also logs the action, disables all buttons during the operation, and re-enables the UNDO button if
     * there are still actions to undo.
     */
    public void actionUndo() {
        // Delete the current Region of Interest in the image
        currentImage.deleteRoi();

        // Reset and close the ROI manager
        RoiManager.getRoiManager().reset();
        RoiManager.getRoiManager().close();

        // Log the start of the undo action
        addLog("Running action \"Undo !\" ...", -1);

        // Disable all buttons during the operation
        disable(all);

        for (int i = 0; i < tabModifs[nModifs].length; i++) tabModifs[nModifs][i] = "";
        nModifs--;

        // Reload the model from the RSML file
        currentModel = RootModel.RootModelWildReadFromRsml(rsmlPath);

        // Reapply all modifications up to the current state
        int j = 1;
        while (!tabModifs[j][0].equals("")) {
            readLineAndExecuteActionOnModel(tabModifs[j], currentModel);
            j++;
        }

        // Clean and resample the model
        currentModel.cleanWildRsml();
        currentModel.resampleFlyingRoots();

        // Update the current image with the modified model
        VitimageUtils.actualizeData(projectRsmlOnImage(currentModel), currentImage);

        // Log the successful completion of the undo action
        addLog("Ok.", 2);

        // Finish the action and update the UI
        finishActionAborted();

        // Write the current state of the modifications to the info file
        writeInfoFile();

        // If there are no more modifications to undo, disable the UNDO button
        if (nModifs < 1) disable(UNDO);
    }

    /**
     * Action ok clicked.
     */
    public void actionOkClicked() {
        okClicked = true;
    }

    /**
     * Action move point.
     */
    public void actionMovePoint() {
        System.out.println("M1");
        boolean did = false;
        addLog("Running action \"Move a point\" ...", -1);
        addLog(" Click on the point to move, then the target destination.", 1);
        disable(all);
        System.out.println("M3");
        Point3d[] tabPt = getAndAdaptCurrentPoints(waitPoints(2));
        System.out.println("M4");
        String[] infos = null;
        if (tabPt != null) {
            System.out.println("M5, len=" + tabPt.length);
            infos = movePointInModel(tabPt, currentModel);
            System.out.println("M7");
            did = true;
        }
        System.out.println("M8");
        if (did) finishActionThenGoOnStepSaveActionAndUpdateImage(infos);
        else finishActionAborted();
        System.out.println("M9");
    }

    /**
     * Action remove point.
     */
    public void actionRemovePoint() {
        String[] infos = null;
        System.out.println("Rem0");
        addLog("Running action \"Remove point\" ...", -1);
        addLog(" Remove the point and all the children points of the root. Click on a point.", 1);
        System.out.println("Rem01");
        Point3d[] tabPt = getAndAdaptCurrentPoints(waitPoints(1));
        System.out.println("Rem02");

        boolean did = false;
        if (tabPt != null) {
            System.out.println("Rem2");
            infos = removePointInModel(tabPt, currentModel);
            if (infos != null) did = true;
        }
        System.out.println("Rem5");
        if (did) finishActionThenGoOnStepSaveActionAndUpdateImage(infos);
        else finishActionAborted();
        System.out.println("Rem7");

    }

    /**
     * Action add middle points.
     */
    public void actionAddMiddlePoints() {
        String[] infos = null;
        boolean did = false;
        addLog("Running action \"Add point\" ...", -1);
        addLog(" Add point. Click on a line, then click on the middle point to add.", 1);
        enable(OK);
        waitOkClicked();
        Point3d[] tabPt = getAndAdaptCurrentPoints((PointRoi) currentImage.getRoi());
        if (tabPt != null) {
            infos = addMiddlePointsInModel(tabPt, currentModel);
        }
        if (infos != null) did = true;
        if (did) finishActionThenGoOnStepSaveActionAndUpdateImage(infos);
        else finishActionAborted();
    }

    /**
     * Action switch point.
     */
    public void actionSwitchPoint() {
        boolean did = false;
        addLog("Running action \"Switch cross\" ...", -1);
        addLog(" Resolve a X cross. Click on the first point of Root A before cross, and first point of Root B before cross.", 1);
        String[] infos = null;
        Point3d[] tabPt = getAndAdaptCurrentPoints(waitPoints(2));
        if (tabPt != null) {
            infos = switchPointInModel(tabPt, currentModel);
            if (infos != null) did = true;
        }
        if (did) finishActionThenGoOnStepSaveActionAndUpdateImage(infos);
        else finishActionAborted();
    }

    /**
     * Action extend branch.
     */
    public void actionExtendBranch() {
        boolean did = false;
        addLog("Running action \"Extend branch\" ...", -1);
        addLog("Click on the extremity of a branch, then draw the line for each following observations.", 1);
        enable(OK);
        String[] infos = null;
        waitOkClicked();
        Point3d[] tabPt = getAndAdaptCurrentPoints((PointRoi) currentImage.getRoi());
        if (tabPt != null) infos = extendBranchInModel(tabPt, currentModel);
        if (infos != null) did = true;
        if (did) finishActionThenGoOnStepSaveActionAndUpdateImage(infos);
        else finishActionAborted();
    }


    /**
     * Action backextend branch.
     */
    public void actionBackExtendBranch() {
        boolean did = false;
        addLog("Running action \"Back Extend branch\" ...", -1);
        addLog("Click on the first point of a branch, then draw the line for each previous observations.", 1);
        enable(OK);
        String[] infos = null;
        waitOkClicked();
        Point3d[] tabPt = getAndAdaptCurrentPoints((PointRoi) currentImage.getRoi());
        if (tabPt != null) infos = backExtendBranchInModel(tabPt, currentModel);
        if (infos != null) did = true;
        if (did) finishActionThenGoOnStepSaveActionAndUpdateImage(infos);
        else finishActionAborted();
    }


    /**
     * Action create branch.
     */
    public void actionCreateBranch() {
        boolean did = false;
        addLog("Running action \"Create branch\" ...", -1);
        addLog("Click on the start point of the branch at the emergence time, then draw the line for each following observations.", 1);
        enable(OK);
        String[] infos = null;
        waitOkClicked();
        Point3d[] tabPt = getAndAdaptCurrentPoints((PointRoi) currentImage.getRoi());
        if (tabPt != null) {
            infos = createBranchInModel(tabPt, currentModel);
        }
        if (infos != null) did = true;
        if (did) finishActionThenGoOnStepSaveActionAndUpdateImage(infos);
        else finishActionAborted();

    }

    /**
     * Action info.
     */
    public void actionInfo() {
        System.out.println("I1");
        //boolean did=false;
        addLog("Running action \"Inform about a node and a root\" ...", -1);
        addLog(" Click on the node you want to inspect.", 1);
        disable(all);
        System.out.println("I3");
        Point3d[] tabPt = getAndAdaptCurrentPoints(waitPoints(1));
        System.out.println("I4");
        //String[]infos=null;
        if (tabPt != null) {
            System.out.println("I5, len=" + tabPt.length);
            informAboutPointInModel(tabPt, currentModel);
            System.out.println("I7");
            //did=true;
        }
        System.out.println("I8");
        finishActionAborted();
        System.out.println("I9");

    }

    /**
     * Action change time.
     */
    public void actionChangeTime() {
        System.out.println("I1");
        boolean did = false;
        addLog("Running action \"Change time of a node\" ...", -1);
        addLog(" Click on the node you want to change time.", 1);
        disable(all);
        System.out.println("I3");
        Point3d[] tabPt = getAndAdaptCurrentPoints(waitPoints(1));
        System.out.println("I4");
        String[] infos = null;
        if (tabPt != null) {
            System.out.println("I5, len=" + tabPt.length);
            infos = changeTimeInPointInModel(tabPt, currentModel);
            if (infos == null) did = false;
            System.out.println("I7");
        }
        System.out.println("I8");
        if (did) finishActionThenGoOnStepSaveActionAndUpdateImage(infos);
        else finishActionAborted();
    }

    /**
     * Action resample.
     */
    /**
     * Action save.
     */
    public void actionResample() {
        System.out.println("I1");
        boolean did = false;
        disable(all);
        double[] tabHours = currentModel.hoursCorrespondingToTimePoints;
        String s = "Steps/hours : ";
        for (int i = 0; i < tabHours.length; i++) s += " | " + i + " -> " + VitimageUtils.dou(tabHours[i]);
        String s2 = "Mean timestep = " + VitimageUtils.dou(tabHours[tabHours.length - 1] / (tabHours.length - 1));
        addLog("Running action \"Resample RSML\" ...", -1);
        addLog(s, -1);
        addLog(s2, -1);
        addLog(" Select the target timeStep (in hours).", 1);
        double timestep = VitiDialogs.getDoubleUI("Indicate the target timestep", "Timestep (in hours)", VitimageUtils.dou(tabHours[tabHours.length - 1] / (tabHours.length - 1)));
        addLog(" Select the output file name.", 1);
        addLog(" Suggested : 61_graph_expertized_resample_" + timestep + "hours.rsml", 1);
        System.out.println("I3");
        String path = VitiDialogs.saveImageUIPath("Path to your resampled rsml", "61_graph_expertized_resample_" + timestep + "hours.rsml");
        System.out.println("I4");
        String[] infos = null;
        infos = resampleModel(currentModel, timestep, path.replace("\\", "/"));
        if (infos == null) did = false;
        System.out.println("I8");
        if (did) finishActionThenGoOnStepSaveActionAndUpdateImage(infos);
        else finishActionAborted();
    }

    /**
     * Action save.
     */
    public void actionSave() {
        System.out.println("I1");
        boolean did = false;
        addLog("Running action \"Save current RSML\" ...", -1);
        addLog(" Select the output file name.", 1);
        addLog(" Suggested : 61_graph_expertized.rsml", 1);
        disable(all);
        IJ.showMessage("Your expertized RSML will be written in \n" + this.dataDir + "/61_graph_expertized.rsml");
        String path = this.dataDir + "/61_graph_expertized.rsml";
        String[] infos = null;
        infos = saveExpertizedModel(currentModel, path.replace("\\", "/"));
        if (infos == null) did = false;
        if (did) finishActionThenGoOnStepSaveActionAndUpdateImage(infos);
        else finishActionAborted();
    }


    /**
     * Move point in model.
     *
     * @param tabPt the tab pt
     */
    /* Corresponding operations on the model *******************************************************************/
    public String[] movePointInModel(Point3d[] tabPt, RootModel rm) {
        String[] infos = formatInfos("MOVEPOINT", tabPt);
        Object[] obj = rm.getClosestNode(tabPt[0]);
        Node n = (Node) obj[0];
        Root r = (Root) obj[1];
        System.out.println("Moving :\n --> Node " + n + "\n --> Of root " + r);
        n.x = (float) tabPt[1].x;
        n.y = (float) tabPt[1].y;
        r.updateTiming();
        return infos;
    }

    /**
     * Removes the point in model.
     *
     * @param tabPt the tab pt
     * @return true, if successful
     */
    public String[] removePointInModel(Point3d[] tabPt, RootModel rm) {
        String[] infos = formatInfos("REMOVEPOINT", tabPt);

        System.out.println("Rem21");
        Object[] obj = rm.getClosestNode(tabPt[0]);
        Node n1 = (Node) obj[0];
        Root r1 = (Root) obj[1];

        //Identify the first parent to stay
        System.out.println("Rem22");
        while (n1.parent != null && !n1.parent.hasExactBirthTime()) {
            n1 = n1.parent;
        }

        System.out.println("Removing :\n --> Node " + n1 + "\n --> Of root " + r1);
        //Case where we remove a part of a primary
        System.out.println("Rem23");
        if (r1.childList != null && r1.childList.size() > 1) {
            for (Root rChi : r1.childList) {
                Node n = rChi.getNodeOfParentJustAfterMyAttachment();
                if (n1.isParentOrEqual(n)) {
                    System.out.println("But first, removing :\n --> Node " + n1 + "\n --> Of root " + r1);
                    removePointInModel(new Point3d[]{new Point3d(rChi.firstNode.x, rChi.firstNode.y, rChi.firstNode.birthTime)}, rm);
                }
            }
        }

        System.out.println("Rem24");
        //Case where we remove a tip
        if (n1 != r1.firstNode) {
            r1.lastNode = n1.parent;
            r1.lastNode.child = null;
            r1.updateTiming();
            return infos;
        } else {//Removing a full root
            System.out.println("Rem25");
            if (r1.getParent() != null) {        //Case where we remove the first point of a lateral root
                ArrayList<Root> childs = r1.getParent().childList;
                ArrayList<Root> newList = new ArrayList<Root>();
                for (Root r : childs) if (r != r1) newList.add(r);
                r1.getParent().childList = newList;
                System.out.println("Removing from childlist " + r1);
            }
            //General case : a lateral or a primary
            System.out.println("Rem26");
            ArrayList<Root> newList = new ArrayList<Root>();
            System.out.println("Removing from rootList " + r1);
            for (Root r : rm.rootList) if (r != r1) newList.add(r);
            rm.rootList = newList;
            System.out.println("Rem27");
            return infos;
        }
    }

    /**
     * Adds the middle points in model.
     *
     * @param tabPts the tab pts
     * @return true, if successful
     */
    public String[] addMiddlePointsInModel(Point3d[] tabPts, RootModel rm) {
        String[] infos = formatInfos("ADDMIDDLE", tabPts);
        System.out.println("H1");
        if (tabPts == null || tabPts.length < 2) {
            IJ.showMessage("This action needs you to click on 1) the line to change and 2) the point to add. Abort");
            return null;
        }

        System.out.println("H2");
        Object[] obj = rm.getNearestRootSegment(tabPts[0], USER_PRECISION_ON_CLICK);
        if (obj[0] == null) {
            IJ.showMessage("Please click better, we have not found the corresponding segment");
            return null;
        }
        System.out.println("H3");
        Node nParent = (Node) obj[0];
        Root rParent = (Root) obj[1];
        Node nChild = nParent.child;
        System.out.println("Adding nodes in segment :\n --> Node 1 " + nParent + "\n --> Node 2 " + nChild + "\n --> Of root " + rParent);

        for (int i = 1; i < tabPts.length; i++) {
            Node nPlus = new Node((float) tabPts[i].x, (float) tabPts[i].y, nParent, true);
            nPlus.birthTime = 0.5f;
            nParent.child = nPlus;
            nPlus.parent = nParent;
            nParent = nPlus;
        }
        nParent.child = nChild;
        nChild.parent = nParent;
        rParent.updateTiming();
        return infos;
    }

    /**
     * Switch point in model.
     *
     * @param tabPt the tab pt
     * @return true, if successful
     */
    public String[] switchPointInModel(Point3d[] tabPt, RootModel rm) {
        String[] infos = formatInfos("SWITCHPOINT", tabPt);
        Object[] obj1 = rm.getClosestNode(tabPt[0]);
        Object[] obj2 = rm.getClosestNode(tabPt[1]);
        Node n1 = (Node) obj1[0];
        Root r1 = (Root) obj1[1];
        Node n2 = (Node) obj2[0];
        Root r2 = (Root) obj2[1];

        boolean isFeasible = !(n1.parent.birthTime >= n2.birthTime);
        if (n2.parent.birthTime >= n1.birthTime) isFeasible = false;
        if (n1.child.birthTime <= n2.birthTime) isFeasible = false;
        if (n2.child.birthTime <= n1.birthTime) isFeasible = false;
        System.out.println("Trying to switch :\n --> Node " + n1 + "\n and node n2 " + n2);

        if (!isFeasible) {
            IJ.showMessage("This switch is not possible");
            return null;
        }
        Node par1 = n1.parent;
        Node chi1 = n1.child;
        n1.parent = n2.parent;
        n1.child = n2.child;
        n2.parent = par1;
        n2.child = chi1;
        r1.resampleFlyingPoints(rm.hoursCorrespondingToTimePoints);
        r1.updateTiming();
        r2.resampleFlyingPoints(rm.hoursCorrespondingToTimePoints);
        r2.updateTiming();
        return infos;
    }

    /**
     * This method creates a new branch in the RootModel.
     * It first checks if there are at least two points provided for the branch.
     * Then it finds the closest node in the primary root to the first point.
     * It checks if the points are in the correct time order and if any time slices are missed.
     * If all checks pass, it creates a new Node for each point and links them together to form a branch.
     * The new branch is then added to the RootModel.
     *
     * @param tabPt an array of Point3d objects representing the points of the new branch
     * @param rm    the RootModel object to which the new branch will be added
     * @return an array of Strings containing information about the operation, or null if the operation was not
     * successful
     */
    public String[] createBranchInModel(Point3d[] tabPt, RootModel rm) {
        // Check if there are at least two points provided for the branch
        int N = tabPt.length;
        if (N < 2) return null;

        // Find the closest node in the primary root to the first point
        Object[] obj = rm.getClosestNodeInPrimary(tabPt[0]);
        Node n = (Node) obj[0];
        Root r = (Root) obj[1];
        System.out.println("Creating branch from :\n --> Node " + n + "\n --> Of root " + r);
        boolean[] extremity = new boolean[N];

        // Check if the points are in the correct time order
        boolean reverseTimeOrder = tabPt[1].z - tabPt[0].z < 0;
        if (reverseTimeOrder) {
            // If points are in reverse time order, reverse the array
            for (int i = 0; i < N / 2; i++) {
                Point3d temp = tabPt[i];
                tabPt[i] = tabPt[N - i - 1];
                tabPt[N - i - 1] = temp;
            }
        }

        // Check if any time slices are missed
        for (int i = 0; i < N - 1; i++) {
            double diff = tabPt[i + 1].z - tabPt[i].z;
            if (diff < 0 || diff > 1) return null;
            extremity[i] = diff == 1;
        }
        extremity[N - 1] = true;

        // Create a new Node for each point and link them together to form a branch
        n = new Node((float) tabPt[0].x, (float) tabPt[0].y, null, false);
        n.birthTime = (float) tabPt[0].z;
        Node firstNode = n;
        Node nPar = n;

        for (int incr = 1; incr < N; incr++) {
            Node nn = new Node((float) tabPt[incr].x, (float) tabPt[incr].y, nPar, true);
            nn.parent = nPar;
            nPar.child = nn;
            if (extremity[incr]) {
                nn.birthTime = (float) tabPt[incr].z;
                nn.birthTimeHours = (float) rm.hoursCorrespondingToTimePoints[(int) tabPt[incr].z];
            } else {
                nn.birthTime = (float) (tabPt[incr].z - 0.5);
                nn.birthTimeHours =
                        (float) (0.5 * rm.hoursCorrespondingToTimePoints[(int) tabPt[incr].z] + 0.5 * nPar.birthTime);
            }
            nPar = nn;
        }

        // Add the new branch to the RootModel
        Root rNew = new Root(null, rm, "", 2);
        rNew.firstNode = firstNode;
        rNew.lastNode = nPar;
        rNew.updateTiming();
        r.attachChild(rNew);
        rNew.attachParent(r);
        rm.rootList.add(rNew);

        // Return an array of Strings containing information about the operation
        return formatInfos("CREATEBRANCH", tabPt);
    }

    /**
     * Extend branch in model.
     *
     * @param tabPt the tab pt
     * @return true, if successful
     */
    public String[] extendBranchInModel(Point3d[] tabPt, RootModel rm) {
        String[] infos = formatInfos("EXTENDBRANCH", tabPt);
        if (tabPt.length < 2) return null;
        int N = tabPt.length;
        Object[] obj = rm.getClosestNode(tabPt[0]);
        Node n = (Node) obj[0];
        Root r = (Root) obj[1];
        System.out.println("Extending branch from :\n --> Node " + n + "\n --> Of root " + r);
        boolean[] extremity = new boolean[N];
        if (n != r.lastNode) {
            IJ.showMessage("Please select the last point of the branch you want to extend. Abort.");
            return null;
        }
        if (n.birthTime != tabPt[0].z) {
            IJ.showMessage("Please be wise : when selecting extremity, be on the right slice. Abort.");
            return null;
        }
        for (int i = 0; i < N - 1; i++) {
            if (tabPt[i + 1].z - tabPt[i].z < 0) {
                IJ.showMessage("You gave point in reverse time order. Abort.");
                return null;
            }
            if (tabPt[i + 1].z - tabPt[i].z > 1) {
                IJ.showMessage("You gave points that does not follow in time, you missed a slice. Abort.");
                return null;
            }
            if (tabPt[i + 1].z - tabPt[i].z == 1) {
                extremity[i] = true;
            }
        }
        extremity[N - 1] = true;

        int incr = 1;
        Node nPar = n;
        while (incr < N) {
            Node nn = new Node((float) tabPt[incr].x, (float) tabPt[incr].y, nPar, true);
            nn.parent = nPar;
            nPar.child = nn;
            if (extremity[incr]) {
                nn.birthTime = (float) tabPt[incr].z;
                nn.birthTimeHours = (float) rm.hoursCorrespondingToTimePoints[(int) tabPt[incr].z];
            } else {
                nn.birthTime = (float) (tabPt[incr].z - 0.5);
                nn.birthTimeHours = (float) (0.5 * rm.hoursCorrespondingToTimePoints[(int) tabPt[incr].z] + 0.5 * nPar.birthTime);
            }
            System.out.println("\nJust added the node " + nn + " \n  with parent=" + nPar);
            incr++;
            nPar = nn;
        }
        r.lastNode = nPar;
        r.updateTiming();
        return infos;
    }


    /**
     * Extend branch in model.
     *
     * @param tabPt the tab pt
     * @return true, if successful
     */
    public String[] backExtendBranchInModel(Point3d[] tabPt, RootModel rm) {
        String[] infos = formatInfos("BACKEXTENDBRANCH", tabPt);
        if (tabPt.length < 2) return null;
        int N = tabPt.length;
        Object[] obj = rm.getClosestNode(tabPt[0]);
        Node n = (Node) obj[0];
        Root r = (Root) obj[1];
        System.out.println("Back extending branch from :\n --> Node " + n + "\n --> Of root " + r);
        boolean[] extremity = new boolean[N];
        if (n != r.firstNode) {
            IJ.showMessage("Please select the first point of the branch you want to extend. Abort.");
            return null;
        }
        for (int i = 0; i < N; i++) {
            if (tabPt[i].z != 1) {
                IJ.showMessage("Please only select points anterior in the first slice (anterior to dynamic imaging)");
                return null;
            }
        }


        Node nFirst = new Node((float) tabPt[N - 1].x, (float) tabPt[N - 1].y, null, true);
        nFirst.birthTime = (float) 0;
        nFirst.birthTimeHours = (float) rm.hoursCorrespondingToTimePoints[0];
        Node nPar = nFirst;
        extremity[N - 1] = true;


        int decr = N - 2;
        while (decr > 0) {
            Node nn = new Node((float) tabPt[decr].x, (float) tabPt[decr].y, nPar, true);
            nn.parent = nPar;
            nPar.child = nn;
            //if(extremity[decr]) {
            nn.birthTime = (float) 0;
            nn.birthTimeHours = (float) ((float) rm.hoursCorrespondingToTimePoints[0]);
			/*}
			else {
				nn.birthTime=(float) (tabPt[decr].z);
				nn.birthTimeHours=(float) (0.5*rm.hoursCorrespondingToTimePoints[(int) tabPt[decr].z]+0.5*nPar.birthTime);
			}*/
            System.out.println("\nJust added the node " + nn + " \n  with parent=" + nPar);
            decr--;
            nPar = nn;
        }

        nPar.child = r.firstNode;
        r.firstNode.parent = nPar;
        r.firstNode = nFirst;
        r.updateTiming();
        return infos;
    }


    /**
     * Inform about point in model.
     *
     * @param tabPt the tab pt
     */
    public void informAboutPointInModel(Point3d[] tabPt, RootModel rm) {
        Object[] obj = rm.getClosestNode(tabPt[0]);
        Node n = (Node) obj[0];
        Root r = (Root) obj[1];
        IJ.showMessage("Informations at coordinates " + tabPt[0] + " :\n --> Node " + n + "\n --> Of root " + r);
    }

    /**
     * Change time in point in model.
     *
     * @param tabPt the tab pt
     * @return true, if successful
     */
    public String[] changeTimeInPointInModel(Point3d[] tabPt, RootModel rm) {
        String[] infos = formatInfos("CHANGETIME", tabPt);
        Object[] obj = rm.getClosestNode(tabPt[0]);
        Node n = (Node) obj[0];
        Root r = (Root) obj[1];
        if (n == null) return null;
        double tt = VitiDialogs.getDoubleUI("New time", "time", n.birthTime);
        n.birthTime = (float) tt;
        r.resampleFlyingPoints(rm.hoursCorrespondingToTimePoints);
        r.updateTiming();
        return infos;
    }


    /**
     * Save expertized model
     *
     * @param rm   the RootModl
     * @param path the path to save the final exported model, as a rsml file
     * @return true, if successful
     */
    public String[] saveExpertizedModel(RootModel rm, String path) {
        String[] infos = formatInfos("SAVE_" + new File(path).getName(), new Point3d[]{new Point3d(0, 0, 0)});
        rm.writeRSML3D(new File(path).getAbsolutePath().replace("\\", "/"), "", true, false);
        return infos;
    }

    /**
     * Change time in point in model.
     * <p>
     * param tabPt the tab pt
     *
     * @return true, if successful
     * <p>
     * OLD : public String[] resampleModel(RootModel rm, double timestep, String path) {
     * IJ.log("Called action resampleModel");
     * String[] infos = formatInfos("RESAMPLE_" + new File(path).getName(), new Point3d[]{new Point3d(timestep, 0, 0)});
     * RootModel rm2 = RootModel.RootModelWildReadFromRsml(rsmlPath);
     * //ystem.out.println(rm2.cleanWildRsml()) ;
     * //System.out.println(rm2.resampleFlyingRoots());
     * int j = 1;
     * while (!tabModifs[j][0].equals("")) {
     * readLineAndExecuteActionOnModel(tabModifs[j], rm2);
     * j++;
     * }
     * IJ.log("Starting callback");
     * RootModel romod = resampleRootModelToTargetTimestep(rm2, timestep);
     * romod.writeRSML3D(new File(path).getAbsolutePath().replace("\\", "/"), "", true, false);
     * <p>
     * return infos;
     * }
     */
    public String[] resampleModel(RootModel rm, double timestep, String path) {
        String[] infos = formatInfos("RESAMPLE_" + new File(path).getName(), new Point3d[]{new Point3d(timestep, 0,
                0)});
        RootModel rm2 = RootModel.RootModelWildReadFromRsml(rsmlPath);

        IntStream.range(1, tabModifs.length)
                .parallel()
                .filter(j -> !tabModifs[j][0].isEmpty())
                .forEach(j -> readLineAndExecuteActionOnModel(tabModifs[j], rm2));

        RootModel romod = resampleRootModelToTargetTimestep(rm2, timestep);
        romod.writeRSML3D(path.replace("\\", "/"), "", true, false);

        return infos;
    }


    public RootModel resampleRootModelToTargetTimestep(RootModel rm, double timestep) {
        //Preparing variables
        boolean debug = true;
        if (debug) IJ.log("Starting resampling");
        double[] tabHours = Arrays.copyOf(rm.hoursCorrespondingToTimePoints, rm.hoursCorrespondingToTimePoints.length);
        int NimgInit = this.registeredStack.getStackSize();
        double hourMax = tabHours[tabHours.length - 1];
        int N = (int) Math.floor(hourMax / timestep) + 1;
        double[] hours = new double[N];
        int[] correspondingImage = new int[N];
        if (debug) IJ.log("variables ok. N=" + N);

        //Identifying for each timepoint the corresponding image in the original stack, and the actual time since experiment start
        for (int i = 0; i < N; i++) {
            hours[i] = i * timestep;
            int index = NimgInit - 1;
            for (int j = NimgInit; j > 0; j--) {
                if (tabHours[j] > hours[i]) index = j - 1;
            }
            correspondingImage[i] = index;
            IJ.log("Timestep " + i + " = " + hours[i] + " identified keyimage=" + index);
        }


        //Change the timebasis and use flyingRootsMethod to create new nodes along with the timepoints
        if (debug) IJ.log("Starting step 0");

        if (debug) IJ.log("Starting step 1");
        rm.changeTimeBasis(timestep, N);

        if (debug) IJ.log("Starting step 2");
        rm.resampleFlyingRoots();

        //Remove all the timepoints not falling onto actual timepoints
        if (debug) IJ.log("Starting step 3");
        rm.removeInterpolatedNodes();
        if (debug) IJ.log("All steps ok.");


        Timer t = new Timer();
        ImagePlus[] tabImg = new ImagePlus[N];
        for (int i = 0; i < N; i++) {
            t.print("Projecting " + i + " / " + N);
            ImagePlus imgRSML = rm.createGrayScaleImageWithHours(imgInitSize, zoomFactor, false, hours[i], true, new boolean[]{true, true, true, false, true}, new double[]{2, 2});
            imgRSML.setDisplayRange(-timestep, hourMax);
            tabImg[i] = RGBStackMerge.mergeChannels(new ImagePlus[]{tabReg[correspondingImage[i]], imgRSML}, true);
            IJ.run(tabImg[i], "RGB Color", "");
        }
        t.print("Projecting resampled root model took : ");
        ImagePlus res = VitimageUtils.slicesToStack(tabImg);
        res.setTitle("Model_resampled_timestep_" + timestep + "hours");
        res.show();
        return rm;
    }


    public String[] formatInfos(String action, Point3d[] tabPt) {
        int nbPoints = tabPt.length;
        String[] out = new String[2 + 4 * nbPoints];
        out[0] = action;
        out[1] = "" + nbPoints;
        for (int i = 0; i < nbPoints; i++) {
            out[2 + i * 4] = ("Pt_" + i);
            out[2 + i * 4 + 1] = ("" + VitimageUtils.dou(tabPt[i].x));
            out[2 + i * 4 + 2] = ("" + VitimageUtils.dou(tabPt[i].y));
            out[2 + i * 4 + 3] = ("" + VitimageUtils.dou(tabPt[i].z));
        }
        return out;
    }

    public void readLineAndExecuteActionOnModel(String[] line, RootModel rm) {
        int nPoints = Integer.parseInt(line[1]);
        Point3d[] tabPt = new Point3d[nPoints];
        for (int i = 0; i < nPoints; i++) {
            tabPt[i] = new Point3d(Double.parseDouble(line[2 + i * 4 + 1]), Double.parseDouble(line[2 + i * 4 + 2]), Double.parseDouble(line[2 + i * 4 + 3]));
        }
        String action = line[0];

        switch (action) {
            case "MOVEPOINT":
                movePointInModel(tabPt, rm);
                break; // TODO
            case "REMOVEPOINT":
                removePointInModel(tabPt, rm);
                break;// TODO
            case "ADDMIDDLE":
                addMiddlePointsInModel(tabPt, rm);
                break;// TODO
            case "SWITCHPOINT":
                switchPointInModel(tabPt, rm);
                break;// TODO
            case "CREATEBRANCH":
                createBranchInModel(tabPt, rm);
                break;// TODO
            case "EXTENDBRANCH":
                extendBranchInModel(tabPt, rm);
                break;// TODO
            case "BACKEXTENDBRANCH":
                backExtendBranchInModel(tabPt, rm);
                break;// TODO
            case "CHANGETIME":
                changeTimeInPointInModel(tabPt, rm);
                break;// TODO
            default:
                // Handle the case where no matching action is found
                break;// TODO
        }
    }


    /**
     * Finish action aborted.
     */
    /* Helpers for starting and finishing actions *******************************************************************/
    public void finishActionAborted() {
        IJ.setTool("hand");
        addLog(" action aborted.", 2);
        enable(all);
        disable(OK);
    }

    /**
     * Finish action then go on step save rsml and update image.
     */
    public void finishActionThenGoOnStepSaveActionAndUpdateImage(String[] infos) {
        IJ.setTool("hand");
        addLog(" action ok.", 2);
        addLog("Updating image...", 0);
        nModifs++;
        System.arraycopy(infos, 0, tabModifs[nModifs], 0, infos.length);
        VitimageUtils.actualizeData(projectRsmlOnImage(currentModel), currentImage);
        addLog("Ok.", 2);
        enable(all);
        disable(OK);
        writeInfoFile();
    }

    /**
     * Save the model into a final RSML
     */
    public void saveRsmlModel() {
        IJ.setTool("hand");
        addLog("Saving RSML", 0);
        this.currentModel.writeRSML3D(new File(dataDir, "61_graph_expertized.rsml").getAbsolutePath().replace("\\", "/"), "", true, false);
        VitimageUtils.actualizeData(projectRsmlOnImage(currentModel), currentImage);
        addLog("Ok.", 2);
        enable(all);
        disable(OK);
    }


    /**
     * Point start.
     */
    public void pointStart() {
        disable(all);
        RoiManager rm = RoiManager.getRoiManager();
        rm.reset();
        IJ.setTool("multipoint");
    }

    /**
     * Gets the and adapt current points.
     *
     * @param pr the pr
     * @return the and adapt current points
     */
    public Point3d[] getAndAdaptCurrentPoints(PointRoi pr) {
        if (pr == null) {
            currentImage.deleteRoi();
            RoiManager.getRoiManager().reset();
            RoiManager.getRoiManager().close();
            return null;
        }
        Point[] tab2D = pr.getContainedPoints();
        Point3d[] tabPt = new Point3d[tab2D.length];
        for (int i = 0; i < tabPt.length; i++) {
            tabPt[i] = new Point3d(tab2D[i].x / zoomFactor, tab2D[i].y / zoomFactor, pr.getPointPosition(i));
            System.out.println("Processed point " + i + ": " + tabPt[i]);
        }
        currentImage.deleteRoi();
        RoiManager.getRoiManager().reset();
        RoiManager.getRoiManager().close();
        return tabPt;
    }

    /**
     * This method projects the Root System Markup Language (RSML) model onto an image.
     * It creates a grayscale image for each time point in the model, then merges this with the registered stack image.
     * The resulting images are combined into a stack and returned.
     *
     * @param rm the RootModel object which contains the RSML data
     * @return an ImagePlus object which is a stack of images with the RSML model projected onto them
     */
    public ImagePlus projectRsmlOnImage(RootModel rm) {
        // Start a timer to measure the execution time of this method
        Timer t = new Timer();

        // Loop over each time point in the model
        for (int i = 0; i < Nt; i++) {
            // Create a grayscale image of the RSML model at this time point
            ImagePlus imgRSML = rm.createGrayScaleImageWithTime(imgInitSize, zoomFactor, false, (i + 1), true,
                    new boolean[]{true, true, true, false, true}, new double[]{2, 2});

            // Set the display range of the image
            imgRSML.setDisplayRange(0, Nt + 3);

            // Merge the grayscale image with the registered stack image
            tabRes[i] = RGBStackMerge.mergeChannels(new ImagePlus[]{tabReg[i], imgRSML}, true);

            // Convert the image to RGB color
            IJ.run(tabRes[i], "RGB Color", "");
        }

        // Print the execution time of this method
        t.print("Updating root model took : ");

        // Combine the images into a stack
        ImagePlus res = VitimageUtils.slicesToStack(tabRes);

        // Get the name of the box
        String chain = getBoxName();

        // Create a name for the image
        String nom =
                "Model_of_box_" + chain + "_at_step_" + (nModifs < 1000 ? "0" : "") + (nModifs < 100 ? "0" : "") + (nModifs < 10 ? "0" : "") + nModifs;

        // Set the title of the image
        res.setTitle(nom);

        // Return the image
        return res;
    }


    /**
     * Wait ok clicked.
     */
    public void waitOkClicked() {
        while (!okClicked) {
            VitimageUtils.waitFor(100);
        }
        okClicked = false;
    }

    /**
     * Wait points.
     *
     * @param nbExpected the nb expected
     * @return the point roi
     */
    public PointRoi waitPoints(int nbExpected) {
        Roi r = null;
        PointRoi pr = null;
        while (count != nbExpected) {
            VitimageUtils.waitFor(100);
            r = currentImage.getRoi();
            if (r != null) {
                pr = ((PointRoi) r);
                count = pr.getContainedPoints().length;
            }
        }
        count = 0;
        return pr;
    }

    /**
     * Adds the log.
     *
     * @param t     the t
     * @param level the level
     */
    public void addLog(String t, int level) {
        if (level == -1) logArea.append("\n\n > " + t);
        if (level == 0) logArea.append("\n > " + t);
        if (level == 1) logArea.append("\n " + t);
        if (level == 2) logArea.append(" " + t);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * Key pressed.
     *
     * @param arg0 the arg 0
     */
    public void keyPressed(KeyEvent arg0) {
    }

    /**
     * Key released.
     *
     * @param arg0 the arg 0
     */
    public void keyReleased(KeyEvent arg0) {
    }

    /**
     * Key typed.
     *
     * @param arg0 the arg 0
     */
    public void keyTyped(KeyEvent arg0) {
    }

    /**
     * Close all views.
     */
    public void closeAllViews() {
        if (currentImage != null) currentImage.close();
        if (RoiManager.getInstance() != null) RoiManager.getInstance().close();
    }


    /**
     * Enable.
     *
     * @param but the but
     */
    public void enable(int but) {
        enable(new int[]{but});
    }

    /**
     * Disable.
     *
     * @param but the but
     */
    public void disable(int but) {
        disable(new int[]{but});
    }

    /**
     * Enable.
     *
     * @param tabBut the tab but
     */
    public void enable(int[] tabBut) {
        setState(tabBut, true);
    }

    /**
     * Disable.
     *
     * @param tabBut the tab but
     */
    public void disable(int[] tabBut) {
        setState(tabBut, false);
    }

    /**
     * Sets the state.
     *
     * @param tabBut the tab but
     * @param state  the state
     */
    public void setState(int[] tabBut, boolean state) {
        for (int but : tabBut) {
            switch (but) {
                case OK:
                    this.buttonOk.setEnabled(state);
                    break;
                case CREATE:
                    this.buttonCreate.setEnabled(state);
                    break;
                case EXTEND:
                    this.buttonExtend.setEnabled(state);
                    break;
                case BACKEXTEND:
                    this.buttonBackExtend.setEnabled(state);
                    break;
                case ADD:
                    this.buttonAdd.setEnabled(state);
                    break;
                case REMOVE:
                    this.buttonRemove.setEnabled(state);
                    break;
                case MOVE:
                    this.buttonMove.setEnabled(state);
                    break;
                case SWITCH:
                    this.buttonSwitch.setEnabled(state);
                    break;
                case UNDO:
                    this.buttonUndo.setEnabled(state);
                    break;
                case INFO:
                    this.buttonInfo.setEnabled(state);
                    break;
                case CHANGE:
                    this.buttonChange.setEnabled(state);
                    break;
                case SAVE:
                    this.buttonSave.setEnabled(state);
                    break;
                case RESAMPLE:
                    this.buttonResample.setEnabled(state);
                    break;
            }
        }
    }

    /**
     * Check computer capacity.
     *
     * @param verbose the verbose
     * @return the string[]
     */
    public String[] checkComputerCapacity(boolean verbose) {
        int nbCpu = Runtime.getRuntime().availableProcessors();
        int jvmMemory = (int) ((new Memory().maxMemory() / (1024 * 1024)));//Java virtual machine available memory (in Megabytes)
        long memoryFullSize = 0;
        String[] str = new String[]{"", ""};
        try {
            memoryFullSize = (((com.sun.management.OperatingSystemMXBean) ManagementFactory
                    .getOperatingSystemMXBean()).getTotalPhysicalMemorySize()) / (1024 * 1024);
        } catch (Exception e) {
            return str;
        }

        str[0] = "Welcome to RSML Expert ";
        str[1] = "System check. Available memory in JVM=" + jvmMemory + " MB over " + memoryFullSize + " MB. #Available processor cores=" + nbCpu + ".";
        if (verbose) return str;
        else return new String[]{"", ""};
    }

    /**
     * Welcome and inform about computer capabilities.
     */
    public void welcomeAndInformAboutComputerCapabilities() {
        String[] str = checkComputerCapacity(true);
        addLog(str[0], 0);
        addLog(str[1], 0);
    }

    /**
     * Gets the rsml name.
     *
     * @return the rsml name
     */
	/*public String getRsmlName() {
		return (  new File(modelDir,"4_2_Model_"+ (stepOfModel<1000 ? ("0") : "" ) + (stepOfModel<100 ? ("0") : "" ) + (stepOfModel<10 ? ("0") : "" ) + stepOfModel+".rsml" ).getAbsolutePath());
	}
*/
}
