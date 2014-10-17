package folioxml.utils;

public class Dirs {
	private String version = "?";
	private String path;
	private String index;
	private String export;

	public Dirs() {
		super();
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getExport() {
		return export;
	}

	public void setExport(String export) {
		this.export = export;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}