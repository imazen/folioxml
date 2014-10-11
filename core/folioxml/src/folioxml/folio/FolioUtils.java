package folioxml.folio;

import folioxml.core.InvalidMarkupException;

import java.io.IOException;
import java.io.StringReader;

public class FolioUtils {

	/**
	 * Extracts and combines all text tokens found in the specified section of Folio Flat File markup.
	 * Escape characters are not decoded - <<, <TB>, <CH:127> etc are removed, not decoded. 
	 * For basic use only.
	 * @param folioMarkup
	 * @return
	 * @throws IOException
	 * @throws InvalidMarkupException
	 * @deprecated
	 */
	public static String extractText(String folioMarkup) throws IOException, InvalidMarkupException{
		StringBuilder sb = new StringBuilder(folioMarkup.length());
		FolioTokenReader r = new FolioTokenReader(new StringReader(folioMarkup));
		while(true){
			FolioToken ft = r.read();
			if (ft == null) break;
			if (ft.type == FolioToken.TokenType.Text){
				sb.append(ft.text);
			}
		}
		return sb.toString();
	}
}
