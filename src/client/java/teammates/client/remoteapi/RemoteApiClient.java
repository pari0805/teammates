package teammates.client.remoteapi;

import java.io.IOException;
import java.util.logging.Level;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;

import teammates.client.util.ClientProperties;
import teammates.common.util.Logger;
import teammates.storage.api.OfyHelper;

/**
 * Enables access to any Datastore (local/production).
 *
 * @see <a href="https://cloud.google.com/appengine/docs/standard/java/tools/remoteapi">https://cloud.google.com/appengine/docs/standard/java/tools/remoteapi</a>
 */
public abstract class RemoteApiClient {

    protected Objectify ofy() {
        return ObjectifyService.ofy();
    }

    protected void doOperationRemotely() throws IOException {



        String appUrl = ClientProperties.TARGET_URL.replaceAll("^https?://", "");
        String appDomain = appUrl.split(":")[0];
        int appPort = appUrl.contains(":") ? Integer.parseInt(appUrl.split(":")[1]) : 443;

        Logger logger = Logger.getLogger(RemoteApiClient.class.getName());
        logger.log(Level.INFO, "--- Starting remote operation ---");
        logger.log(Level.INFO,"Going to connect to:" + appDomain + ":" + appPort);

        RemoteApiOptions options = new RemoteApiOptions().server(appDomain, appPort);

        if (ClientProperties.isTargetUrlDevServer()) {
            // Dev Server doesn't require credential.
            options.useDevelopmentServerCredential();
        } else {
            /* Your Google Cloud SDK needs to be authenticated for Application Default Credentials
            in order to run any script in production server.
            Refer to https://developers.google.com/identity/protocols/application-default-credentials.*/
            options.useApplicationDefaultCredential();
        }

        RemoteApiInstaller installer = new RemoteApiInstaller();
        installer.install(options);

        OfyHelper.registerEntityClasses();
        Closeable objectifySession = ObjectifyService.begin();


        try {
            doOperation();
        } catch(Exception e){
            e.printStackTrace();
        } finally {
            try{
            objectifySession.close();
            installer.uninstall();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("--- Remote operation completed ---");
    }

    /**
     * This operation is meant to be overridden by child classes.
     */
    protected abstract void doOperation();
}
