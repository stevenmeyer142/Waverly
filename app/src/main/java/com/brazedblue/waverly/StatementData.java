package com.brazedblue.waverly;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
// import android.util.CustomLog;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


class StatementData implements Comparable {
    private Node m_ParentNode;
    private Node m_NounNode;
    private Node m_VerbNode;
    private Node m_VerbButtonNode;
    private Node m_ResultNode;
    private Node m_PictureNode;
    private Node m_IDNode;
    private Node m_TimeNode;
    private File m_Directory;
    private Bitmap m_PictureCache;

    private static final String NOUN_TAG = "noun";
    private static final String VERB_TAG = "verb";
    private static final String VERB_BUTTON_TAG = "verb_button";
    private static final String RESULT_TAG = "result";
    private static final String PICTURE_TAG = "picture";
    private static final String TIME_MILLISEC_TAG = "time";
    static String STATEMENT_TAG = "statement";
    static final String UUID_TAG = "uuid";
    private static final String JPEG_SUFFIX = "jpg";

    public enum STATEMENT_ELEMENT {
        NONE,
        NOUN,
        VERB_BUTTON,
        VERB_TEXT,
        RESULT_TEXT,
        PICTURE_FILE
    }

    private static String TAG = "StatementData";

    StatementData(Node node) {
        m_ParentNode = node;

        readNodes();
    }

    StatementData() throws ParserConfigurationException, TransformerException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();

        m_ParentNode = doc.createElement(STATEMENT_TAG);
        setDocument(doc);

        createNodes(doc);
    }

    void setDocument(Document doc) throws TransformerException {
        m_ParentNode = doc.adoptNode(m_ParentNode);
        if (m_ParentNode == null) {
            throw new TransformerException("Could not adopt node");
        }
        if (m_ParentNode != doc.getFirstChild()) {
            doc.appendChild(m_ParentNode);
        }

    }

    Document getOwnerDocument() {
        return m_ParentNode.getOwnerDocument();
    }

    Node createNode(String tag) {
        Node node = getOwnerDocument().createElement(tag);
        m_ParentNode.appendChild(node);
        return node;

    }

    void releasePictureCache() {
        if (m_PictureCache != null) {
            m_PictureCache = null;
        }
    }

    void setDirectory(File dir) {
        m_Directory = dir;
    }


    private void createNodes(Document doc) {
        m_IDNode = doc.createElement(UUID_TAG);
        m_ParentNode.appendChild(m_IDNode);
        m_IDNode.setTextContent(UUID.randomUUID().toString());

        m_PictureNode = doc.createElement(PICTURE_TAG);
        m_ParentNode.appendChild(m_PictureNode);

        m_VerbButtonNode = doc.createElement(VERB_BUTTON_TAG);
        m_ParentNode.appendChild(m_VerbButtonNode);
        m_VerbButtonNode.setTextContent(getResourceString(com.brazedblue.waverly.R.string.verb_button_blank));

        m_TimeNode = doc.createElement(TIME_MILLISEC_TAG);
        m_ParentNode.appendChild(m_TimeNode);
        setTime(new Date());
    }

    private void readNodes() {
        NodeList childNodes = m_ParentNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String name = childNode.getNodeName();

            if (NOUN_TAG.equals(name)) {
                m_NounNode = childNode;
                continue;
            }
            if (VERB_TAG.equals(name)) {
                m_VerbNode = childNode;
                continue;
            }
            if (RESULT_TAG.equals(name)) {
                m_ResultNode = childNode;
                continue;
            }
            if (PICTURE_TAG.equals(name)) {
                m_PictureNode = childNode;
                continue;
            }
            if (VERB_BUTTON_TAG.equals(name)) {
                m_VerbButtonNode = childNode;
                continue;
            }
            if (UUID_TAG.equals(name)) {
                m_IDNode = childNode;
                continue;
            }

            if (TIME_MILLISEC_TAG.equals(name)) {
                m_TimeNode = childNode;
                continue;
            }

        }

    }


    Node getParentNode() {
        return m_ParentNode;
    }


    Bitmap loadPicture(int width, int height) {
        Bitmap newBitmap = null;
        if (m_PictureCache == null &&  m_Directory != null && !getPictureFileName().isEmpty()) {
            String pictureName = m_PictureNode.getTextContent();
            File pictureFile = new File(m_Directory, pictureName);
            try {
                String filePath = pictureFile.getAbsolutePath();

                if (width > 0 && height > 0) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    newBitmap = BitmapFactory.decodeFile(filePath, options);
                    float scale = Utils.scaleFactorToFit(options.outWidth, options.outHeight,
                            width, height, true);
                    scale = Math.min(scale, 1);
                    if (scale == 0) {
                        scale = 1;
                    }
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = (int) Math.floor(1.0f / scale);
                    newBitmap = BitmapFactory.decodeFile(filePath, options);
                    m_PictureCache = newBitmap;
                } else {
                    newBitmap = BitmapFactory.decodeFile(filePath);
                }

            } catch (Throwable e) {
                CustomLog.e(TAG, "Could not get picture " + pictureFile, e);
            }
        }
        return newBitmap;
    }

    void write() throws IOException, TransformerException {
        setTime(new Date());
        Node parentNode = getParentNode();
        Document document = parentNode.getOwnerDocument();
        String uri = document.getDocumentURI();

        File file = new File(URI.create(uri));
        FileOutputStream outStream = new FileOutputStream(file);

        TransformerFactory.newInstance().newTransformer().transform(
                new DOMSource(document), new StreamResult(outStream));

        outStream.close();
    }

    void writePicture(Bitmap bitmap) throws IOException {
        if (m_Directory != null) {
            deleteOldPictureFile();

            String fileName = UUID.randomUUID().toString() + "." + JPEG_SUFFIX;
            setPictureFileName(fileName);
            File file = new File(m_Directory, fileName);
            FileOutputStream outStream = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.close();
        } else {
            CustomLog.e(TAG, "writePicture(Bitmap) m_Directory == null");
        }

    }


    void writePicture(InputStream picInStream, String picName) throws IOException {
        if (m_Directory != null) {
            deleteOldPictureFile();


            setPictureFileName(picName);

            File file = new File(m_Directory, picName);
            FileOutputStream outStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = picInStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, read);
            }

            outStream.close();
            picInStream.close();
        } else {
            CustomLog.e(TAG, "writePicture(File) m_Directory == null");
        }
    }

    InputStream getPictureInStream() throws FileNotFoundException {
        InputStream result = null;
        if (m_Directory != null) {
            String fileName = getPictureFileName();
            if (fileName != null && !fileName.isEmpty()) {
                result = new FileInputStream(new File(m_Directory, fileName));
            }
        } else {
            CustomLog.e(TAG, "getPictureFile m_Directory == null");
        }

        return result;
    }

    private void deleteOldPictureFile() {
        String fileName = getPictureFileName();

        if (fileName != null && !fileName.isEmpty()) {
            File oldJPeg = new File(m_Directory, fileName);
            if (!oldJPeg.delete()) {
                CustomLog.e(TAG, "deleteOldPictureFile could not delete " + oldJPeg);
            }
            setPictureFileName("");
            releasePictureCache();
        }
    }

    void purge() {
        if (m_Directory != null) {
            m_PictureCache = null;
        }
    }

    boolean isValid() {
        if ((m_NounNode == null) || (m_VerbNode == null)
         || (m_ResultNode == null) ) {
            return false;
        }
        return true;
    }

    void validate() {
        if (m_NounNode == null) {
            m_NounNode = createNode(NOUN_TAG);
            m_NounNode.setTextContent(getResourceString(com.brazedblue.waverly.R.string.noun_blank));
        }
        if (m_VerbNode == null) {
            m_VerbNode = createNode(VERB_TAG);
            m_VerbNode.setTextContent(getResourceString(com.brazedblue.waverly.R.string.verb_blank));
        }
        if (m_ResultNode == null) {
            m_ResultNode = createNode(RESULT_TAG);
            m_ResultNode.setTextContent(getResourceString(com.brazedblue.waverly.R.string.result_blank));
        }

    }

    void setNoun(String text) {
        if (m_NounNode == null) {
            m_NounNode = createNode(NOUN_TAG);
        }

        m_NounNode.setTextContent(text);
    }

    String getNoun() {
        return getElementString(STATEMENT_ELEMENT.NOUN);
    }

    void setVerbText(String text) {
        if (m_VerbNode == null) {
            m_VerbNode = createNode(VERB_TAG);
        }

        m_VerbNode.setTextContent(text);
    }

    String getVerbText() {
        return getElementString(STATEMENT_ELEMENT.VERB_TEXT);
    }

    String getVerbButtonText() {
        return getElementString(STATEMENT_ELEMENT.VERB_BUTTON);
    }

    void setVerbButtonText(String text) {
        if (m_VerbButtonNode == null) {
            m_VerbButtonNode = createNode(VERB_BUTTON_TAG);
        }
        m_VerbButtonNode.setTextContent(text);
    }

    void setResultText(String text) {
        if (m_ResultNode == null) {
            m_ResultNode = createNode(RESULT_TAG);
        }

        m_ResultNode.setTextContent(text);
    }

    String getResult() {
        return getElementString(STATEMENT_ELEMENT.RESULT_TEXT);
    }

    Bitmap getPicture(DisplayMetrics metrics) {
        if (metrics != null) {
            return getPicture(metrics.widthPixels, metrics.heightPixels);
        } else {
            return getPicture(0, 0);

        }
    }

    Bitmap getPicture(int width, int height) {
        if (m_PictureCache != null) {
            return m_PictureCache;
        } else {
            return loadPicture(width, height);
        }
    }

    String getPictureFileName() {
        return getElementString(STATEMENT_ELEMENT.PICTURE_FILE);
    }

    boolean wasElementSet(STATEMENT_ELEMENT element) {
        boolean result = true;
        switch (element) {
            case NOUN:
                result = m_NounNode != null;
                break;

            case VERB_BUTTON:
                result = m_VerbButtonNode != null;
                break;


            case VERB_TEXT:
                result = m_VerbNode != null;
                break;

            case RESULT_TEXT:
                result = m_ResultNode != null;
                break;
        }

        return result;
    }

    String getElementHintString(STATEMENT_ELEMENT element)
    {
        switch (element) {
            case NOUN:
                return getResourceString(com.brazedblue.waverly.R.string.noun_blank);


            case VERB_BUTTON:
                return getResourceString(com.brazedblue.waverly.R.string.verb_button_blank);



            case VERB_TEXT:
                return getResourceString(com.brazedblue.waverly.R.string.verb_blank);


            case RESULT_TEXT:
               return getResourceString(com.brazedblue.waverly.R.string.result_blank);

            default:
                 CustomLog.e(TAG, "getElementHintString case not implemented " + element);
                break;

        }

        return "";

    }

    void setElementString(STATEMENT_ELEMENT element, String text)
    {

        switch (element) {
            case NOUN:
                setNoun(text);
                break;


            case VERB_BUTTON:
                setVerbButtonText(text);
                break;

            case VERB_TEXT:
                setVerbText(text);
                break;

            case RESULT_TEXT:
                setResultText(text);
                break;


            case NONE:
                CustomLog.e(TAG, "setElementString case not implemented " + element);
                break;

        }

    }

    String getElementString(STATEMENT_ELEMENT element) {
        switch (element) {
            case NOUN:
                if (m_NounNode != null) {
                    return m_NounNode.getTextContent();
                } else {
                    return "";
                }


            case VERB_BUTTON:
                if (m_VerbButtonNode != null) {
                    return m_VerbButtonNode.getTextContent();
                } else {
                    return "";
                }


            case VERB_TEXT:
                if (m_VerbNode != null) {
                    return m_VerbNode.getTextContent();
                } else {
                    return "";
                }

            case RESULT_TEXT:
                if (m_ResultNode != null) {
                    return m_ResultNode.getTextContent();
                } else {
                    return "";
                }

            case PICTURE_FILE:
                if (m_PictureNode != null) {
                    return m_PictureNode.getTextContent();
                } else {
                    return "";
                }

            case NONE:
                CustomLog.e(TAG, "getElementString NONE case");
                break;

        }

        return "";
    }

    String getResourceString(int id) {
        Context context = StatementsStorage.getContext();
        if (context != null) {
            return context.getString(id);
        } else {
            CustomLog.e(TAG, "getResourceString null context");
            return "";
        }
    }

    void setPictureFileName(String name) {
        if (m_PictureNode == null) {
            m_PictureNode = createNode(PICTURE_TAG);
        }

        m_PictureNode.setTextContent(name);
    }


    UUID getUUID() {
        String text = (m_IDNode != null) ? m_IDNode.getTextContent() : "";
        return UUID.fromString(text);
    }

    void setUUID(UUID uuid) {
        if (m_IDNode == null) {
            m_IDNode = createNode(UUID_TAG);
        }

        m_IDNode.setTextContent(uuid.toString());
    }

    StatementData cloneStatementData(Document document) {
        Node clonedNode = m_ParentNode.cloneNode(true);

        StatementData result = new StatementData(clonedNode);

        result.setUUID(UUID.randomUUID());
        result.setPictureFileName("");
        return result;
    }

    boolean areTextNodesEqual(Node n1, Node n2) {
        boolean result = false;

        if (n1 != null && n2 != null) {
            result = n1.getTextContent().equals(n2.getTextContent());
        } else if (n1 == null && n2 == null) {
            result = true;
        }

        return result;
    }

    @Override
    public int compareTo(@NonNull Object another) {
        StatementData otherData = (StatementData) another;

        Date myDate = getTime();
        Date otherDate = otherData.getTime();
        return myDate.compareTo(otherDate);
    }

    boolean isSame(StatementData data) {
        boolean same = areTextNodesEqual(m_NounNode, data.m_NounNode);
        if (same) {
            same = areTextNodesEqual(m_ResultNode, data.m_ResultNode);
        }
        if (same) {
            same = areTextNodesEqual(m_VerbButtonNode, data.m_VerbButtonNode);
        }
        if (same) {
            same = areTextNodesEqual(m_VerbNode, data.m_VerbNode);
        }

        if (same) {
            String pic1Name = getPictureFileName();
            String pic2Name = data.getPictureFileName();
            if (pic1Name != null && pic2Name != null) {

                int suff1Index = pic1Name.lastIndexOf('.');
                int suff2Index = pic2Name.lastIndexOf('.');
                if (suff1Index >= 0 && suff2Index >= 0) {
                    pic1Name = pic1Name.substring(0, suff1Index);
                    pic2Name = pic2Name.substring(0, suff2Index);
                    same = pic1Name.equals(pic2Name);
                }
                else {
                    same = pic1Name.isEmpty() && pic2Name.isEmpty();
                }
            } else {
                same = (pic1Name == null && pic2Name == null);
            }
        }

        return same;
    }

    Date getTime() {
        Date result = null;

        if (m_TimeNode != null) {
            String timeString = m_TimeNode.getTextContent();

            if (timeString != null) {
                try {
                    Long value = Long.decode(timeString);
                    result = new Date(value.longValue());
                } catch (NumberFormatException e) {
                    CustomLog.e(TAG, "getTime", e);
                }
            } else {
                CustomLog.d(TAG, "null string");
            }
        }
        if (result == null) {
            result = new Date();
        }

        return result;
    }

    void setTime(Date time) {
        if (m_TimeNode == null) {
            m_TimeNode = createNode(TIME_MILLISEC_TAG);
        }

        String millisecStr = String.valueOf(time.getTime());
        m_TimeNode.setTextContent(millisecStr);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StatementData) {
            StatementData other = (StatementData) o;
            return getUUID().equals(other.getUUID());
        } else {
            return false;
        }
    }
}