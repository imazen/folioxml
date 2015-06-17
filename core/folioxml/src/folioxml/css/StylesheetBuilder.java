package folioxml.css;

import folioxml.core.InvalidMarkupException;
import folioxml.slx.SlxRecord;
import folioxml.slx.SlxToken;

/**
 * Operates on SLX valid only (that has been CssClassCleaner proccessed inside SlxTransformer)
 * @author nathanael
 *
 */
public class StylesheetBuilder {

	private SlxRecord root = null;
	public StylesheetBuilder(SlxRecord root){
		this.root = root;
	}
	

	
	private void getDefaultCss(String applySelector, StringBuilder sb){
		sb.append(applySelector + "td p:first {margin:0 0 0 0;}\n"); //The first P tag shouldn't have margins...
		sb.append(applySelector + "th p:first {margin:0 0 0 0;}\n"); //The first P tag shouldn't have margins...
		
		sb.append(applySelector + "p {margin:0; margin-top:0.2em;} p._empty{ padding-top:1em; }\n"); //Empty paragraphs get padding to mimic folio model.
		

		//Set the default font size and face. Folio uses TimesNewRoman 12
		//white-space-collapse: preserve; - Tries to maintain compatibility with Folio's treatment of whitespace.
		//font-weight: bolder; text-align: center 
		// margin:30px;
		sb.append(applySelector.length() == 0 ? "body" :  applySelector + " {font-family: \"Times New Roman\"; font-size:12pt; line-height:1.0em; white-space-collapse: preserve; white-space:pre-wrap;}\n");


		sb.append(applySelector + "th {font-weight:auto;text-align:auto;}\n"); //Reset to act like td - what is folio behavior?
	}
	
	public String getCss(String applySelector) throws InvalidMarkupException{
		StringBuilder sb = new StringBuilder();

        if (applySelector == null) applySelector = "";
        if (applySelector.length() > 0 && !applySelector.endsWith(" ")) applySelector += " ";

		getDefaultCss(applySelector,sb);
		
		for(SlxToken t:root.getTokens()){
			if (t.matches("style-def")){
				String cls = t.get("class");
				String style = t.get("style");
				String type = t.get("type");
				if (cls != null && style != null){
					String selector = "";
					/* style-def types: level, highlighter, character-style, paragraph, link, field */
					if (type.equalsIgnoreCase("level")) selector = "div";
					if (type.equalsIgnoreCase("paragraph")) selector = "p";
					if (type.equalsIgnoreCase("link")) selector = "a";
					if (selector.length() == 0) selector = "span";
					selector += "." + cls;
					
					sb.append(applySelector + selector + " {\n  ");
					sb.append(style.replace(";", ";\n  "));
					sb.append("}\n");
					
				}
			}
		}
		return sb.toString();
	}
	
	public String getCssAndStyleTags() throws InvalidMarkupException{
		return "<style type=\"text/css\">\n" + getCss(null) + "</style>";
	}
}

/* Default stylesheet for HTML 4 - we have to modify things so they mimic folio behavior...
 * 
 * html, address,
blockquote,
body, dd, div,
dl, dt, fieldset, form,
frame, frameset,
h1, h2, h3, h4,
h5, h6, noframes,
ol, p, ul, center,
dir, hr, menu, pre   { display: block }
li              { display: list-item }
head            { display: none }
table           { display: table }
tr              { display: table-row }
thead           { display: table-header-group }
tbody           { display: table-row-group }
tfoot           { display: table-footer-group }
col             { display: table-column }
colgroup        { display: table-column-group }
td, th          { display: table-cell }
caption         { display: table-caption }
th              { font-weight: bolder; text-align: center }
caption         { text-align: center }
body            { margin: 8px }
h1              { font-size: 2em; margin: .67em 0 }
h2              { font-size: 1.5em; margin: .75em 0 }
h3              { font-size: 1.17em; margin: .83em 0 }
h4, p,
blockquote, ul,
fieldset, form,
ol, dl, dir,
menu            { margin: 1.12em 0 }
h5              { font-size: .83em; margin: 1.5em 0 }
h6              { font-size: .75em; margin: 1.67em 0 }
h1, h2, h3, h4,
h5, h6, b,
strong          { font-weight: bolder }
blockquote      { margin-left: 40px; margin-right: 40px }
i, cite, em,
var, address    { font-style: italic }
pre, tt, code,
kbd, samp       { font-family: monospace }
pre             { white-space: pre }
button, textarea,
input, select   { display: inline-block }
big             { font-size: 1.17em }
small, sub, sup { font-size: .83em }
sub             { vertical-align: sub }
sup             { vertical-align: super }
table           { border-spacing: 2px; }
thead, tbody,
tfoot           { vertical-align: middle }
td, th, tr      { vertical-align: inherit }
s, strike, del  { text-decoration: line-through }
hr              { border: 1px inset }
ol, ul, dir,
menu, dd        { margin-left: 40px }
ol              { list-style-type: decimal }
ol ul, ul ol,
ul ul, ol ol    { margin-top: 0; margin-bottom: 0 }
u, ins          { text-decoration: underline }
br:before       { content: "\A"; white-space: pre-line }
center          { text-align: center }
:link, :visited { text-decoration: underline }
:focus          { outline: thin dotted invert }

Begin bidirectionality settings (do not change) 

BDO[DIR="ltr"]  { direction: ltr; unicode-bidi: bidi-override }
BDO[DIR="rtl"]  { direction: rtl; unicode-bidi: bidi-override }

*[DIR="ltr"]    { direction: ltr; unicode-bidi: embed }
*[DIR="rtl"]    { direction: rtl; unicode-bidi: embed }

@media print {
h1            { page-break-before: always }
h1, h2, h3,
h4, h5, h6    { page-break-after: avoid }
ul, ol, dl    { page-break-before: avoid }
}

 */