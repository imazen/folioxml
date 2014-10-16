package folioxml.utils;

public class Configuration {
	private Dirs folioHelp;
	private Dirs folioXml;

	public Configuration() {
		super();
	}

	public Dirs getFolioHelp() {
		return folioHelp;
	}

	public void setFolioHelp(Dirs folioHelp) {
		this.folioHelp = folioHelp;
	}

	public Dirs getFolioXml() {
		return folioXml;
	}

	public void setFolioXml(Dirs folioXml) {
		this.folioXml = folioXml;
	}
}