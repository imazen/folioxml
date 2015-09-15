package folioxml.slx;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

public class SlxContextStack {

    private Stack<SlxToken> stack = new Stack<SlxToken>();

    /**
     * @param xmlMode       in XML mode, ghost tags are prohibited and will throw an InvalidMarkupException.
     * @param tagGhostPairs If true, ghost tags are prepped for conversion to XML (pairs are tagged with a UUID)
     */
    public SlxContextStack(boolean xmlMode, boolean tagGhostPairs) {
        this.inXmlMode = xmlMode;
        this.tagGhostPairs = tagGhostPairs;
    }

    protected boolean inXmlMode = false;

    /**
     * If true, XML mode is enforced (no ghost tags allowed)
     *
     * @return
     */
    public boolean inXmlMode() {
        return inXmlMode;
    }

    protected boolean tagGhostPairs = false;

    /**
     * If true, then matching pairs of ghost tags are being branded with a UUID so that the token stream is prepared for
     * the conversion to XML.
     *
     * @return
     */
    public boolean taggingGhostPairs() {
        return tagGhostPairs;
    }

    /**
     * Adds the token to the top of the stack. Can be any type: ghost, context, normal
     *
     * @param t
     * @throws InvalidMarkupException
     */
    public void add(SlxToken t) throws InvalidMarkupException {
        if (t.isGhost && inXmlMode)
            throw new InvalidMarkupException("SlxContextStack prohibits ghost tags when in XML mode", t);
        stack.push(t);
    }

    /**
     * TODO: this method is not needed since doing anything should consider removing it
     */
    public int size() {
        return stack.size();
    }

    /**
     * returns the innermost parent with startsNewContext == true
     *
     * @return
     */
    public SlxToken getTopContext() {
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (stack.get(i).startsNewContext) return stack.get(i);
        }
        return null;
    }

    /**
     * The top item, even if it is a ghost. Returns null if stack is empty.
     *
     * @return
     */
    public SlxToken topItem() {
        if (stack.size() == 0) return null;
        return (stack.peek());
    }

    /**
     * The top (non-ghost) item. Use topItem() to get the actual top item, including ghosts.
     * Same as topItem() in xmlMode;
     *
     * @return
     */
    public SlxToken top() {
        if (inXmlMode) return topItem();

        for (int i = stack.size() - 1; i >= 0; i--) {
            if (!stack.get(i).isGhost) return stack.get(i);
        }
        return null;
    }

    /**
     * How many non-ghost items are currently on the stack.
     * Returns stack.size() in xmlMode
     *
     * @return
     */
    public int nonGhostCount() {
        if (inXmlMode) return size();

        int count = 0;
        for (int i = 0; i < stack.size(); i++) {
            if (!stack.get(i).isGhost) count++;
        }
        return count;
    }

    /**
     * Pops the top (non-ghost) item off the stack. Returns null if the stack is empty of non-ghosts.
     * Calls popItem() in xmlMode
     *
     * @return
     */
    public SlxToken pop() {
        if (inXmlMode) return popItem();
        int index = -1;
        SlxToken s = null;
        for (int i = stack.size() - 1; i >= 0; i--) {
            s = stack.get(i);
            if (!s.isGhost) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            stack.remove(index);
        }
        return s;
    }

    /**
     * Returns the topmost item off the stack, but only if it is a ghost. Returns null if the top item isn't a ghost, or the stack is empty.
     *
     * @return
     */
    public SlxToken topGhost() {
        if (stack.size() > 0 && stack.peek().isGhost) return stack.peek();
        return null;
    }

    /**
     * Pops the topmost item off the stack, even if it is a ghost.  Returns null if the stack is empty.
     *
     * @return
     */
    public SlxToken popItem() {
        if (stack.size() == 0) return null;
        return stack.pop();
    }

    /**
     * Pops the topmost item off the stack, but only if it is a ghost.  Returns null if the top item isn't a ghost, or the stack is empty.
     *
     * @return
     */
    public SlxToken popGhost() {
        if (stack.size() > 0 && stack.peek().isGhost) return stack.pop();
        return null;
    }

    /**
     * Removes the specified ghost item from the stack - can be at any level *within* the current context. Throws an exception if the item is not a ghost.
     * Throws exception in xmlMode
     *
     * @param t
     * @return
     * @throws InvalidMarkupException
     */
    public boolean pullGhost(SlxToken t) throws InvalidMarkupException {
        if (t.isGhost && inXmlMode)
            throw new InvalidMarkupException("SlxContextStack prohibits ghost tags when in XML mode", t);

        if (!t.isGhost) throw new InvalidMarkupException("pullGhost only accepts ghost tags as arguments", t);

        SlxToken s;
        int index = -1;
        for (int i = stack.size() - 1; i >= 0; i--) {
            s = stack.get(i);
            if (t.equals(s)) {
                index = i;
                break;
            } //Record the index if we find the match
            if (s.startsNewContext)
                break; //don't cross context bounds, but allow the innermost context to be removed by reference
        }
        if (index > -1) {
            stack.remove(index);
            return true;
        }
        return false;
    }

    /**
     * Returns true if there is a tag that matches the specified tag name (or tag name regex) within the innermost context. use has(string,true) to bypass the context boundaries.
     *
     * @param string
     * @return
     */
    public boolean has(String string) throws InvalidMarkupException {
        return has(string, false);
    }

    /**
     * Returns true if there is a tag that matches the specified tag name (or tag name regex) within the innermost context. use has(string,true) to bypass the context boundaries.
     *
     * @param string
     * @return
     */
    public boolean has(String string, boolean bypassContext) throws InvalidMarkupException {
        SlxToken s;
        for (int i = stack.size() - 1; i >= 0; i--) {
            s = stack.get(i);
            if (s.matches(string)) return true;
            if (!bypassContext && s.startsNewContext) return false; //don't cross context bounds
        }
        return false;
    }

    /**
     * Returns a collection of the ghost tags currently open.
     *
     * @param name
     * @param bypassContext
     * @return
     * @throws InvalidMarkupException
     */
    public List<SlxToken> getGhostTags(String name, boolean bypassContext) throws InvalidMarkupException {
        return getOpenTags(name, true, bypassContext);
    }

    /**
     * Returns a collection of the ghost tags currently open, topmost first.
     *
     * @param name
     * @param bypassContext
     * @return
     * @throws InvalidMarkupException
     */
    public List<SlxToken> getOpenTags(String name, boolean ghostsOnly, boolean bypassContext) throws InvalidMarkupException {
        List<SlxToken> ghosts = new ArrayList<SlxToken>();
        SlxToken s;
        for (int i = stack.size() - 1; i >= 0; i--) {
            s = stack.get(i);
            if ((!ghostsOnly || s.isGhost) && (name == null || s.matches(name))) ghosts.add(s);
            if (!bypassContext && s.startsNewContext) return ghosts; //don't cross context bounds
        }
        return ghosts;
    }


    /**
     * Returns the innermost  tag that matches the specified tag name (or tag name regex) *within the innermost context!*
     *
     * @param string
     * @return
     */
    public SlxToken get(String string) throws InvalidMarkupException {
        return get(string, false);
    }


    /**
     * Returns the innermost  tag that matches the specified tag name (or tag name regex) *within the innermost context!*
     *
     * @param string
     * @return
     */
    public SlxToken get(String string, boolean bypassContext) throws InvalidMarkupException {
        SlxToken s;
        for (int i = stack.size() - 1; i >= 0; i--) {
            s = stack.get(i);
            if (s.matches(string)) return s;
            if (!bypassContext && s.startsNewContext) return null; //don't cross context bounds
        }
        return null;
    }

    /**
     * Returns the innermost tag that matches the specified tag name and type value. Searches ghosts also. Tag name and value can be a regex. if typeValue == null, find() will return null
     * if typeValue is null, then types will not be filtered.
     *
     * @param name
     * @param typeValue
     * @param bypassContext
     * @return
     */
    public SlxToken find(String name, String typeValue, boolean bypassContext) throws InvalidMarkupException {

        SlxToken s;
        for (int i = stack.size() - 1; i >= 0; i--) {
            s = stack.get(i);
            if (s.matches(name) && (typeValue == null || TokenUtils.fastMatches(typeValue, s.get("type")))) return s;
            if (!bypassContext && s.startsNewContext) return null; //don't cross context bounds
        }
        return null;
    }

    /**
     * Performs the appropriate .add() , .pop(), or .pullGhost() needed for the specified tag.
     * Compares tag name and the 'type' attribute to determine equivalence.
     *
     * @param t
     * @return
     * @throws InvalidMarkupException
     */
    public void process(SlxToken t) throws InvalidMarkupException {
        boolean strict = true;
        if (!t.isTag()) return; //Only tags are proccessed

        if (t.isGhost && inXmlMode)
            throw new InvalidMarkupException("SlxContextStack prohibits ghost tags when in XML mode", t);

        //If it's an opening tag, add it to the stack. Ghosts
        if (t.isOpening()) this.add(t);

        //(Only for non-ghosts): Make sure closing tags match with what's on the top of the stack. Ghost elements span & link aren't counted.
        if (t.isClosing() && !t.isGhost) {
            //See if there are open ghost tags

            SlxToken topGhost = this.topGhost();

            SlxToken opener = this.pop();
            //Compare tag names. If the closing tag has a type attribute, compare that as well.
            boolean isMatch = t.matches(opener.getTagName()) && (t.get("type") == null || t.get("type").equalsIgnoreCase(opener.get("type")));
            //Verify that this closing tag matches the topmost open tag (that's not a ghost)
            if (!isMatch) {
                boolean useContext = !t.startsNewContext;
                boolean matchExistsInContext = (this.find(t.getTagName(), t.get("type"), useContext) != null);


                if (matchExistsInContext)
                    throw new InvalidMarkupException("Closing tag for " + opener.markup + " expected.", t);
                else throw new InvalidMarkupException("Unexpected closing tag found.", t);
            } else if (strict && t.startsNewContext) {
                if (topGhost != null)
                    throw new InvalidMarkupException("Expected closing ghost tag before context ended.", topGhost);
            }
        }

        //For ghosts: make sure there is an opening tag in the stack (current context) that matches.
        if (t.isClosing() && t.isGhost) {
            SlxToken opener = this.find(t.getTagName(), t.get("type"), false);
            if (opener == null) {
                if (strict) throw new InvalidMarkupException("Unexpected closing ghost tag encountered.", t);
            } else {
                if (tagGhostPairs) {
                    opener.ghostPair = t.ghostPair = UUID.randomUUID(); //For later use
                }
                if (!this.pullGhost(opener)) throw new InvalidMarkupException("Failed to remove ghost item from stack");
            }
        }
    }

    /**
     * Returns the opening tag for t. The opening tag must be at the top of the stack (or in the context, in the case of ghost tags)
     *
     * @param t
     * @return
     * @throws InvalidMarkupException
     */
    public SlxToken getOpeningTag(SlxToken t) throws InvalidMarkupException {
        if (t.isGhost && inXmlMode)
            throw new InvalidMarkupException("SlxContextStack prohibits ghost tags when in XML mode", t);

        if (!t.isClosing()) throw new InvalidMarkupException("Only closing tags can be arguments", t);

        //(Only for non-ghosts): Make sure closing tags match with what's on the top of the stack. Ghost elements span & link aren't counted.

        //For ghosts: make sure there is an opening tag in the stack (current context) that matches.
        if (t.isGhost) {
            return this.find(t.getTagName(), t.get("type"), false);

        } else {//See if there are open ghost tags

            //Dont'Remove tag from stack
            SlxToken opener = this.top();
            //Compare tag names. If the closing tag has a type attribute, compare that as well.
            boolean isMatch = t.matches(opener.getTagName()) && (t.get("type") == null || t.get("type").equalsIgnoreCase(opener.get("type")));
            //Verify that this closing tag matches the topmost open tag (that's not a ghost)
            if (!isMatch) {
                return null;

            }
            return opener;
        }


    }

    /**
     * Returns true if there is a matching opening tag within the current context.
     *
     * @param t
     * @return
     * @throws InvalidMarkupException
     */
    public boolean matchingOpeningTagExists(SlxToken t) throws InvalidMarkupException {

        return (find(t.getTagName(), t.get("type"), false) != null);
    }

    /**
     * Returns the topmost item that doesn't match the specified regex. Doesn't cross context bounds
     * @param exclude
     * @return
     *
    public SlxToken top(String exclude){
    SlxToken s;
    for (int i = stack.size() - 1; i >= 0; i--){
    s = stack.get(i);
    if (!s.matches(exclude)) return s;
    if (s.startsNewContext) return null; //don't cross context bounds
    }
    return null;
    }
    /**
     * Returns the innermost match for 'string' that occurs before a match for 'boundingElement'. If 'string' matches 'boundingElement' it will be returned.
     * *within the innermost context*
     * @param string
     * @param boundingElement
     * @return
     *
    public SlxToken getInside(String string, String boundingElement) {
    SlxToken s;
    for (int i = stack.size() - 1; i >= 0; i--){
    s = stack.get(i);
    if (s.matches(string)) return s;
    if (s.matches(boundingElement)) return null; //don't cross bounding bounds
    if (s.startsNewContext) return null; //don't cross context bounds
    }
    return null;
    }

    /**
     * Removes the specified element from the array, but only if it is in the innermost conext (or *is* the innermost context)
     * @param t
     * @return
     *
    public SlxContextStack remove(String string){
    //Get index
    SlxToken s;
    int index = -1;
    for (int i = stack.size() - 1; i >= 0; i--){
    s = stack.get(i);
    if (s.matches(string)) {index = i; break;} //Record the index if we find the match
    if (s.startsNewContext) break; //don't cross context bounds, but allow the innermost context to be removed by reference
    }
    if (index > -1) stack.remove(index);
    return this;
    }

    /**
     * Removes the specified element from the array, but only if it is in the innermost conext (or *is* the innermost context)
     * @param t
     * @return
     *
    public SlxContextStack remove(SlxToken t){
    //Get index
    SlxToken s;
    int index = -1;
    for (int i = stack.size() - 1; i >= 0; i--){
    s = stack.get(i);
    if (t.equals(s)) {index = i; break;} //Record the index if we find the match
    if (s.startsNewContext) break; //don't cross context bounds, but allow the innermost context to be removed by reference
    }
    if (index > -1) stack.remove(index);
    return this;
    }
     * */

}