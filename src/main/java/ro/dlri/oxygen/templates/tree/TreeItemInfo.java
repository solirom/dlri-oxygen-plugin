package ro.dlri.oxygen.templates.tree;

public class TreeItemInfo {
	public String senseTitle;
	public String currentNodeXpathPath;

	public TreeItemInfo(String title, String xpathString) {
		senseTitle = title;
		currentNodeXpathPath = xpathString;
	}

	public String toString() {
		return senseTitle;
	}
}
