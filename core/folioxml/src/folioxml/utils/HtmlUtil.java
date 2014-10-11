package folioxml.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class HtmlUtil {

	/*
	 * 
	 * https://developer.mozilla.org/en-US/docs/HTML/Block-level_elements
	 * <address> Contact information. <article> HTML5 Article content. <aside>
	 * HTML5 Aside content. <audio> HTML5 Audio player. <blockquote> Long
	 * ("block") quotation. <canvas> HTML5 Drawing canvas. <dd> Definition
	 * description. <div> Document division. <dl> Definition list.
	 * 
	 * 
	 * <fieldset> Field set label. <figcaption> HTML5 Figure caption. <figure>
	 * HTML5 Groups media content with a caption (see <figcaption>). <footer>
	 * HTML5 Section or page footer. <form> Input form. <h1>, <h2>, <h3>, <h4>,
	 * <h5>, <h6> Heading levels 1-6. <header> HTML5 Section or page header.
	 * <hgroup> HTML5 Groups header information. <hr> Horizontal rule (dividing
	 * line).
	 * 
	 * <noscript> Content to use if scripting is not supported or turned off.
	 * <ol> Ordered list. <output> HTML5 Form output. <p> Paragraph. <pre>
	 * Preformatted text. <section> HTML5 Section of a web page. <table> Table.
	 * <tfoot> Table footer. <ul> Unordered list. <video> HTML5 Vide
	 */

	private static String[] Html5BlockElementsTags = { "article", "aside",
			"audio", "canvas", "figcaption", "figure", "footer", "header",
			"hgroup", "output", "section", "video" };


    //// added '<br />' 
	private static String[] blockElementsTags = { "address", "blockquote",
			"dd", "div", "dl", "fieldset", "div", "form", "h1", "h2", "h3",
			"h4", "h5", "h6", "noscript", "hr", "ol", "p", "pre", "table",
			"tfoot", "ul" , "br"};

	private static HashSet<String> blockElementsTagsSet = null;
	
	private static boolean includeHtml5Tags = false;
	private static boolean shouldNormalizeTagName = false;

	public static void includeHtml5Tags(boolean mIncludeHtml5Tags) {
		includeHtml5Tags = mIncludeHtml5Tags;
	}

	public static void normalizeTag(boolean mShouldNormalizeTagName) {
		shouldNormalizeTagName = mShouldNormalizeTagName;
	}

	public static boolean isTagNameBlockLevelElement(String tagName) {

		if (shouldNormalizeTagName) {
			tagName = tagName.toLowerCase(Locale.ENGLISH).trim();
		}

		List<String> list = Arrays.asList(blockElementsTags);

		if (includeHtml5Tags) {
			list.addAll(Arrays.asList(Html5BlockElementsTags));
		}
		blockElementsTagsSet = new HashSet<String>(list);

		return blockElementsTagsSet.contains(tagName);
	}

	public static boolean isTagNameBlockLevelElement(String tagName,
			boolean mIncludeHtml5Tags) {

		includeHtml5Tags = mIncludeHtml5Tags;

		return isTagNameBlockLevelElement(tagName);

	}

	public static boolean isTagNameBlockLevelElement(String tagName,
			boolean mIncludeHtml5Tags, boolean mShouldNormalizeTagName) {
		
		
		shouldNormalizeTagName = mShouldNormalizeTagName;

		return isTagNameBlockLevelElement(tagName, mIncludeHtml5Tags);

	}
}
