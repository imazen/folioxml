package folioxml.export;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class StaticFileNode implements FileNode {

    public StaticFileNode(FileNode parent) {
        this.parent = parent;
    }

    FileNode parent;
    String relativePath;
    Map<String, String> attrs = new HashMap<String, String>();
    Map<String, Object> bag = new HashMap<String, Object>();

    @Override
    public FileNode getParent() {
        return parent;
    }

    public StaticFileNode getP() {
        return (StaticFileNode) parent;
    }


    @Override
    public Map<String, String> getAttributes() {
        return attrs;
    }

    @Override
    public Map<String, Object> getBag() {
        return bag;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }


    public int incrementCounter(String counterName, Integer startAt) {
        Integer current = getCounter(counterName, startAt) + 1;
        getBag().put(counterName, current);
        return current;
    }

    public int getCounter(String counterName, Integer defaultValue) {
        Integer current = (Integer) getBag().get(counterName);
        return (current == null) ? defaultValue : current;
    }

    public int setCounter(String counterName, Integer value) {
        getBag().put(counterName, value);
        return value;
    }


    public String getDelimitedHierarchyValues(String counterName, String delimiter) {
        return ((parent == null) ? "" : getP().getDelimitedHierarchyValues(counterName, delimiter) + delimiter) + Integer.toString(getCounter(counterName, null));
    }


    public Deque<StaticFileNode> getAncestors(boolean includeSelf) {
        Deque<StaticFileNode> parents = new ArrayDeque<StaticFileNode>();

        StaticFileNode current = this;
        if (includeSelf) parents.add(this);
        while (current.parent != null) {
            parents.addLast((StaticFileNode) current.parent);
            current = (StaticFileNode) current.parent;
        }
        return parents;
    }

}
