package folioxml.slx;

public interface ISlxTokenWriter {
    public void setUnderlyingWriter(ISlxTokenWriter base);

    public void write(SlxToken t) throws folioxml.core.InvalidMarkupException;
}