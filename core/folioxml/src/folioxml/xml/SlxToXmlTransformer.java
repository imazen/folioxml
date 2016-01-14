package folioxml.xml;

import folioxml.core.InvalidMarkupException;
import folioxml.core.TokenUtils;
import folioxml.slx.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Never proccess the same data twice with the same instance, or you will encounter errors. (There is an algorithm checker)
 *
 * @author nathanael
 */
public class SlxToXmlTransformer {

    public SlxToXmlTransformer() {

    }

    public SlxToXmlTransformer(SlxRecordReader srr) {
        this.srr = srr;
    }

    public SlxToXmlTransformer(File f) throws UnsupportedEncodingException, FileNotFoundException, InvalidMarkupException, IOException {
        this.srr = new SlxRecordReader(f);
    }

    public SlxToXmlTransformer(boolean keepEmptyGhostTags, boolean createEmptyGhostsAtStartAndEnd, boolean reuseAttrMaps) {
        this.createEmptyGhostsAtStartAndEnd = createEmptyGhostsAtStartAndEnd;
        this.keepEmptyGhostTags = keepEmptyGhostTags;
        this.reuseAttrMaps = reuseAttrMaps;
    }

    /**
     * If true, ghost tags with no content will be maintained. If false, they will be deleted.
     */
    public boolean keepEmptyGhostTags = false; //If false, ghost tags with no content will not be re-created.

    /**
     * True to place self-closing ghost tags at the beginning and end of a ghost tag's area, even if they contain no content.
     * This can be useful for reversing the splitting process, but makes the markup messier.
     */
    public boolean createEmptyGhostsAtStartAndEnd = false; //If true, empty ghost pairs will be placed at the beginning and end of a ghost tag's area, even if they contain no content.


    /**
     * True to recycle the attribute maps from the SlxTokens. If you set this to true, you cannot use the SlxTokens or the SlxRecord again, they will be corrupted.
     */
    public boolean reuseAttrMaps = false;

    public SlxRecordReader srr = null;


    public XmlRecord read() throws InvalidMarkupException, IOException {
        SlxRecord r = srr.read();
        if (r == null) return null;
        return convert(r);
    }

    public void close() throws IOException {
        srr.close();
        srr = null;
    }

    /**
     * Converts the SlxRecord.
     * If reuseAttrMaps, It will recycle data structures from the tokens where possible. Don't attempt to use the SlxRecord afterwards.
     * Regardless of the setting SlxTokens will have a pairID set on them.
     *
     * @param r
     * @return
     * @throws InvalidMarkupException
     * @throws IOException
     */
    public XmlRecord convert(SlxRecord r) throws InvalidMarkupException, IOException {
        //1) First, tag ghost pairs with a UUID if they aren't already.
        if (!r.ghostPairsGenerated) tagGhostPairs(r.getTokenReaderForRecord());

        //2) Then, build the tree structure and mark it up for splitting
        TNode tree = buildTree(r.getTokenReaderForRecord());

        //3) Follow the splitting instructions
        splitGhosts(tree);

        //4) The tree should have one root node, <record>. Find this, and turn it into an XmlRecord instance.
        if (tree.childrenCount() > 1) throw new InvalidMarkupException("No tags can exist outside the record tag");
        else if (tree.childrenCount() == 0)
            throw new InvalidMarkupException("Tree cannot be empty - record tag should exist");
        ///tree.children //assert length is one (the SlxRecord).

        //Write out the new, XML compliant token stream
        XmlRecord newR = new XmlRecord(r, false);

        tree.toXmlNode().children.filterByTagName("record", false).first(true).moveChildrenTo(newR);

        r.slxXmlRecordTag = newR; //Ok, now future XmlRecords will be able to translate the .parent chain by lookup
        return newR;
    }

    /*
     * Tags the ghost pairs using SlxContextStack
     */
    public void tagGhostPairs(ISlxTokenReader r) throws IOException, InvalidMarkupException {
        SlxContextStack s = new SlxContextStack(false, true);
        while (r.canRead()) {
            SlxToken t = r.read();
            s.process(t);
        }
        if (s.size() > 0) throw new InvalidMarkupException("Token stream is not complete - there are orphaned tags");
    }


    public TNode buildTree(ISlxTokenReader r) throws InvalidMarkupException, IOException {
        //The root of the tree. No token is attached to this node.
        TNode root = new TNode();
        root.isRoot = true;

        //Use to build a TNode tree...
        TNodeStack s = new TNodeStack();


        while (r.canRead()) {
            SlxToken token = r.read();


            TNode current = new TNode();
            current.t = token;
            //This is how we know how to build the tree. TNodeStack tracks it for us.
            current.parent = s.top() == null ? root : s.top();

            //Process the TNode. At this point, both opening and closing TNodes are proccessed. Should throw an error if there is anything
            //wrong with the non-ghost tags structure.
            s.process(current);

            //But never allow closing nodes to be added into the hierarchy unless they are ghost tags.
            if (!token.isTag() || token.isGhost || !token.isClosing()) {
                current.parent.addChild(current);
            }

            //Verify ghost tags have been tagged
            if (token.isGhost && token.ghostPair == null)
                throw new InvalidMarkupException("buildTree cannot be called until ghost pairs are all tagged. Use tagGhostPairs if the tokens aren't coming from SlxRecordReader", token);


            //Good time to find the connecting paths between the pairs
            //Populate the ghostChildren collections.
            //Find duplicates in ghostChildren and put them in ghostPairs, then remove those entries in parent recursively.

            //Switch to using a linkedhashmap
            if (token.isGhost && token.isOpening()) {
                //Go all the way up the ancestry, adding the UUID and current TNode to the .ghostChildren collection of each ancestor
                TNode temp = current.parent;
                while (temp != null) {
                    //Prepare if null
                    if (temp.ghostChildren == null) temp.ghostChildren = new LinkedHashMap<UUID, TNode>();
                    temp.ghostChildren.put(token.ghostPair, current); //Mark every parent up the chain.
                    temp = temp.parent;
                }
            } else if (token.isGhost && token.isClosing()) {
                //Cycle up the chain looking for the intersecting parent
                //Remove from ancestors above the intersection point.
                //Move to ghostPairs for current node
                TNode temp = current.parent;

                while (temp != null) {
                    //prepare if null
                    if (temp.ghostChildren == null) temp.ghostChildren = new LinkedHashMap<UUID, TNode>();

                    if (temp.ghostChildren.containsKey(token.ghostPair)) {
                        //Match found - intersect point a/b\c
                        TNode opener = temp.ghostChildren.get(token.ghostPair);

                        //Remove children entries here and above
                        TNode temp2 = temp;
                        while (temp2 != null) {
                            if (temp2.ghostChildren != null) temp2.ghostChildren.remove(token.ghostPair);
                            temp2 = temp2.parent;
                        }


                        //Add to ghostPairs
                        if (temp.ghostPairs == null) temp.ghostPairs = new LinkedHashMap<UUID, Pair<TNode, TNode>>();
                        temp.ghostPairs.put(token.ghostPair, new Pair<TNode, TNode>(opener, current));
                        current.opener = opener;
                        break;

                    } else {
                        temp.ghostChildren.put(token.ghostPair, current); //Mark every parent up the chain.
                    }
                    temp = temp.parent;
                }
            }

        }

        //Big huge warning here! We are relying on SlxTransformer to ensure that ghost tags exist in matched pairs.
        //If there is a orphan ghost tag of either opening or closing type, it will encompass the entire remainder of the record.

        //One way to check for problems is make sure that root.ghostChildren is empty - this means no orphans.

        if (root.ghostChildren != null && root.ghostChildren.size() > 0) {
            throw new InvalidMarkupException("There are orphaned ghost tags present in this record. They should have already been cleaned by now");
        }

        return root;

    }

    public HashSet<UUID> bannedPairs = new HashSet<UUID>();


    public void splitGhosts(TNode b) throws InvalidMarkupException {
        //Go through the tree recursively, ghostChildren and ghostPairs should be populated.

        //Remove all ghost tag references from the children collections. We don't need them anymore - we have ghostChildren and ghostPairs.
        //Ghost tags have no ghostChildren or ghostPairs to work with, and no children either.
        //Order is important - since we are processing top-down, removing leaf nodes after their intermediate parent replacements already exist should be harmless.
        //We can still reference these through ghostPairs and ghostChildren until we're done, so the b -> parent link should not be broken.

        //Skip ghost nodes. What if it is located in a place it can't stay?

        if (b.t == null && !b.isRoot)
            throw new InvalidMarkupException("Error converting SLX to XML: Token not attached to tree node.");


        if (b.t != null && b.t.isGhost) {
            return;
        }

        TNode countDepth = b;
        int depth = 0;
        while (countDepth != null) {
            countDepth = countDepth.parent;
            depth++;
        }
        //Depth assertion...Not needed
        //if (depth > 35)
        //	assert(depth < 40); //Or we have some kind of problem

        if (b.t != null && b.parent != null && b.parent.t != null) {
            if (b.t.ghostPair != null && b.t.ghostPair == b.parent.t.ghostPair)
                throw new InvalidMarkupException("We have nested ghost pairs - impossible?"); //we should never have two copies of a pair in the hierarchy

            //if (b.t.matches(b.parent.t.getTagName())){ // && (b.t.get("type") == null || b.t.get("type").equalsIgnoreCase(b.parent.t.get("type")))
            //We have matching tags nested.
            //assert(true);
            //}
        }


        if (b.delayedGhosts != null && b.children != null) {
            for (SlxToken insert : b.delayedGhosts) {
                if (b.t != null && !SlxValidator.isAllowedInside(insert, b.t)) {
                    for (TNode tnp : b.children) {
                        tnp.addDelayedGhost(insert);
                    }
                } else {
                    b.insertParent(b.children, insert.toNonGhostVersion(true), false, -1, this); //Handles adding ghostChildren, moving ghostPairs if needed
                }
            }
            b.delayedGhosts = null;
        }

        //Process pairs (a/b\c
        if (b.ghostPairs != null) {
            while (b.ghostPairs.size() > 0) {
                Object key = b.ghostPairs.keySet().toArray()[0];
                if (bannedPairs.contains(key)) {
                    throw new InvalidMarkupException("Encountered the same ghost pair twice!", b.t);
                }


                TNode a = b.ghostPairs.get(key).a;
                TNode c = b.ghostPairs.get(key).b;
                b.ghostPairs.remove(key); //Removed;
                bannedPairs.add((UUID) key);
                //Somehow, the ghost pair gets recreated once more causing duplicates...


                TNode aBranch = a.findChildUnder(b);
                TNode cBranch = c.findChildUnder(b);
                if (aBranch == cBranch)
                    throw new InvalidMarkupException("Bug in algorithm - aBranch and cBranch cannot be equal");

                //b.children should exist if b.ghostPairs does.

                if (b.children.indexOf(aBranch) < 0 || b.children.indexOf(cBranch) < 0)
                    throw new InvalidMarkupException("Failed to locate the branches A and C belong to. Ghost tag splitting failed on token", b.t);

                //Get the middle batch - between aBranch and cBranch - the simple part
                List<TNode> list = b.children.subList(b.children.indexOf(aBranch) + 1, b.children.indexOf(cBranch));

                //There is only one peak for each pair of ghost tags.
                //Thus, we can steal the attribute collection from the opening ghost tag this once.
                //The ghost tag is discarded during writing, so the duplicate reference shouldn't cause a problem.
                //Should save a lot on initialization costs. Simple ghost tags are the majority.

                //DISCOVERED OVERSIGHT
                //You can't insert a intermediate parent under a <table> or <tr> tag.
                //Gotta push those down to the subchildren.
                SlxToken insert = a.t.toNonGhostVersion(false); //WARNING - the attribute collection on the original is cloned.

                if (b.t != null && !SlxValidator.isAllowedInside(insert, b.t)) {
                    insert = a.t.toNonGhostVersion(true); //Gotta deep copy...
                    //System.out.println("Failed to place " + insert.toString() + " inside " + b.toString());
                    for (TNode tnp : list) {
                        tnp.addDelayedGhost(insert);
                    }
                } else {

                    b.insertParent(list, insert, aBranch == a && cBranch == c && keepEmptyGhostTags, b.children.indexOf(aBranch), this); //Handles adding ghostChildren, moving ghostPairs if needed (which affects this loop, but should be safe)
                }
            }
        }
        //Process a/b/c and a\b\c
        if (b.ghostChildren != null) {
            while (b.ghostChildren.size() > 0) {
                Object key = b.ghostChildren.keySet().toArray()[0];
                TNode lower = b.ghostChildren.get(key);
                b.ghostChildren.remove(key); //remove
                TNode branch = lower.findChildUnder(b);

                if (b.children.indexOf(branch) < 0)
                    throw new InvalidMarkupException("Failed to locate the branches A or C belong to. Ghost tag splitting failed on token", b.t);
                //b.children should exist if b.ghostChildren does.

                //Build
                SlxToken insert = null;
                List<TNode> list = null;
                //Find the opening ghost tag and make a non-ghost version.
                //Populate a list of afected children.
                if (lower.t.isOpening()) {
                    insert = lower.t.toNonGhostVersion(true);

                    list = b.children.subList(b.children.indexOf(branch) + 1, b.children.size());//Inserts a intermediate parent for children after 'branch'

                } else if (lower.t.isClosing()) {
                    TNode opening = lower.opener; //findOpeningTag() Won't work - the pair is already gone. Gotta keep a reference.
                    if (opening == null)
                        throw new InvalidMarkupException("Matching opening node wasn't found.", lower.t);
                    insert = opening.t.toNonGhostVersion(true);
                    list = b.children.subList(0, b.children.indexOf(branch));//Before branch - a closing tag
                }

                //If we can't add an intermediate here, delay it to the child.
                if (b.t != null && !SlxValidator.isAllowedInside(insert, b.t)) {
                    for (TNode tnp : list) {
                        tnp.addDelayedGhost(insert);
                    }
                } else {
                    //Insert
                    b.insertParent(list, insert, branch == lower && createEmptyGhostsAtStartAndEnd, b.children.indexOf(branch), this);
                }


            }
        }

        //Do all children recursively
        for (int i = 0; i < b.childrenCount(); i++) {

            splitGhosts(b.children.get(i));
        }

    }


    private class TNode {
        public boolean isRoot = false;
        /**
         * The token this object wraps
         */
        public SlxToken t = null; //What about the closing tag..? Drop?
        public TNode parent = null;
        public List<TNode> children = null;
        public UUID pairId = null;
        public TNode opener = null; //So closing ghost tags can keep a reference to their opeing ghost tag for cloning purposes.
        public LinkedHashMap<UUID, TNode> ghostChildren = null;
        public List<SlxToken> delayedGhosts = null; //These must be added around all node contents, since it was invalid XML for them to be added around this node itself.


        public LinkedHashMap<UUID, Pair<TNode, TNode>> ghostPairs = null;
        public boolean generated = false;


        public int childrenCount() {
            return children == null ? 0 : children.size();
        }

        /**
         * Converts the tree into an xml Node
         *
         * @return
         */
        public Node toXmlNode() {
            Node n = new Node(t, !reuseAttrMaps); //Original token gets modified!
            if (t != null) {
                if (t.isGhost) return null;
                assert (!(t.isTag() && t.isClosing())) : "Closing nodes should not exist here";
            }

            if (children != null) {
                if (t != null) assert (t.isTag() && t.isOpening()) : "Huh?";
                //Create the children collection
                if (children.size() > 0) n.children = new NodeList(children.size());
                for (TNode tn : children) {
                    Node c = tn.toXmlNode();
                    if (c != null) {
                        n.children.list().add(c);
                        c.parent = n;
                    }
                }
            }
            return n;
        }

        public void fixChildrenParentRefs() {
            for (int i = 0; i < childrenCount(); i++) {
                children.get(i).parent = this;
            }
        }
/*
        public void write(ISlxTokenWriter w) throws InvalidMarkupException{
			
			
			if (t == null){
				//The root node only writes the children.
				for (int i = 0; i < childrenCount(); i++){
					children.get(i).write(w);
				}
			}else{
				
				if (this.t.isGhost) return;
				//No closing tags should be present
				assert(!(this.t.isClosing() && this.t.isTag()));
				
				
				w.write(t);
				if (t.isTag() && t.isOpening()){
					for (int i = 0; i < childrenCount(); i++){
						children.get(i).write(w);
					}
					w.write(t.getClosingTag());
				}else{
					assert(childrenCount() == 0);
				}
			}
		}
		*/

        /**
         * Looks upwards through the hierarchy and returns the ancestor that is a immediate descendant of grandparent).
         * This may be itself.
         *
         * @param grandparent
         * @return
         */
        public TNode findChildUnder(TNode grandparent) {
            TNode temp = this;
            while (temp.parent != null) {
                if (temp.parent == grandparent) return temp;
                temp = temp.parent;
            }
            return null;
        }

        /**
         * Inserts a parent between this node and a set of children.
         *
         * @param affectedChildren
         * @param t
         * @throws InvalidMarkupException
         */
        public void insertParent(List<TNode> affectedChildren, SlxToken t, boolean keepIfEmpty, int insertEmptyTagAt, SlxToXmlTransformer optionalCaller) throws InvalidMarkupException {
            //You must pass in non-ghost children in for tags to be created.
            int nonGhosts = 0;
            for (TNode ch : affectedChildren) {
                if (!ch.t.isGhost) nonGhosts++; //Ghost tags cannot have children, so we don't have to be recursive.
            }

            if (nonGhosts == 0 && !keepIfEmpty) return;


            if (insertEmptyTagAt < 0 && keepIfEmpty)
                throw new InvalidMarkupException("Please specify a positive insertEmptyTagAt value if keepIfEmpty=true");


            //We can't do it directly, or we will get a coomidifcation error - we have to clone before assigning to n.children
            ArrayList<TNode> copy = new ArrayList<TNode>();
            copy.addAll(affectedChildren);


            //Here's what's happening.
            //insertParent is getting called in a peculiar order. Need to try to fix this anyhow... but I think the results should be the same regardless of order

            //insertParent is being called on B first somehow. (duh!, because b completes before a in ghostPairs....)  (also, ghostPairs get processed before ghostChildren, that will always be a problem_
            // <a><b></b></a>
            //The problem is that ghost tags pair peaks are occuring twice.


            //To avoid this, we need to prevent addGhostChildrenSmart from adding anything that doesn't already exist in the corresponding parent ggostPairs.
            //If the parent doesn't have a ghost pair, we shouldn't have it, because the parent has already proccessed it.
            //If we have it, and they do also, remove it from the parent.

            //ghostChildren aren't affected, since those are desired at every level of the tree.


            //The new intermediate parent
            TNode n = new TNode();
            n.parent = this;
            n.children = copy;
            n.t = t; //The opening ghost tag
            n.generated = true;
            //Generated tokens are getting processed twice.. :(


            //Swap the parent references, insert 'n', and remove children
            n.fixChildrenParentRefs();

            //Get index of first
            if (copy.size() > 0) {
                int insertAt = this.children.indexOf(copy.get(0));
                this.children.removeAll(copy);
                this.children.add(insertAt, n); //Add 'n to replace the children.
            } else {
                //The empty check is performed at the top of the function. keepIfEmpty=true here.
                this.children.add(insertEmptyTagAt, n);
            }

            //Adding *all* ghost children from descendants of 'n' to 'n'
            for (TNode c : copy) {
                //Add direct ghost children as well.
                if (c.t.isGhost) n.addGhostChildSmart(c);
                //Add others
                if (c.ghostChildren != null) {
                    Collection<TNode> vals = c.ghostChildren.values();
                    for (TNode v : vals) {
                        n.addGhostChildSmart(v); //ghostPairs is updated on the fly.
                    }
                }
            }


            if (n.ghostPairs != null && ghostPairs != null) {

                //Find the intersection of 'n.ghostPairs' and 'this.ghostPairs'.
                //Use this intersection as the new collection for n.ghostPairs.
                //Remove this intersection from 'this.ghostPiars'.

                Object[] nKeys = n.ghostPairs.keySet().toArray(new UUID[0]); //Can't iterate through a collection we are modifying. Have to use a fixed array of keys

                //Go through each key in 'n'. If present in 'this.ghostPairs', remove from this.
                //If not present, remove from 'n'.

                for (Object o : nKeys) {
                    if (this.ghostPairs.remove(o) == null) {
                        //Null means that 'this' didn't have it.
                        //Soo. remove from n
                        n.ghostPairs.remove(o);
                    } else {
                        //This means that 'this' had it, and removed it. only 'n' has it now.
                        //Check the banned collection
                        if (optionalCaller.bannedPairs.contains(o)) {
                            throw new InvalidMarkupException("Encountered the same ghost pair twice!");
                        }
                    }
                }


            } else {
                if (n.ghostPairs != null && ghostPairs == null)
                    n.ghostPairs = null; //If the parent has no ghost pairs, we shouldn't have any.
            }

        }

        protected void addGhostChildSmart(TNode c) throws InvalidMarkupException {
            UUID key = c.t.ghostPair;
            if (key == null) throw new InvalidMarkupException("Ghost tag is missing pair key UUID");
            if (ghostChildren == null) ghostChildren = new LinkedHashMap<UUID, TNode>();
            if (ghostChildren.containsKey(key)) {
                TNode o = ghostChildren.get(key);

                if (o == c) {
                    throw new InvalidMarkupException(); //Don't remember. But shouldn't happen.
                }


                //Add to ghostPairs
                if (ghostPairs == null) ghostPairs = new LinkedHashMap<UUID, Pair<TNode, TNode>>();

                ghostPairs.put(key, new Pair<TNode, TNode>(o, c));
                c.opener = o;

                //Remove from ghostChildren
                ghostChildren.remove(key);

            } else {
                ghostChildren.put(key, c);
            }
        }

        public void addChild(TNode c) {
            if (this.children == null) children = new ArrayList<TNode>();
            children.add(c);
        }

        public void addDelayedGhost(SlxToken t) {
            if (this.delayedGhosts == null) delayedGhosts = new ArrayList<SlxToken>();
            delayedGhosts.add(t);
        }

        public String toString() {
            return (t != null) ? t.toString() : "ROOT NODE";
        }

    }

    private class Pair<A, B> {

        public final A a;
        public final B b;

        public Pair(A first, B second) {
            this.a = first;
            this.b = second;
        }
    }

    /**
     * Based on the SlxContextStack, but with all the ghost logic removed, and designed for the TNode wrapper type instead.
     *
     * @author nathanael
     */
    private class TNodeStack extends Stack<TNode> {

        public TNode top() {
            if (this.size() > 0) return this.peek();
            else return null;
        }

        /**
         * Returns the innermost tag that matches the specified tag name and type value.  Tag name and value can be a regex. if typeValue == null, find() will return null
         * if typeValue is null, then types will not be filtered.
         *
         * @param name
         * @param typeValue
         * @param bypassContext
         * @return
         */
        public SlxToken find(String name, String typeValue, boolean bypassContext) throws InvalidMarkupException {

            SlxToken s;
            for (int i = this.size() - 1; i >= 0; i--) {
                s = this.get(i).t;
                if (s.matches(name) && (typeValue == null || TokenUtils.fastMatches(typeValue, s.get("type"))))
                    return s;
                if (!bypassContext && s.startsNewContext) return null; //don't cross context bounds
            }
            return null;
        }

        /**
         * Performs the appropriate .add() or .pop(),  needed for the specified tag.
         * Compares tag name and the 'type' attribute to determine equivalence.
         *
         * @param t
         * @return
         * @throws InvalidMarkupException
         */
        public void process(TNode tn) throws InvalidMarkupException {
            SlxToken t = tn.t;
            if (!t.isTag()) return; //Only tags are proccessed
            if (t.isGhost) return; //Ghost tags are considered self-closing.

            //If it's an opening tag, add it to the stack. Ghosts
            if (t.isOpening()) this.push(tn);

            //(Only for non-ghosts): Make sure closing tags match with what's on the top of the stack. Ghost elements span & link aren't counted.
            if (t.isClosing()) {
                if (this.size() < 1)
                    throw new InvalidMarkupException("Unexpected closing tag - no matching opening tags", t);

                //!!Remove tag from stack
                TNode opener = this.pop();
                //Compare tag names. If the closing tag has a type attribute, compare that as well.
                boolean isMatch = t.matches(opener.t.getTagName()) && (t.get("type") == null || t.get("type").equalsIgnoreCase(opener.t.get("type")));
                //Verify that this closing tag matches the topmost open tag (that's not a ghost)
                if (!isMatch) {
                    boolean useContext = !t.startsNewContext;
                    boolean matchExistsInContext = (this.find(t.getTagName(), t.get("type"), useContext) != null);

                    if (matchExistsInContext)
                        throw new InvalidMarkupException("Closing tag for " + opener.t.markup + " expected first.", t);
                    else throw new InvalidMarkupException("Unexpected closing tag found.", t);
                }
            }

        }
    }
}
