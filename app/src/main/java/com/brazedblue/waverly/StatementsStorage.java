package com.brazedblue.waverly;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
// import android.util.CustomLog;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

class StatementsStorage {
    private Vector<StatementData> m_ComposedData;
    private Vector<StatementData> m_ReceivedData;
    private Context m_Context;
    private ReentrantLock m_StorageLock = new ReentrantLock();
    private Key m_EncodeKey;

    private static final String RECEIVED_DIR = "Received";
    private static final String COMPOSED_DIR = "Composed";
    private static final String ASSETS_SAMPLES_DIR = "Samples";
    private static final String TAG = "makestatement.StatementsStorage";
    private static final String DOC_FILE_ENTRY = "Document.xml";
    private static final String PIC_FILE_SUFFIX = "_picture";
    private static final String ZIPFILE_SUFFIX = ".zip";
    private static final String ENCODE_DIR = "app_S";
    private static final String SKEY_FILE = "skey.enc";
    private static byte[] FILE_CODE = {117,49,106,107};
    private static final String CIPHER_TYPE = "DES/ECB/PKCS7Padding";


    private ByteArrayOutputStream debugOut = new ByteArrayOutputStream();


    private static int FILE_MODE = Context.MODE_PRIVATE;

    static final String EXTRA_UUID = "com.brazedblue.STATEMENT_UUID";
    private static StatementsStorage s_Storage;

    static StatementsStorage getStatementsStorage(Context context) {
        if (null == s_Storage) {
            s_Storage = new StatementsStorage(context.getApplicationContext());
            try {
                s_Storage.loadData();
                s_Storage.deleteSentFiles();
            }
            catch (Exception e)
            {
                CustomLog.e(TAG, "getStatementsStorage ", e);
            }
        }

        return s_Storage;
    }

    static Context getContext()
    {
        if (s_Storage != null)
        {
            return s_Storage.m_Context;
        }
        else
        {
            return null;
        }
    }

    private String getResourceString(int id)
    {
        return m_Context.getString(id);
    }


    File getPushMeSentDirectory()
    {
        return StatementsProvider.getProviderDirectory(m_Context);
    }

    StatementData createNewStatement()
    {
        StatementData data = null;
        try {
            data = new StatementData();

            handleNewData(data, null, getComposedDirectory(), m_ComposedData, null, null);
        }
        catch (Exception e)
        {
            CustomLog.e(TAG, "createNewStatement ", e);
        }

        return data;
    }

    // delete files that are more than 2 days old
    void deleteSentFiles()
    {
        Calendar cutOffDate = new GregorianCalendar();
        cutOffDate.add(GregorianCalendar.DAY_OF_MONTH, -1);
        long cutOffTime = cutOffDate.getTimeInMillis();
        File dir = getPushMeSentDirectory();
        if (dir.exists()) {
            File[] pushFiles = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(getResourceString(com.brazedblue.waverly.R.string.pushme_suffix));
                }
            });

            for (File pushFile : pushFiles) {
                CustomLog.d(TAG, pushFile.toString());
                if (pushFile.lastModified() < cutOffTime) {
                    pushFile.delete();
                }

            }
        }
    }
    File encodedFileForData(StatementData data) throws IOException, GeneralSecurityException
    {
        File zipFile = zipFileForData(data);
        String resultFileName = zipFile.getAbsolutePath();
        resultFileName = resultFileName.substring(0, resultFileName.lastIndexOf(ZIPFILE_SUFFIX));
        resultFileName = resultFileName.concat("." + getResourceString(com.brazedblue.waverly.R.string.pushme_suffix));
        File resultFile = new File(resultFileName);
        FileInputStream inStream = new FileInputStream(zipFile);
        FileOutputStream outStream = new FileOutputStream((resultFile));

        outStream.write(FILE_CODE);
        encodeStreams(inStream, outStream);
        outStream.close();
        zipFile.delete();

        return resultFile;
    }

    StatementData decodeStreamToData(InputStream inStream) throws IOException,
            GeneralSecurityException, ParserConfigurationException, SAXException, TransformerException
    {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        File receivedDir = getReceivedDirectory();
        File fileDir = new File(receivedDir, uuidString);
        if (!fileDir.mkdir())
        {
            CustomLog.e(TAG, "decodeStreamToData Could not mkDir " + fileDir);
            throw new IOException("Could not mk directory " + fileDir.toString());
        }
        byte [] fileCodeBytes = new byte[FILE_CODE.length];
        inStream.read(fileCodeBytes);
        if (!Arrays.equals(fileCodeBytes, FILE_CODE))
        {
            inStream.close();
            throw new IOException("Bad code for inStream");
        }
        String zipFileName = uuidString + ZIPFILE_SUFFIX;
        FileOutputStream zipOut = new FileOutputStream(new File(fileDir, zipFileName));

        decodeStreams(inStream, zipOut);
        zipOut.close();

        FileInputStream zipInStream = new FileInputStream(new File(fileDir, zipFileName));
        return getStatementWithInputStream(zipInStream);
    }
    private File zipFileForData(StatementData data) throws IOException {
         File dir = getPushMeSentDirectory();

        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create dir" + dir.getPath());

        }

        File file = new File(dir, UUID.randomUUID().toString() + ZIPFILE_SUFFIX);

        FileOutputStream fileOut = new FileOutputStream(file);
        ZipOutputStream zipOut = new ZipOutputStream(fileOut);
        Node parentNode = data.getParentNode();
        Document doc = parentNode.getOwnerDocument();

        ZipEntry entry = new ZipEntry(DOC_FILE_ENTRY);
        zipOut.putNextEntry(entry);
        File xmlFile = new File(URI.create(doc.getDocumentURI()));
        FileInputStream inStream = new FileInputStream(xmlFile);
        byte[] bytes = new byte[10000];
        int read = inStream.read(bytes);
        while (read >= 0) {
            zipOut.write(bytes, 0, read);
            read = inStream.read(bytes);
        }
        zipOut.closeEntry();

        Bitmap picture = data.getPicture(null);
        if (picture != null) {
            String pictureFileName = data.getPictureFileName();
            pictureFileName = pictureFileName.substring(0, pictureFileName.lastIndexOf('.'));

            ZipEntry pictureEntry = new ZipEntry(pictureFileName + PIC_FILE_SUFFIX);
            zipOut.putNextEntry(pictureEntry);
            picture.compress(Bitmap.CompressFormat.JPEG, 100, zipOut);
            zipOut.closeEntry();
        }

        zipOut.close();
        return file;
    }

    Key getEncodeKey()
    {
        if (m_EncodeKey == null) {
            AssetManager assets = m_Context.getResources().getAssets();
            try {
                ObjectInputStream in = new ObjectInputStream(assets.open(ENCODE_DIR + File.separator + SKEY_FILE));
                if (in != null) {
                    m_EncodeKey = (Key) in.readObject();
                 }
                in.close();
            } catch (Exception e) {
                CustomLog.e(TAG, "getEncodeKey " + SKEY_FILE, e);
            }
        }
        return m_EncodeKey;
    }

    void decodeStreams(InputStream inStream, OutputStream outputStream) throws IOException,
            GeneralSecurityException
    {
        Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
        cipher.init(Cipher.DECRYPT_MODE, getEncodeKey());
        byte[] in = new byte[64];
        int read;
        while ((read = inStream.read(in)) != -1) {
            byte[] output = cipher.update(in, 0, read);

            if (output != null)
                outputStream.write(output);
        }

        byte[] output = cipher.doFinal();
        if (output != null)
            outputStream.write(output);
        outputStream.flush();
    }

    private void encodeStreams(InputStream instream, OutputStream outstream) throws IOException,
            GeneralSecurityException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
            cipher.init(Cipher.ENCRYPT_MODE, getEncodeKey());

            //file encryption
            byte[] input = new byte[64];
            int bytesRead;
            while ((bytesRead = instream.read(input)) != -1) {
                byte[] output = cipher.update(input, 0, bytesRead);
                 if (output != null)
                    outstream.write(output);
            }

            byte[] output = cipher.doFinal();
            if (output != null)
                outstream.write(output);

        }
        catch (Throwable t)
        {
            CustomLog.d(TAG, t.toString());
            throw t;
        }
    }
    private StatementsStorage(Context context) {
        m_Context = context;
        m_ComposedData = new Vector<StatementData>();
        m_ReceivedData = new Vector<StatementData>();
    }

    StatementData cloneStatementData(StatementData data) throws ParserConfigurationException, IOException, TransformerException, TransformerFactoryConfigurationError
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();

        StatementData result = data.cloneStatementData(doc);

        handleNewData(result, doc, getComposedDirectory(), m_ComposedData, data.getPictureInStream(), data.getPictureFileName());

        return result;
    }

    private void handleNewData(StatementData data, Document doc, File inDir,
                               Vector<StatementData> list, InputStream picInStream, String picName) throws IOException, TransformerException
    {
        if (doc != null) {
            data.setDocument(doc);
        }
        String uuidString = data.getUUID().toString();
        File fileDir = new File(inDir, uuidString);
        if (!fileDir.mkdir())
        {
            CustomLog.e(TAG, "Could not mkDir " + fileDir);
            throw new IOException("Could not mk directory " + fileDir.toString());
        }
        data.setDirectory(fileDir);
        File statementFile = new File(fileDir, uuidString + ".xml");
        data.getOwnerDocument().setDocumentURI(statementFile.toURI().toString());
        try {
            m_StorageLock.lock();
            if (!statementFile.createNewFile()) {
                CustomLog.e(TAG, "Could not mkDir " + fileDir);
                throw new IOException("Could not mk file " + fileDir.toString());
            }
            data.write();
            if (picInStream != null)
            {
                data.writePicture(picInStream, picName);
            }
            list.insertElementAt(data, 0);
        }
        finally
        {
            m_StorageLock.unlock();
        }

    }

    List<StatementData> getComposedStatements()
    {
        return new Vector<StatementData>(m_ComposedData);
    }

    File getComposedDirectory()
    {
        return m_Context.getDir(COMPOSED_DIR, FILE_MODE);
    }

    List<StatementData> getReceivedStatements()
    {
        return new Vector<StatementData>(m_ReceivedData);
    }

    File getReceivedDirectory()
    {
        return m_Context.getDir(RECEIVED_DIR, FILE_MODE);
    }


    void loadData() throws IOException, ParserConfigurationException {
        m_ComposedData.clear();
        m_ReceivedData.clear();

        File composedDir = getComposedDirectory();
        File receivedDir = getReceivedDirectory();

        if (composedDir.list().length == 0 && receivedDir.list().length == 0)
        {
            loadFromAssets();
        }

        m_StorageLock.lock();
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            loadFromDirectory(composedDir, builder, m_ComposedData);
            loadFromDirectory(receivedDir, builder, m_ReceivedData);
        }
        finally
        {
            m_StorageLock.unlock();
        }

        Collections.sort(m_ComposedData, Collections.reverseOrder());
        Collections.sort(m_ReceivedData, Collections.reverseOrder());
    }

    private void loadFromDirectory(File topDir, DocumentBuilder builder, Vector<StatementData> dataList)
    {
        String[] fileStrings = topDir.list();
        for (int i_file = 0; i_file < fileStrings.length; i_file++) {
            File dir2 = new File(topDir, fileStrings[i_file]);
            if (dir2.isDirectory())
            {
                File[] xmlFiles = dir2.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return filename.endsWith("xml");
                    }
                });
                if (xmlFiles.length == 0)
                {
                    File[] files = dir2.listFiles();
                    for (File deleteFile : files) {
                        deleteFile.delete();
                    }
                    dir2.delete();
                }

                for (int i = 0; i < xmlFiles.length; i++) {
                    File xmlFile = xmlFiles[i];
                    try
                    {
                        InputStream in = new FileInputStream(xmlFile);
                        Document doc = builder.parse(in);
                        doc.setDocumentURI(xmlFile.toURI().toString());
                        NodeList nodeList = doc.getElementsByTagName(StatementData.STATEMENT_TAG);
                        for (int j = 0; j < nodeList.getLength(); j++) {
                            Node node = nodeList.item(j);
                            StatementData data = new StatementData(node);
                            if (data.isValid()) {
                                data.setDirectory(dir2);

                                dataList.addElement(data);
                            } else {
                                CustomLog.e(TAG, "Invalid assets incoming file " + xmlFile + ", statement " + node.toString());
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        CustomLog.e(TAG, "loadFromDirectory exception " + xmlFile, e);
                    }
                    catch (SAXException e)
                    {
                        CustomLog.e(TAG, "loadFromDirectory exception " + xmlFile, e);
                        xmlFile.delete();
                    }

                }
            }
        }
    }

    private StatementData blankIfNull(StatementData data)
    {
        if (null == data)
        {
            try {
                 data = createNewStatement();
            }
            catch (Exception e)
            {
                CustomLog.e(TAG, "blankIfNull " + data.getUUID(), e);
            }
        }

        return data;
    }

    void loadFromAssets() throws IOException, ParserConfigurationException {
        AssetManager assets = m_Context.getResources().getAssets();

        String[] samplesAssetsDir = assets.list(ASSETS_SAMPLES_DIR);
        m_ComposedData.clear();

        File composedDir = getComposedDirectory();
        if (!composedDir.exists())
        {
            if (!composedDir.mkdir())
            {
                String message = "Could not make dir " + composedDir;
                CustomLog.d(TAG, message);
                throw new IOException(message);
            }
        }

        for (String assitsDir : samplesAssetsDir) {
            File outDir = new File(composedDir, assitsDir);
            if (!outDir.mkdir())
            {
                CustomLog.d(TAG, "unable to make directory " + outDir);
                continue;
            }
            String assetsDirFullPath = ASSETS_SAMPLES_DIR + File.separator + assitsDir;
            String[] assitsFiles = assets.list(assetsDirFullPath);

            for (String assetsFile : assitsFiles) {
                String inFileName = assetsDirFullPath + File.separator + assetsFile;
                InputStream in = assets.open(inFileName);
                File outFile  = new File(outDir, assetsFile);
                OutputStream out = new FileOutputStream(outFile);

                byte[] buf = new byte[1024];
                int len;
                while((len=in.read(buf))>0){
                    out.write(buf,0,len);
                }
                out.close();
                in.close();
            }
        }
    }


    void writeStatementData(StatementData data) throws IOException, TransformerException {
        try
        {
            m_StorageLock.lock();
            data.write();
        }
        finally
        {
            m_StorageLock.unlock();
        }
    }
    StatementData getStatementWithUUIDString(String uuidString)
    {
        return getStatementWithUUIDString(uuidString, true);
    }
    StatementData getStatementWithUUIDString(String uuidString, boolean blankIfNull) {
        UUID uuid =  UUID.fromString(uuidString);

        StatementData result = null;
        for (int i = 0; i < m_ComposedData.size(); ++i) {
            if (m_ComposedData.elementAt(i).getUUID().equals(uuid)) {
                result = m_ComposedData.elementAt(i);
                break;
            }
        }
        if (null == result) {
            for (int i = 0; i < m_ReceivedData.size(); ++i) {
                if (m_ReceivedData.elementAt(i).getUUID().equals(uuid)) {
                    result = m_ReceivedData.elementAt(i);
                    break;
                }
            }

        }

        return blankIfNull ? blankIfNull(result) : result;
    }



    void deleteStatement(StatementData data)
    {
        if (data != null)
        {
            m_StorageLock.lock();
            m_ComposedData.remove(data);
            m_ReceivedData.remove(data);
            m_StorageLock.unlock();

            Node root = data.getParentNode();
            Document doc = root.getOwnerDocument();
            String docURI = doc.getDocumentURI();
            try {
                File file = new File(new URI(docURI));
                File directory = file.getParentFile();
                File[] files = directory.listFiles();
                for (int i = 0; i < files.length; i++)
                {
                    if (!files[i].delete())
                    {
                        CustomLog.e(TAG, "deleteStatement - Could not delete " + files[i]);
                    }
                }
                if (!directory.delete())
                {
                    CustomLog.e(TAG, "deleteStatement - Could not delete " + directory);
                }
            }
            catch (URISyntaxException e)
            {
                CustomLog.e(TAG, "deleteStatement " + docURI, e);
            }
        }
    }

    StatementData getNextStatement(StatementData afterThis)
    {
        if (afterThis == null)
        {
            if (m_ComposedData.size() > 0){
                return m_ComposedData.firstElement();
            }
            else if (m_ReceivedData.size() > 0)
            {
                return m_ReceivedData.firstElement();
            }
            else
            {
                return blankIfNull(null);
            }
        }

        boolean found = false;
        StatementData result = null;
        for (StatementData data : m_ComposedData) {
            if (found)
            {
                result = data;
                break;
            }

            if (data.equals(afterThis))
            {
                found = true;
            }
        }
        if (found && result == null)
        {
            if (m_ReceivedData.size() > 0) {
                result = m_ReceivedData.firstElement();
            }
            else if (m_ComposedData.size() > 0)
            {
                result = m_ComposedData.firstElement();

            }
        }
        if (!found )
        {
            for (StatementData data : m_ReceivedData) {
                if (found)
                {
                    result = data;
                    break;
                }

                if (data.equals(afterThis))
                {
                    found = true;
                }
            }
        }

        if (found && result == null)
        {
            if (m_ComposedData.size() > 0)
            {
                result = m_ComposedData.firstElement();
            }
            else if (m_ReceivedData.size() > 0) {
                result = m_ReceivedData.firstElement();
            }
        }

        return blankIfNull(result);
    }

    StatementData getPreviousStatement(StatementData beforeThis)
    {
        if (beforeThis == null)
        {
            if (m_ComposedData.size() > 0){
                return m_ComposedData.firstElement();
            }
            else if (m_ReceivedData.size() > 0)
            {
                return m_ReceivedData.firstElement();
            }
            else
            {
                return blankIfNull(null);
            }
        }

        boolean found = false;
        StatementData result = null;
        StatementData previous = null;
        for (StatementData data : m_ComposedData) {

            if (data.equals(beforeThis))
            {
                found = true;
                result = previous;
            }
            previous = data;
        }
        if (found && result == null)
        {
            if (m_ReceivedData.size() > 0) {
                result = m_ReceivedData.lastElement();
            }
            else if (m_ComposedData.size() > 0)
            {
                result = m_ComposedData.lastElement();

            }
        }
        if (!found )
        {
            for (StatementData data : m_ReceivedData) {
                if (data.equals(beforeThis))
                {
                    found = true;
                    result = previous;
                }
                previous = data;
            }
        }

        if (found && result == null)
        {
            if (m_ComposedData.size() > 0)
            {
                result = m_ComposedData.lastElement();
            }
            else if (m_ReceivedData.size() > 0) {
                result = m_ReceivedData.lastElement();
            }
        }

        return blankIfNull(result);
    }


    public StatementData getStatementWithInputStream(InputStream inputStream) throws IOException,
            ParserConfigurationException, SAXException, TransformerException{
        StatementData result = null;
        ZipInputStream zis = new ZipInputStream(inputStream);
        File receivedDir = getReceivedDirectory();
        if (!receivedDir.exists())
        {
            if (!receivedDir.mkdir()) {
                throw new IOException("Could not create directory " + receivedDir.getPath());
            }

        }
        UUID uuid = UUID.randomUUID();

        InputStream bitmapInStream = null;
        Document doc = null;
        String picFileName = null;
        try {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String filename = ze.getName();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;
                while ((count = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                }
                if (filename.equals(DOC_FILE_ENTRY))
                {
                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    InputSource in = new InputSource(new ByteArrayInputStream(baos.toByteArray()));
                    doc = builder.parse(in);
                    NodeList nodeList = doc.getElementsByTagName(StatementData.STATEMENT_TAG);
                    if (nodeList.getLength() > 0) {
                        Node parentNode = nodeList.item(0);
                        result = new StatementData(parentNode);
                        result.setUUID(uuid);
                        result.setTime(new Date());
                        result.setPictureFileName("");
                    }
                }
                else if (filename.endsWith(PIC_FILE_SUFFIX))
                {
                    picFileName = filename.substring(0, filename.lastIndexOf(PIC_FILE_SUFFIX));
                    picFileName += ".jpg"; // jpg encoding used
                    bitmapInStream = new ByteArrayInputStream(baos.toByteArray());
                }
            }
        } finally {
            zis.close();
        }

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document newDoc = builder.newDocument();
        handleNewData(result, newDoc, getReceivedDirectory(), m_ReceivedData, bitmapInStream, picFileName);

        return result;
    }

    void deleteItems(final ArrayList<String> itemIDS)
    {
        for (String id : itemIDS) {
            StatementData data = getStatementWithUUIDString(id);
            if (data != null)
            {
                deleteStatement(data);
            }
        }
    }
}
