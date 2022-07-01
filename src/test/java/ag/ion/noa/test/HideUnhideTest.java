/**
 * **************************************************************************
 *                                                                          *
 * NOA (Nice Office Access) *
 * ------------------------------------------------------------------------ * *
 * The Contents of this file are made available subject to * the terms of GNU
 * Lesser General Public License Version 2.1. * * GNU Lesser General Public
 * License Version 2.1 *
 * ======================================================================== *
 * Copyright 2003-2006 by IOn AG * * This library is free software; you can
 * redistribute it and/or * modify it under the terms of the GNU Lesser General
 * Public * License version 2.1, as published by the Free Software Foundation. *
 * * This library is distributed in the hope that it will be useful, * but
 * WITHOUT ANY WARRANTY; without even the implied warranty of * MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU * Lesser General Public
 * License for more details. * * You should have received a copy of the GNU
 * Lesser General Public * License along with this library; if not, write to the
 * Free Software * Foundation, Inc., 59 Temple Place, Suite 330, Boston, * MA
 * 02111-1307 USA * * Contact us: * http://www.ion.ag * http://ubion.ion.ag *
 * info@ion.ag * *
 * **************************************************************************
 */
/*
 * Last changes made by $Author: andreas $, $Date: 2006-10-04 14:14:28 +0200 (Mi, 04 Okt 2006) $
 */
package ag.ion.noa.test;

import ag.ion.bion.officelayer.application.IApplicationInfo;
import ag.ion.bion.officelayer.application.IOfficeApplication;
import ag.ion.bion.officelayer.application.OfficeApplicationException;
import ag.ion.bion.officelayer.application.OfficeApplicationRuntime;

import ag.ion.bion.officelayer.desktop.IFrame;

import ag.ion.bion.officelayer.document.DocumentDescriptor;
import ag.ion.bion.officelayer.document.IDocument;
import ag.ion.bion.officelayer.document.IDocumentService;
import ag.ion.bion.officelayer.internal.application.ApplicationAssistant;
import com.zparkingb.utils.ZApplicationFolder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import java.util.HashMap;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;

import junit.framework.TestCase;

/**
 * Test case for the OpenOffice.org Bean.
 *
 * @author Andreas Bröker
 * @version $Revision: 10398 $
 */
public class HideUnhideTest extends TestCase {

    private static Logger LOGGER = Logger.getLogger("ag.ion");

    private static final File TESTROOT = ZApplicationFolder.getApplicationPath(ApplicationAssistant.class).toFile();

    private IDocument document = null;

    //----------------------------------------------------------------------------
    /**
     * Main entry point for the OpenOffice.org Bean Test.
     *
     * @param args arguments of the test
     *
     * @author Andreas Bröker
     * @date 21.05.2006
     */
    public static void main(String[] args) throws OfficeApplicationException {

        LogManager.getLogManager().reset();
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        LOGGER.addHandler(consoleHandler);
        LOGGER.setLevel(Level.FINEST);

        System.out.println("--We are running in--");
        System.out.println(TESTROOT);

        try {
            FileHandler fileHandler = new FileHandler(TESTROOT + File.separator + "log.xml");
            fileHandler.setLevel(Level.FINEST);
            LOGGER.addHandler(fileHandler);
        } catch (Throwable throwable) {
        }
        HideUnhideTest testOfficeBean = new HideUnhideTest();

        if (args.length == 0) {
            testOfficeBean.test(null);
        }
        else {
            testOfficeBean.test(args[0]);
        }
    }

    //----------------------------------------------------------------------------
    /**
     * Test the OpenOffice.org Bean by creating an empty odt file, opening it,
     * creating a pdf and cleanup
     *
     * @param officeHome home path to OpenOffice.org
     *
     * @author Andreas Bröker
     * @date 21.05.2006
     */
    public void test(String officeHome) throws OfficeApplicationException {
        System.out.println("NOA Office Bean Test");

        try {
            System.out.println("Activating OpenOffice.org connection ...");
            IOfficeApplication application;
            if (officeHome != null) {
                HashMap hashMap = new HashMap(2);
                hashMap.put(IOfficeApplication.APPLICATION_TYPE_KEY, IOfficeApplication.LOCAL_APPLICATION);
                hashMap.put(IOfficeApplication.APPLICATION_HOME_KEY, officeHome);
                application = OfficeApplicationRuntime.getApplication(hashMap);
            }
            else {
                application = OfficeApplicationRuntime.getApplication();
            }
            application.activate();
            IApplicationInfo info = application.getApplicationInfo();
            String which = info.getOfficeHome();
            System.out.println("Using " + which);
            final Frame frame = new Frame();
            frame.setVisible(true);
            frame.setSize(800, 800);
            frame.validate();
            JPanel panelMain =new JPanel();
            panelMain.setLayout(new BoxLayout(panelMain,BoxLayout.PAGE_AXIS));
            frame.add(panelMain);
            
            JTabbedPane tabbedPaneUp = new JTabbedPane();
            JTabbedPane tabbedPaneDown = new JTabbedPane();
            panelMain.add(tabbedPaneUp);
            panelMain.add(tabbedPaneDown);
            tabbedPaneUp.setVisible(true);
            tabbedPaneDown.setVisible(true);
            Panel panelOO = new Panel(new BorderLayout());
            tabbedPaneUp.addTab("OpenOffice",panelOO);
            
            
            JToggleButton btnSwitch = new JToggleButton("Switch");
            tabbedPaneUp.addTab("Action",btnSwitch);
            
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    frame.dispose();
                    document.close();
                    try {
                        System.out.println("Deactivating OpenOffice.org connection ...");
                        application.deactivate();
                    } catch (OfficeApplicationException applicationException) {
                    }
                    System.exit(0);
                }
            });

            System.out.println("Creating frame ...");
            IFrame officeFrame = application.getDesktopService().constructNewOfficeFrame(panelOO);

            System.out.println("Loading document for test ...");

            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("Test.odt").getFile());
//            String absolutePath = file.getAbsolutePath();
            IDocumentService documentService = OfficeApplicationRuntime.getApplication().getDocumentService();
            DocumentDescriptor descriptor = DocumentDescriptor.DEFAULT;
            documentService.loadDocument(officeFrame, file.toString(), descriptor);

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail(throwable.getMessage());
        }
        System.out.println("NOA Office Bean Test successfully.");
    }
    //----------------------------------------------------------------------------

}
