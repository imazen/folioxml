package folioxml.xml;

public class XmlFormatter {

    public XmlFormatter(int startingIndent) {
        defaultIndentDepth = startingIndent;
    }

    public XmlFormatter(int startingIndent, String indentString) {
        defaultIndentDepth = startingIndent;
        this.indentString = indentString;
    }

    public String indentString = "\t";
    public int optimalLineLength = 80;
    public String inlineElementsRegex = "span|link|a";

    public int defaultIndentDepth = 0;


    public String format(NodeList nl) {
        StringBuilder sb = new StringBuilder();
        writeNodes(nl, sb, defaultIndentDepth);
        return sb.toString();
    }

    public String format(Node n) {
        StringBuilder sb = new StringBuilder();
        writeNode(n, sb, defaultIndentDepth);
        return sb.toString();
    }


    private void writeNodes(NodeList nl, StringBuilder sb, int indentDepth) {
        if (nl == null) return;
        for (Node n : nl.list()) writeNode(n, sb, indentDepth);
    }

    private void writeNode(Node n, StringBuilder sb, int indentDepth) {

        boolean onNewLine = sb.length() > 0 ? (sb.charAt(sb.length() - 1) == '\n') : true;


        //We want
        // * non-ghost tags to appear by themselves on their own lines (and their closing tags)
        // * ghost tags, text, entities, and comments are all the same, except text and comments are wrapped.

        if (n.isTag() && !n.matches(inlineElementsRegex)) {
            if (!onNewLine) sb.append('\n');

            for (int i = 0; i < indentDepth; i++) sb.append(indentString);

            n.writeTokenTo(sb);
            //write a newline after
            sb.append('\n');
            if (n.isOpening()) {
                //Do children
                writeNodes(n.children, sb, indentDepth + 1);//Do children. Indent the depth.
                //Write closing tag
                onNewLine = sb.length() > 0 ? (sb.charAt(sb.length() - 1) == '\n') : true;
                if (!onNewLine) sb.append('\n');
                //Write tabs
                for (int i = 0; i < indentDepth; i++) sb.append(indentString);
                //Closing tag
                sb.append(n.getClosingTagString());
                sb.append('\n');
            }
        } else {
            if (onNewLine) {
                for (int i = 0; i < indentDepth; i++) sb.append(indentString);
            }
            if (n.isTag()) {
                n.writeTokenTo(sb); //Opening tag
                if (n.isOpening()) {
                    writeNodes(n.children, sb, indentDepth);//Do children. Don't indent the depth.
                    sb.append(n.getClosingTagString()); //Closing tag
                }

            } else if (n.isEntity()) {
                sb.append(n.toTokenString());//Write inline - append to the previous line.
            } else { //text and comments
                //Build indent string
                StringBuilder indentStr = new StringBuilder(indentDepth + 1);
                for (int i = 0; i < indentDepth; i++) indentStr.append(indentString);

                //Write text wrapped
                writeText(n.toTokenString(), sb, optimalLineLength, indentStr.toString());
            }
        }

    }

    private void writeText(String s, StringBuilder sb, int wrapChars, String indentString) {
        //Keeps track of how many characters are on the current line.
        int currentChars = sb.length();
        //Find the last newline if it exists.
        int lastNl = sb.lastIndexOf("\n");
        if (lastNl > -1) currentChars = sb.length() - lastNl - 1;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c);
            currentChars++;
            //Break lines after whitespace
            if (currentChars > wrapChars) {
                if (c == '\t' || c == ' ') {
                    sb.append('\n');
                    sb.append(indentString);
                    currentChars = indentString.length();
                }
            }
        }
    }


}
