package folioxml.xml;

import folioxml.core.InvalidMarkupException;
import folioxml.text.TextTokenList;
import folioxml.text.VirtualCharSequence;


/**
 * Wraps an XML node and its descendants, exposing just the text as a char sequence. Changes made to this instance affect the underlying XML, but not vice versa.
 * If you change the XML externally, you must re-create this instance. Almost as fast as getTextContents() for creation.
 * Exposes replace, replaceAll, insert, delete, setCharAt, flattenArea...
 * Text tokens may contain entities after using methods of this class.
 *
 * @author nathanael
 */
public class XmlToStringWrapper extends VirtualCharSequence {


    //TODO: need to filter out some nodes usually - like note, popup, etc.
    //Changes need to be fielded.


    /**
     * @param source
     * @param decodeEntities If true, decodes entities found in text and entity tokens. Re-encodes special characters in modified tokens. If false, your regexes must account for &apos; as well as ' . Replacement text must be careful to encode chars properly
     * @throws InvalidMarkupException
     */
    public XmlToStringWrapper(Node source) throws InvalidMarkupException {
        this(source, true);
    }

    public XmlToStringWrapper(Node source, boolean decodeEntities) throws InvalidMarkupException {
        super(new TextTokenList(source, new Or(new NodeFilter("note"), new NodeFilter("popup"))), decodeEntities);
    }

    public XmlToStringWrapper(NodeList source) throws InvalidMarkupException {
        this(source, true);
    }

    public XmlToStringWrapper(NodeList source, boolean decodeEntities) throws InvalidMarkupException {
        super(new TextTokenList(source, new Or(new NodeFilter("note"), new NodeFilter("popup"))), decodeEntities);
    }
}
