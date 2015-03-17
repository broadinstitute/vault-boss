/**
 * Copyright 2015 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.genomebridge.boss;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class ObjectMaker implements Runnable {

    public static void main( String[] args ) {

        if ( args.length == 0 )
            usage();

        Properties props = new Properties();
        int idx0 = 0;
        try {
            if ( !args[0].equals("--conf") )
                props.load(System.in);
            else {
                if ( args.length == 1 )
                    usage();
                props.load(new FileReader(args[1]));
                idx0 = 2;
            }
        } catch ( IOException e ) {
            usage();
        }

        gBossURL = props.getProperty("host");
        gUser = props.getProperty("user");
        String password = props.getProperty("password");
        if ( gBossURL == null || gUser == null || password == null || !isURL(gBossURL) )
            usage();

        gACL = new String[1];
        gACL[0] = gUser;

        createMD5Digester(); // just to test that it can be done before we rock and roll

        ClientConfig config = new DefaultClientConfig();
        config.getClasses().add(JacksonJsonProvider.class);
        gBOSSClient = Client.create(config);
        gBOSSClient.addFilter(new HTTPBasicAuthFilter(gUser,password));
        //gOSClient = Client.create(config);

        int nProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(nProcessors);
        for ( int iii = idx0; iii != args.length; ++iii ) {
            pool.execute(new ObjectMaker(args[iii]));
        }
        pool.shutdown();
    }

    public ObjectMaker( String jobDir ) {
        mJobDir = jobDir;
    }

    @Override
    public void run() {

        // give it a whirl
        try {
            makeObject();
        } catch ( Throwable e ) {
            mErrMsg = e.getMessage();
            if ( mErrMsg == null )
                mErrMsg = "Caught exception of type "+e.getClass().getName();
        }

        // make certain we don't leave running sub-processes behind
        if ( mTarProc != null ) {
            cleanUpProcess();
        }

        if ( mErrMsg == null ) {
            System.out.println(mJobDir+": OK");
            System.out.println("\tMD5: "+DatatypeConverter.printHexBinary(mMD5)+" Len: "+mSize);
            mMD5 = null;
        }
        else {
            // clean up if things did not go well
            if ( mObjectURL != null ) {
                try { gBOSSClient.resource(mObjectURL).delete(); }
                catch ( Exception e ) {
                    mErrMsg += "  Object could not be removed: " + e.getMessage();
                }
                mObjectURL = null;
            }
            System.out.println(mJobDir+": "+mErrMsg);
            mErrMsg = null;
        }
    }

    private void makeObject() {

        // test for an appropriate jobDir
        File jobDir = new File(mJobDir).getAbsoluteFile();
        if ( !jobDir.isDirectory() ) {
            mErrMsg = "Jobdir is not a directory."; return;
        }
        if ( !jobDir.canRead() ) {
            mErrMsg = "Jobdir is not readable."; return;
        }

        // create object in BOSS
        File parentDir = jobDir.getParentFile();
        String objName = jobDir.getName();

        ObjectResource obj = new ObjectResource();
        obj.ownerId = gUser;
        obj.objectName = objName;
        obj.readers = gACL;
        obj.writers = gACL;
        obj.storagePlatform = OBJECTSTORE;

        ClientResponse response = gBOSSClient.resource(gBossURL)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.APPLICATION_JSON_TYPE)
                        .post(ClientResponse.class, obj);

        if ( response.getStatus() != ClientResponse.Status.CREATED.getStatusCode() ) {
            response.close(); mErrMsg = "Unable to create object."; return;
        }

        mObjectURL = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        response.close();
        if ( mObjectURL == null ) {
            mErrMsg = "Unable to find location of new object."; return;
        }

        // get length of tar file
        ProcessBuilder procBldr = new ProcessBuilder("tar","--atime-preserve","-czC",parentDir.toString(),jobDir.getName())
                        .redirectError(ProcessBuilder.Redirect.INHERIT);
        procBldr.environment().put("GZIP","-n");
        long tarLen = 0L;
        try {
            mTarProc = procBldr.start();
            DigestInputStream dis = new DigestInputStream(mTarProc.getInputStream(),createMD5Digester());
            byte[] buf = new byte[CHUNK_SIZE];
            int len;
            while ( (len = dis.read(buf)) != -1 )
                tarLen += len;
            mMD5 = dis.getMessageDigest().digest();
            mSize = tarLen;
            cleanUpProcess();
            if ( mErrMsg != null )
                return;
        } catch ( IOException e ) {
            mErrMsg = "Unable to calculate tar length--"+e.getMessage(); return;
        }

        // resolve new object to get writable URL for object's data
        String resolveURL = mObjectURL + "/resolve";
        ResolutionRequest req = new ResolutionRequest();
        req.httpMethod = HttpMethod.PUT;
        req.validityPeriodSeconds = VALIDITY_DURATION;
        req.contentType = MediaType.APPLICATION_OCTET_STREAM;
        response = gBOSSClient.resource(resolveURL)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.APPLICATION_JSON_TYPE)
                        .post(ClientResponse.class, req);

        if ( response.getStatus() != ClientResponse.Status.OK.getStatusCode() ) {
            response.close(); mErrMsg = "Unable to resolve object."; return;
        }

        // create tar process and stream that process's output to the upload URL
        URI uploadURI = response.getEntity(ResolutionResource.class).objectUrl;
        response.close();
        try {
            mTarProc = procBldr.start();
        } catch ( IOException e ) {
            mErrMsg = "Unable to stream tar--"+e.getMessage(); return;
        }
        DigestInputStream dis = new DigestInputStream(mTarProc.getInputStream(),createMD5Digester());
        uploadTar(tarLen,uploadURI,dis);
    }

    private void cleanUpProcess() {
        // ugly, ugly, ugly.  in Java 8 we'd just call Process.waitFor with a timeout.
        // give the process up to 31 seconds to exit
        int tarRC = -1;
        boolean ok = false;
        long timeout = 1000L;
        for ( int attempt = 0; !ok && attempt != 5; ++attempt ) {
            try {
                tarRC = mTarProc.exitValue();
                ok = true;
            }
            catch ( IllegalThreadStateException e ) {
                try {
                    Thread.sleep(timeout);
                }
                catch ( InterruptedException e1 ) {
                    // shouldn't happen, but doesn't much matter if it does
                }
                timeout = 2*timeout;
            }
        }
        try {
            tarRC = mTarProc.exitValue();
            ok = true;
        }
        catch ( IllegalThreadStateException e ) {
            // nothing to do
        }
        if ( !ok ) {
            mTarProc.destroy();
            mErrMsg = (mErrMsg==null?"":mErrMsg) + "  Tar process failed to terminate.";
            while ( true )
                try { tarRC = mTarProc.waitFor(); break; }
                catch ( InterruptedException e1 ) {}
        }
        if ( tarRC != 0 ) {
            mErrMsg = (mErrMsg==null?"":mErrMsg) + "  Tar process returned status "+tarRC+'.';
        }
        mTarProc = null;
    }

    private void uploadTar( long contentLength, URI uploadURI, DigestInputStream is ) {
        HttpURLConnection conn = null;
        long totLen = 0L;
        try {
            conn = (HttpURLConnection)uploadURI.toURL().openConnection();
            conn.setFixedLengthStreamingMode(contentLength);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestMethod(HttpMethod.PUT);
            conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            OutputStream os = conn.getOutputStream();
            byte[] buf = new byte[CHUNK_SIZE];
            int len;
            while ( (len = is.read(buf)) != -1 ) {
                os.write(buf, 0, len);
                totLen += len;
            }
            os.close();
            if ( conn.getResponseCode() != ClientResponse.Status.OK.getStatusCode() )
                mErrMsg = "Unable to upload object content: status="+conn.getResponseCode();
            else {
                List<String> eTags = conn.getHeaderFields().get(HttpHeaders.ETAG);
                if ( eTags == null || eTags.size() < 1 )
                    mErrMsg = "Unable to get ETag for uploaded data";
                else {
                    byte[] sentMD5 = is.getMessageDigest().digest();
                    if ( !Arrays.equals(mMD5,sentMD5) ) {
                        mErrMsg = "MD5 changed across the two tar passes."; return;
                    }

                    String eTag = eTags.get(0);
                    byte[] gotMD5 = DatatypeConverter.parseHexBinary(eTag.substring(1,eTag.length()-1));
                    if ( !Arrays.equals(gotMD5,sentMD5) ) {
                        mErrMsg = "ETag doesn't match streamed data's MD5."; return;
                    }
                }
            }
        } catch ( IOException e ) {
            mErrMsg = "IOException while uploading object content after writing " + totLen +
                        " of " + contentLength + " bytes: " + e.getMessage();
        } finally {
            if ( conn != null )
                conn.disconnect();
            try {
                is.close();
            } catch ( IOException e ) {
                mErrMsg = "Couldn't close tar process input stream: " + e.getMessage();
            }
        }
    }

    private static MessageDigest createMD5Digester() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Can't create MD5 MessageDigest.");
        }
    }

    private static boolean isURL( String urlish ) {
        boolean result = true;
        try {
            new URL(urlish);
        } catch ( MalformedURLException e ) {
            result = false;
        }
        return result;
    }
    private static void usage() {
        System.err.println("Usage: ObjectMaker [--conf hostuserpwd.props] jobDir1 [jobDir2...]");
        System.err.println("Each job directory is tarred and streamed to BOSS for storage.");
        System.err.println("If you don't specify a properties file as an arg, stdin will be read for host, user, and password properties.");
        System.exit(1);
    }

    private static class ResolutionResource {
        public URI objectUrl;
        
        @SuppressWarnings("unused")
        public Integer validityPeriodSeconds;
        
        @SuppressWarnings("unused")
        public String contentType;
        
        @SuppressWarnings("unused")
        public String contentMD5Hex;
    }

    private static class ResolutionRequest {
        @SuppressWarnings("unused")
        public Integer validityPeriodSeconds;
 
        @SuppressWarnings("unused")
        public String httpMethod;
 
        @SuppressWarnings("unused")
        public String contentType;

        @SuppressWarnings("unused")
        public String contentMD5Hex;
    }

    @JsonInclude(Include.NON_NULL)
    public class ObjectResource {
        public String objectId;
        public String objectName;
        public String storagePlatform;
        public String directoryPath;
        public Long sizeEstimateBytes;
        public String ownerId;
        public String[] readers, writers;
    }

    private String mJobDir;
    private byte[] mMD5;
    private long mSize;
    private String mErrMsg;
    private String mObjectURL;
    private Process mTarProc;

    private static int CHUNK_SIZE = 16*1024*1024; // 16Mb
    private static int VALIDITY_DURATION = 604800; // 1 week
    private static String OBJECTSTORE = "localStore";
    private static String gBossURL;
    private static String gUser;
    private static String[] gACL;
    private static Client gBOSSClient;
    //private static Client gOSClient;
}
