package ag.ion.noa.test;

import ag.ion.bion.officelayer.application.ILazyApplicationInfo;
import ag.ion.bion.officelayer.application.IOfficeApplication;
import ag.ion.bion.officelayer.application.OfficeApplicationException;
import ag.ion.bion.officelayer.application.OfficeApplicationRuntime;
import static ag.ion.bion.officelayer.application.OfficeApplicationRuntime.getApplicationAssistant;
import java.util.HashMap;
import junit.framework.TestCase;

/**
 * Test case for the OpenOffice.org Bean.
 *
 * @author Andreas Bröker
 * @version $Revision: 10398 $
 */
public class RetrieveOfficePathTest extends TestCase {

    public void testSearchApplicationPath() throws OfficeApplicationException {
            test(null);
    }
    
    public void test(String officeHome) throws OfficeApplicationException {
        System.out.println("NOA Office Bean Test");

        try {
            System.out.println("Activating OpenOffice.org connection ...");
            ILazyApplicationInfo appInfo = getApplicationAssistant().getLatestLocalLibreOfficeApplication();
            
            System.out.println("Found "+appInfo.getWriterExecutable());
            
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            fail(throwable.getMessage());
        }
        System.out.println("NOA Office Bean Test successfully.");
    }
    //----------------------------------------------------------------------------

}
