package folioxml.slx;

import folioxml.core.InvalidMarkupException;


public class SlxValidator {

    protected SlxContextStack stack;

    public SlxValidator(SlxContextStack stackReference) {
        this.stack = stackReference;
    }
    
        /* Slx compatibility tag set
     infobase-meta, style-def/>, record, record-attribute/>, span, link, popupLink, end-popup-contents/>, note, namedPopup, parabreak />,  object/>, table, tr, td
     * paragraph-attribute/>, pagebreak />, br/>, bookmark/>, pp/>, se/>
    */
    
    /* Transformed tag set
     * new: <p>, <popup>, <link type="popup">
     * infobase-meta, style-def/>, record>, span, link, popup, note, namedPopup,   object/>, table, tr, td
     *  br/>, bookmark/>
    */
    
    /* removed by transform: record-attribute, paragraph-attribute, pp, se, pagebreak, popupLink, end-popup-contents, parabreak*/
    
    /* 
     * context tags:  record, infobase-meta, popupLink, note, namedPopup, popup
     * standard: p, table, tr, td, object/>, br/>, bookmark/>, style-def/>, record-attribute/>, paragraph-attribute/>
     * ghost: span, link
     * 
     * auto-repairs: insert <p> tags, close p tags
     * auto-close tr, td, p
     * auto-close record tags.
     * auto-close ghost tags (span,link) before context end.
     */

    /**
     * These tags can't have content.
     */
    private static String noContentTags = "br|bookmark|style-def|record-attribute|paragraph-attribute|object|pagebreak|object-def";


    /**
     * Called before SlxTransformer makes any modifications to ancestors.
     * This tag recieves some tags that validate() doesn't, such as record-attribute and paragraph-attribute.
     *
     * @param t
     * @throws folioxml.core.InvalidMarkupException
     */
    public void preValidate(SlxToken t) throws InvalidMarkupException {
        //Verify infobase-meta tags,
        //Verify record-attribute tags
        //verify paragraph-attribute tags
        //verify style-def tags.
        SlxToken topContext = stack.getTopContext();
        boolean isInRoot = (topContext != null && topContext.matches("record") && "root".equalsIgnoreCase(topContext.get("level")));


        if (t.isContent() && isInRoot)
            throw new InvalidMarkupException("Text and entities are not allowed at the top of the file. They cannot appear in the root record.", t);

        //We only proccess opening tags for normal elements, but both opening and closing for ghost elements
        if (t.isTag() && (t.isOpening() || t.isGhost)) {
            if (topContext == null && !t.matches("record"))
                throw new InvalidMarkupException("Only record tags are allowed at the root level. All tags must be contained within a record.", t);


            if (isInRoot) {
                if (!t.matches("infobase-meta|style-def|object-def|namedPopup"))
                    throw new InvalidMarkupException("Only style definitions, named popups, and infobase information can appear in the root record", t);

            } else {
                if (t.matches("style-def|infobase-meta|object-def"))
                    throw new InvalidMarkupException("infobase-meta, object-def, and style-def are only allowed in the root record, at the top of the file.", t);

                if (t.matches("record-attribute") && !stack.has("record", true))
                    throw new InvalidMarkupException("The record-attribute tag can only be used within a record.", t);


                if (t.matches("paragraph-attribute") && !stack.has("p", true))
                    throw new InvalidMarkupException("The paragraph-attribute tag can only be used within a paragraph.", t);
            }
        }
    }

    /**
     * Called before SlxTransformer pushes/pops a tag
     *
     * @param t
     * @throws folioxml.core.InvalidMarkupException
     */
    public void validate(SlxToken t) throws InvalidMarkupException {
        SlxToken top = stack.top();


        if (t.isTag()) {
            if (!t.matches("infobase-meta|record|note|popup|namedPopup|link|span|p|table|tr|td|th|object|br|bookmark|style-def|object-def")) {
                throw new InvalidMarkupException("Unrecognized tag.", t);
            }
            //Normal tags are already required to close at the same level as they open. We only need to test when they open.
            if (t.isOpening()) {

                if (t.matches("record")) assert (stack.size() == 0);


                if (t.matches("tr")) assert (top.matches("table")); //tr tags only go in tables
                if (top.matches("table")) assert (t.matches("tr")); //tables can only contain tr tags
                if (t.matches("td|th")) assert (top.matches("tr")); //td tags only go in tr
                if (top.matches("tr")) assert (t.matches("td|th")); //tr tags can only contain td tags

                //paragraphs cannot contain tabes
                if (t.matches("table")) assert (!stack.has("p"));

                //tables cannot contain other tables - (Are you sure?)
                if (t.matches("table")) assert (!stack.has("table"));

                //notes cannot contain other notes
                if (t.matches("note")) assert (!stack.has("note", true));

                //records cannot contain other records
                if (t.matches("record")) assert (!stack.has("record", true));

                //notes must be inside a paragraph
                if (t.matches("note")) assert (stack.has("p"));

                //links cannot contain other links
                if (t.matches("link") && stack.has("link"))
                    throw new InvalidMarkupException("Links cannot be nested. " + t + " was found inside " + stack.get("link"), t);

                //Paragraphs should be right below the context node or table cell
                if (t.matches("p")) assert (top.matches("record|note|popup|namedPopup|td|infobase-meta"));


            }

            if (stack.has(noContentTags)) {
                assert (t.isClosing() && t.matches(noContentTags)); //These tags shouldn't be followed by anthing but their own closing tags
            }
        } else if (t.isContent()) {
            assert (!top.matches("table|tr")); //Tables and tr can't have content directly inside them

            assert (!stack.has(noContentTags));//can't have any content
        }
    }

    /**
     * Used by GhostTagSplitter.
     * This prevents span tags from getting split around td and tr elements causing invalid markup.
     * <p>
     * Only called for span elements.
     *
     * @param top
     * @param t   The child tokne
     * @return
     * @throws InvalidMarkupException
     */
    public static boolean isAllowedInside(SlxToken t, SlxToken parent) throws InvalidMarkupException {
        assert (t.matches("span|link"));

        //return !(parent.matches("table|tr|object|br|bookmark|style-def"));

        ///if (parent.matches("span|p|td|th|infobase-meta|record|note|popup|namedPopup|link")) return true; //Cannot be directly inside - table|tr|object|br|bookmark|style-def|object-def
        //Removing record tag from allowed parent list - need to force span tags to be inside paragraph tags, not directly in the file.

        if (parent.matches("span|p|td|th|infobase-meta|note|popup|namedPopup|link"))
            return true; //Cannot be directly inside - table|tr|object|br|bookmark|style-def|object-def

        return false;
    }

}